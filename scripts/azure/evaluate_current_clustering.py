# evaluate the effectiveness of current clustering based on the previous inspection
# cannot be stricter than previous clustering
import json, sys 
from typing import List
from collections import defaultdict
from pathlib import Path
from infra import PreviousFailure, UniqueFailure, Failure
from config import PREV_RESULT_DIR, PROJECTS_MAPPING, PREV_COMPRESSED_RESULT_DIR, RESULT_DIR, NEW_DELIMITER 

inspectedNum = defaultdict(int)
clusterSize = defaultdict(int)
def genClusteringScore(inspectedJson, failuresCsv):
    global inspectedNum
    with open(inspectedJson) as f:
        inspectedFailures: List[PreviousFailure] = [PreviousFailure(x) for x in json.load(f)]
#        print(len(inspectedFailures))

    with open(failuresCsv) as f:
        next(f)
        prevFailures: List[Failure] = [Failure(x.split(NEW_DELIMITER)) for x in f]

    results_dict = defaultdict(list)
    for result in prevFailures + inspectedFailures:
        results_dict[result].append(result)
    clusters: List[UniqueFailure] = [UniqueFailure(c, False) for c in results_dict.values() 
                                     if any([type(x) is Failure for x in c]) and c[0].reproStatus == "REPRODUCIBLE"]
    cnt = 0
    for cluster in clusters:
        clusterPrevFailures: List[PreviousFailure] = [c for c in cluster.failures if type(c) is PreviousFailure]
        if len(set(map(lambda x: x.naiveStatus(), clusterPrevFailures))) > 1:
            print(set(map(lambda x: x.naiveStatus(), clusterPrevFailures)))
            for status in set(map(lambda x: x.naiveStatus(), clusterPrevFailures)):
                #print(status)
                for f in clusterPrevFailures:
                    if f.naiveStatus() == status:
                        #print(f.dump())
                        break
            cnt += 1
        cluster.auto_debug()
        inspected = len(clusterPrevFailures) + (cluster.status == "Filtered")
        #if inspected == 0:
            #print(cluster.failures[0].dump())
        inspectedNum[inspected] += 1
        clusterSize[len(cluster.failures)] += 1

    return {
            "size": len(clusters),
            "nonFilteredSize": len([cluster for cluster in clusters if cluster.status != "Filtered"]),
            "fault": cnt,
            }
        

def main(resultsDir: Path, rnd: str):
    result = {}
    for project, projId in PROJECTS_MAPPING.items():
        result[project] = genClusteringScore(resultsDir / f"{project.lower()}.json", 
                                             RESULT_DIR / f"r{rnd}-output" / f"r{rnd}-{projId}-output" / "failures-output.csv")
    with open("output.csv", "w") as output:
        output.write(','.join(["project", "total", "size", "nonFilteredSize", 'mistakes']) + "\n")
        for project in PROJECTS_MAPPING.keys():
            with open(PREV_COMPRESSED_RESULT_DIR / resultsDir.name / f"{project.lower()}.json") as f:
                clusters = json.load(f)
                originalSize = len(clusters)
                nonFilteredSize = len([1 for cluster in clusters if cluster['status'] != "Filtered"])
            with open(PREV_RESULT_DIR / resultsDir.name / f"{project.lower()}.json") as f:
                totalSize = len(json.load(f))
            output.write(','.join([project, str(totalSize), f"{originalSize} -> {result[project]['size']}", f"{nonFilteredSize} -> {result[project]['nonFilteredSize']}", str(result[project]["fault"])]) + "\n")
        
if __name__ == "__main__":
    main(PREV_COMPRESSED_RESULT_DIR / f"r{sys.argv[1]}", sys.argv[1])
    print("inspectedNum:", inspectedNum, sum(inspectedNum.values()))
    print("sizeOfClusters:", clusterSize)
