# calculate the tests that 
# 1. have at least one seed saved in at least two of the rounds, and
# 2. have different first-round coverage data in the rounds
import sys, json
from typing import List, Dict, Tuple, Set
from config import PROJECTS_MAPPING, ROUNDS
from pathlib import Path
from infra import VM, Campaign
from util import get_data_dir, get_trunc_module
from collections import defaultdict
coverageTrends: defaultdict[str, defaultdict[str, Dict[str, List[Tuple[int, int]]]]] = defaultdict(lambda: defaultdict(lambda: {}))

def main(rounds: List[str]):
    assert all(rnd in ROUNDS['confuzz'] + ROUNDS['align'] + ROUNDS['default'] for rnd in rounds), \
            "rounds must all starts from default"
    global coverageTrends
    projectTests:Dict[str, Set[str]] = {project: set() for project in PROJECTS_MAPPING}
    for rnd in rounds:
        for project, projId in PROJECTS_MAPPING.items(): # TODO: check test names are the same across different rounds
            for vm in VM.getVMs(get_data_dir(rnd, projId), get_trunc_module(project)):
                for test in vm.getTests():
                    projectTests[project].add(test)
                    campaign: Campaign = vm.getCampaignForTest(test)
                    coverageTrend = campaign.getCoverageTrend()
                    if coverageTrend is not None:
                        coverageTrends[rnd][project][test] = coverageTrend

    # Now check what's the tests that are actually wrong
    tables: Dict[str, defaultdict[str, Dict[str, Dict[str, int]]]] = {}
    tables["firstRoundCovDiffAtLeastTwo"] = defaultdict(lambda: {})
    for project, tests in projectTests.items():
        for test in tests:
            validRounds = {rnd: coverageTrends[rnd][project][test][0][1] 
                           for rnd in rounds if test in coverageTrends[rnd][project]}
            if len(validRounds) > 2 and len(set(validRounds.values())) > 1:
                tables["firstRoundCovDiffAtLeastTwo"][project][test] = validRounds
            elif len(validRounds) == 1:
                print(f"What happened? {project} {test} {next(iter(validRounds))}")
    
    resultDir:Path = Path('meta_coverage') / "_".join(rounds)
    resultDir.mkdir(parents=True, exist_ok=True)
    with open(resultDir / "META.csv", "w") as f:
        f.write(','.join([''] + list(PROJECTS_MAPPING.keys())) + '\n')
        for k,v in tables.items():
            f.write(','.join([k] + [str(len(v[project])) for project in PROJECTS_MAPPING]) + '\n')

    for k, v in tables.items():
        (resultDir / k).mkdir(exist_ok = True)
        for project in PROJECTS_MAPPING.keys():
            with open(resultDir / k / f"{project}", "w") as f:
                for test, coverage in v:
                    f.write(f"{test};{json.dumps(coverage)}\n")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python calculate_coverage_diff.py <rounds>")
        exit(1)
    rounds: List[str] = sorted(sys.argv[1].split(','))
    main(rounds)
