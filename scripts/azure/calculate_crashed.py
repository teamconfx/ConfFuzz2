# calculate info regarding how many tests crashed during the fuzzing
import sys
from typing import List, Dict, Set
from config import PROJECTS_MAPPING, ROUNDS
from pathlib import Path
from infra import VM, Campaign
from util import get_data_dir, get_trunc_module
from collections import defaultdict
tables: Dict[str, defaultdict[str, defaultdict[str, Set[str]]]] = {}

def main(rounds: List[str]):
    assert all(rnd in sum(ROUNDS.values(), []) for rnd in rounds), \
            f"{rounds} rounds must be all in confuzz"
    global tables
    tables["defaultFailures"] = defaultdict(lambda: defaultdict(set))
    tables["fuzzTimeFileLessThenHalf"] = defaultdict(lambda: defaultdict(set))
    tables["noFuzzTimeFile"] = defaultdict(lambda: defaultdict(set))
    tables["crashedVM"] = defaultdict(lambda: defaultdict(set))
    tests = {}
    for rnd in rounds:
        for project, projId in PROJECTS_MAPPING.items(): # TODO: check test names are the same across different rounds
            print(f"data_dir = {get_data_dir(rnd, projId)}")
            for vm in VM.getVMs(get_data_dir(rnd, projId), get_trunc_module(project)):
                tests = vm.getTests()
                for test in tests:
                    campaign: Campaign = vm.getCampaignForTest(test)
                    # now update the table
                    fuzzTime = campaign.getFuzzTime()
                    if not campaign.exists():
                        tables["crashedVM"][rnd][project].add(test)
                    elif fuzzTime is None:
                        fuzzTime = campaign.getFuzzTimeFromLog()
                        tables["noFuzzTimeFile"][rnd][project].add(f"{test} : {fuzzTime}")
                    elif fuzzTime < 29*60*1000:
                        tables["fuzzTimeFileLessThenHalf"][rnd][project].add(f"{test} : {fuzzTime} ms")
                    
                    corpusSize = campaign.getCorpusSize()
                    failureSize = campaign.getFailureSize()
                    if corpusSize is not None and failureSize is not None \
                            and corpusSize == 0 and failureSize > 0:
                        tables["defaultFailures"][rnd][project].add(test)
    resultDir:Path = Path('metadata') / "_".join(rounds)
    resultDir.mkdir(parents=True, exist_ok=True)
    for k, v in tables.items():
        with open(resultDir / f"{k}.csv", 'w') as f:
            f.write(",".join([''] + rounds) + "\n")
            for project in PROJECTS_MAPPING.keys():
                f.write(",".join([project] + [str(len(v[rnd][project])) for rnd in rounds]) + "\n")
        (resultDir / k).mkdir(exist_ok = True)
        for project in PROJECTS_MAPPING.keys():
            for rnd in rounds:
                with open(resultDir / k / f"{rnd}_{project}", "w") as f:
                    f.write('\n'.join(list(v[rnd][project])))

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python calculate_crashed.py <rounds>")
        exit(1)
    rounds: List[str] = sorted(sys.argv[1].split(','))
    main(rounds)
