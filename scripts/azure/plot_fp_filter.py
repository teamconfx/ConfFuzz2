import sys, io, json
import matplotlib.pyplot as plt
from copy import deepcopy
from pathlib import Path
from infra import FuzzedFailure, PreviousFailure, getInspectedFailures
from zipfile import ZipFile
from util import get_fp_config_dir, get_fp_result_dir
from typing import Dict, List, Tuple, Set
from collections import defaultdict
from config import FP_RESULT_DIR, PROJECTS_MAPPING, ROUNDS
from calculate_unique_failure import calculateForProject, calculateForRounds

RETRY_TIMES = 10

def main(rnd: str):
    # First do the healthcheck
    config_counts: Dict[str, int] = {}
    for project in PROJECTS_MAPPING:
        config_counts[project] = len(list(get_fp_config_dir(project).iterdir()))
    assert (FP_RESULT_DIR / f"r{rnd}").exists(), f"{FP_RESULT_DIR}/r{rnd} does not exist!"
    result_counts: defaultdict[str, int] = defaultdict(int)
    for project in PROJECTS_MAPPING:
        for resultZip in get_fp_result_dir(rnd, project).glob("*.zip"):
            with ZipFile(resultZip) as zipFile:
                result_counts[project] += len([name for name in zipFile.namelist() if name.endswith("json")\
                        and 'fuzzDebugFailures' in name])
    assert all([result_counts[k] == config_counts[k] for k in config_counts]), \
            [(result_counts[k], config_counts[k]) for k in config_counts if result_counts[k] != config_counts[k]] 

    
    # build the list of results for each project
    project_results: defaultdict[str, List[Tuple[PreviousFailure, List[FuzzedFailure]]]] = defaultdict(list)
    for project in PROJECTS_MAPPING:
        # first, get previous failures
        previousFailures:List[PreviousFailure] = getInspectedFailures(project)
        addedFailures:List[PreviousFailure] = []
        for resultZip in get_fp_result_dir(rnd, project).glob("*.zip"):
            with ZipFile(resultZip) as zipFile:
                for resultJson in filter(lambda x: x.endswith("json") and "fuzzDebugFailures" in x, zipFile.namelist()):
                    testName = resultJson.split('/')[-3:-1]
                    with io.TextIOWrapper(zipFile.open(resultJson), 'utf-8') as f:
                        fuzzedFailures:List[FuzzedFailure] = [FuzzedFailure(x, *testName) 
                                                               for x in json.load(f)]
                        assert len(fuzzedFailures) == RETRY_TIMES, (project, resultZip, resultJson)

                    # now get the original config
                    minConfigFile = get_fp_config_dir(project) / resultJson.split('_')[-1]
                    assert minConfigFile.exists()
                    with open(minConfigFile) as f:
                        minConfig = json.loads(next(f))
                        configTestName = next(f)

                    # next find the inspected failure
                    assert len([pf for pf in previousFailures if pf.minConfig == minConfig and pf.testName() == configTestName and all(af != pf or af.testName() != pf.testName() for af in addedFailures)]) >= 1, \
                        (project, minConfig, minConfigFile, configTestName, [pf.dump() for pf in previousFailures if pf.minConfig == minConfig and pf.testName() == configTestName])
                    previousFailure = [pf for pf in previousFailures if pf.minConfig == minConfig and pf.testName() == configTestName and all(af != pf or af.testName() != pf.testName() for af in addedFailures)][0]
                    assert configTestName.split('#') == testName, (resultZip, resultJson, minConfig,(configTestName, '#'.join(testName)))
                    addedFailures.append(previousFailure)
                    project_results[project].append((previousFailure, fuzzedFailures))
    
    # calculate the Total FP rate for the inspected ones for each project
    outputDir: Path = Path('fp_filter_figures')
    outputDir.mkdir(exist_ok=True)
    for project, projId in PROJECTS_MAPPING.items():
        failureCounts: List[Tuple[PreviousFailure, int]] = []
        for previousFailure, fuzzedFailures in project_results[project]:
            failureCounts.append((previousFailure, sum(1 for ff in fuzzedFailures if ff == previousFailure)))
        plot: defaultdict[str, List[int|float]] = defaultdict(list)
        rounds: List[str] = ROUNDS['confuzz']
        for fpThresh in range(RETRY_TIMES + 1):
            # first the FP rate of the inspected ones
            #totalNum = sum(1 for x in failureCounts if x[1] <= fpThresh)
            #fpNum = sum(1 for pf, fc in failureCounts if fc <= fpThresh and pf.naiveStatus() == "FP")
            #plot['grossFPrate'].append(fpNum / totalNum if totalNum > 0 else 0)
            # count the number of bugs
            bugs = set(sum([uf.bugId for uf, fc in failureCounts if fc <= fpThresh and uf.naiveStatus(True) == "BUG"], []))
            plot["#Bugs"].append(len(bugs))
            # count the actual FPs
            remainingFailures = []
            for pf, fc in failureCounts:
                if fc > fpThresh:
                    pf = deepcopy(pf)
                    pf.status = "Filtered"
                remainingFailures.append(pf)
            data = {rnd: calculateForProject(rnd, project, projId, remainingFailures) for rnd in rounds}
            fpRates = {rnd: (x['FP'] / x['Total'] if x['Total'] > 0 else 0) for rnd, x in data.items()}
            plot['FPrate'].append(sum(fpRates.values()) / len(fpRates))
            
        colors = ["red", "green", "orange"]
        lineTags = []
        _, ax1 = plt.subplots()
        for idx,(k,v) in enumerate(plot.items()):
            ax = ax1 if idx == 0 else ax1.twinx()
            line, = ax.plot(range(RETRY_TIMES+1), v, label=k, color=colors[idx])
            lineTags.append((line, k))
            ax.set_ylabel(k, color=colors[idx])
            ax.tick_params(axis='y', labelcolor=colors[idx])
        plt.title(project)
        plt.legend(*zip(*lineTags))
        plt.savefig(outputDir / f'{project}.pdf', format="pdf")
        plt.clf()

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python plot_fp_filter.py <round>")
        exit(1)
    rnd = sys.argv[1]
    main(rnd)
