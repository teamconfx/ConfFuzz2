import sys, io, json
import matplotlib.pyplot as plt
from copy import deepcopy
from pathlib import Path
from infra import FuzzedFailure, PreviousFailure, getInspectedFailures, getBugs, isTrivial
from zipfile import ZipFile
from util import get_fp_config_dir, get_fp_result_dir
from typing import Dict, List, Tuple, Set
from collections import defaultdict
from config import FP_RESULT_DIR, PROJECTS_MAPPING, ROUNDS
from calculate_unique_failure import calculateForProject

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
    allBugs = getBugs()
    outputDir: Path = Path('meta_fp_filter')
    outputDir.mkdir(exist_ok=True)
    for cmpName, cmp in FuzzedFailure.metrics.items():
        failureData: defaultdict[int, defaultdict[str, int|float]] = defaultdict(lambda: defaultdict(int))
        for project, projId in PROJECTS_MAPPING.items():
            failureCounts: List[Tuple[PreviousFailure, int]] = []
            for previousFailure, fuzzedFailures in project_results[project]:
                failureCounts.append((previousFailure, sum(1 for ff in fuzzedFailures if cmp(ff, previousFailure))))
            rounds: List[str] = ROUNDS['confuzz']
            for fpThresh in range(RETRY_TIMES + 1):
                bugs = set(sum([uf.bugId for uf, fc in failureCounts if fc <= fpThresh and uf.naiveStatus(True) == "BUG"], []))
                nonTrivialBugs = {idx for idx in bugs if isTrivial(allBugs[idx])}
                assert all(idx.startswith('Bug-') for idx in nonTrivialBugs), nonTrivialBugs
                # count the actual FPs
                remainingFailures = []
                for pf, fc in failureCounts:
                    if fc > fpThresh:
                        pf = deepcopy(pf)
                        pf.status = "Filtered"
                    remainingFailures.append(pf)

                data = {rnd: calculateForProject(rnd, project, projId, remainingFailures) for rnd in rounds}
                failureData[fpThresh]['#Bugs'] += len(bugs)
                failureData[fpThresh]['#NontrivialBugs'] += len(nonTrivialBugs)
                failureData[fpThresh]['#AvgBugs'] += sum([len(x['#Bug']) for x in data.values()])/len(data)
                failureData[fpThresh]['#AvgNontrivialBugs'] += sum([len({idx for idx in x["#Bug"] if isTrivial(allBugs[idx])}) for x in data.values()])/len(data)
                failureData[fpThresh]['#FP'] += sum([x['FP'] for x in data.values()])/len(data)
                failureData[fpThresh]['Total'] += sum([x['Total'] for x in data.values()])/len(data)
                
        with open(outputDir/f"{cmpName}.csv", "w") as output:
            headers = ['#Bugs', "#NontrivialBugs", "#AvgBugs", "#AvgNontrivialBugs", '#FP', 'Total']
            output.write(','.join(['THRESH'] + headers + ['FPrate']) + '\n')
            for fpThresh in range(RETRY_TIMES + 1):
                data = failureData[fpThresh]
                output.write(','.join(map(lambda x: str(round(x, 2)), [fpThresh] + [data[header] for header in headers] + [data['#FP'] / data['Total']])) + '\n')

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python calculate_fp_filter.py <round>")
        exit(1)
    rnd = sys.argv[1]
    main(rnd)
