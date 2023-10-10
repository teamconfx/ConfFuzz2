import sys, json, csv
from typing import List, Dict
from config import DATA_DIR, PROJECTS_MAPPING, PREV_RESULT_DIR, NEW_DELIMITER
from infra import Failure, PreviousFailure, UniqueFailure
from collections import defaultdict

cnt = 0
def calStats(failures):
    bugIds = set(f.bugId for f in failures)
    if '' in bugIds:
        bugIds.remove('')
    FPs = [failure for failure in failures if failure.status == "FP"]
    bugs = [failure for failure in failures if failure.status not in ["FP", "Not-Reproducible", "CONFUZZ"]]
    return [len(failures), len(FPs), len(bugIds), len(bugs) - len(bugIds)]

def genIntersectionStat(inspectedJson, newCsv):
    with open(inspectedJson) as f:
        prevFailures = [PreviousFailure(x) for x in json.load(f)]
    with open(newCsv) as f:
        newFailures = [Failure(x.split(NEW_DELIMITER)) for x in f.readlines()[1:] if "REPRODUCIBLE" in x]

    results_dict = defaultdict(list)
    for result in prevFailures + newFailures:
        results_dict[result].append(result)

    # NOTE: DUP is no longer checked here
    uniqueFailures: List[UniqueFailure] = [UniqueFailure(results) for results in results_dict.values()]
    commonUniqueFailures = []

    global cnt
    prevCommons = []
    newUnique = []
    for uniqueFailure in uniqueFailures:
        prevs = [failure for failure in uniqueFailure.failures if type(failure) == PreviousFailure]
        if 0 < len(prevs) < len(uniqueFailure.failures):
            commonUniqueFailures.append(uniqueFailure)
            prevCommons += [failure for failure in uniqueFailure.failures if type(failure) == PreviousFailure]
            cnt += 1
        elif len(prevs) == 0:
            newUnique.append(uniqueFailure)
        #elif len(prevs) == len(uniqueFailure.failures):
            #cnt += 1
        uniqueFailure.auto_debug()
    #cnt -= len(prevCommons)
        
    newCommons = []
    for uniqueFailure in commonUniqueFailures:
        for failure in uniqueFailure.failures:
            newFailure = PreviousFailure(failure = failure)
            newFailure.status = uniqueFailure.status
            newFailure.bugId = uniqueFailure.bugId
            newCommons.append(newFailure)

    newCommonStat = calStats(newCommons)
    prevCommonStat = calStats(prevCommons)
    prevTotalStat = calStats(prevFailures)

    return {
                "prevTotal": prevTotalStat,
                "prevCommon": prevCommonStat,
                "newCommon": newCommonStat
            }

if __name__ == "__main__":
    inspectedRnd, newRnd = sys.argv[1:]
    sections = ["prevTotal", "prevCommon", "newCommon"]
    allResults = {section: [] for section in sections}
    for section in sections:
        allResults[section].append(["project","#failures","#FPs","#Bugs","#Duplicates"])
    for project, projectId in PROJECTS_MAPPING.items():
        print(project)
        projectResult = genIntersectionStat(PREV_RESULT_DIR/f"r{inspectedRnd}/{project.lower()}.json",
                            DATA_DIR/f"r{newRnd}-output/r{newRnd}-{projectId}-output/failures-output.csv")
        for section in sections:
            allResults[section].append([project] + projectResult[section])

    result = [["project","#failures","#FPs","#Bugs","#Duplicates"]]
    for lineTotal, lineCommon in list(zip(allResults["prevTotal"], allResults["prevCommon"]))[1:]:
        print(lineTotal)
        result.append([lineTotal[0]] + [lineTotal[idx] - lineCommon[idx] for idx in range(1,5)])
    allResults["prevUnique"] = result
    sections.append("prevUnique")

    for section in sections:
        totalResult = ["Total"]
        for idx in range(1,5):
            totalResult.append(sum([line[idx] for line in allResults[section][1:]]))
        allResults[section].append(totalResult)
        for line in allResults[section][1:]:
            for idx in range(2,5):
                line[idx] = f"{line[idx]} ({round(line[idx]/line[1]*100, 1) if line[1] > 0 else 100.0}%)"
        with open(f"{section}.csv", "w") as f:
            for line in allResults[section]:
                f.write(",".join(map(lambda x: str(x), line)) + "\n")
    print(cnt)
