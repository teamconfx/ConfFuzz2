from collections import defaultdict
from config import NEW_DELIMITER, PROJECTS_MAPPING, COLOR_MAPPING, RESULT_DIR, ROUNDS, ALL_ROUNDS
import matplotlib.pyplot as plt
import numpy as np
from typing import List, Dict, Set
from pathlib import Path
import sys
import json
from infra import PreviousFailure, getFailuresFromFile, UniqueFailure, Failure
from remove_duplicated import cluster_failures
from util import get_result_dir

def calculateForProject(rnd: str, project: str, projId: str, prevFailures: List[PreviousFailure]|None = None) -> Dict[str, int|float|Set[str]] | None:
    metrics = {"FP": "FP", "BUG": "Repeated"}
    result_dir = get_result_dir(rnd, projId)
    if not result_dir.exists():
        return None
    failures = getFailuresFromFile(result_dir / 'failures-output.csv')
    clusters: List[UniqueFailure] = [c for c in cluster_failures(project, failures, None, True, prevFailures) if c.failures[0].reproStatus == "REPRODUCIBLE" and c.status not in ("Filtered", "Non-Reproducible")]
    #assert not any(uf.status == "" for uf in clusters), f"{project}\n" + '\n'.join([f"{uf.dump()}\n"+'",\n"'.join(uf.failures[0].failure.stackTrace) for uf in clusters if uf.status==''])
    assert all([uf.status == "FP" or uf.bugId != [] for uf in clusters]), [uf.dump() for uf in clusters if uf.status != "FP" and uf.bugId == []]
    ret:Dict[str,float|int|Set[str]] = {"Total": sum(1 for uf in clusters if uf.status in metrics.values())}
    #ret:Dict[str,float|int] = {"Total": sum(len(uf.failures) for uf in clusters if uf.status in metrics.values())}
    for metric, st in metrics.items():
        ret[metric] = sum(1 for uf in clusters if uf.status == st)
        #ret[metric] = sum(len(uf.failures) for uf in clusters if uf.status == st)
    ret["#Bug"] = set(sum([uf.bugId for uf in clusters if uf.status == "Repeated"], []))
    # print the bug id, remove prefix "Bug-", and sorted by the id number
    # print(f"{project}: {sorted([int(bugId[4:]) for bugId in ret['#Bug']])}")
    return ret

def calculateForRound(rnd: str, prevFailures: List[PreviousFailure]|None = None) -> Dict[str, Dict[str, int|float|Set[str]]|None]:
    return {project: calculateForProject(rnd, project, projId, prevFailures) for project, projId in PROJECTS_MAPPING.items()}

def calculateForRounds(rounds: List[str], prevFailures: List[PreviousFailure]|None = None) -> Dict[str, Dict[str, Dict[str, int|float|Set[str]]|None]]:
    return {rnd: calculateForRound(rnd, prevFailures) for rnd in rounds}

def dumpTableToCsv(table: Dict[str, Dict[str, int|float|Set[str]]|None], output: Path, write_bug_id=False) -> None:
    bug_id: Dict[str, list[str]] = defaultdict(list)
    with open(output, "w") as f:
        colKeys = sorted(list(set(sum([list(x.keys()) for x in table.values()], []))))
        #colKeys = ["BUG", "BUG%", "FP", "FP%", "Total"]
        f.write(','.join(["project"] + colKeys) + '\n')
        for k,v in table.items():
            if v is None:
                continue
            out = [k]
            for ck in colKeys:
                if ck not in v:
                    out.append('')
                else:
                    value = v[ck]
                    if type(value) is int:
                        out.append(str(value))
                    elif type(value) is float:
                        out.append(f"{round(value*100, 2)}%")
                    elif type(value) is set:
                        if write_bug_id:
                            # print(f"{k}: {sorted([int(bugId[4:]) for bugId in value])}")
                            bug_id[k] = sorted([int(bugId[4:]) for bugId in value])
                        out.append(str(len(value)))
                    else:
                        raise ValueError("should not reach here!")
            f.write(','.join(out) + '\n')
    if write_bug_id:
        with open(str(output).replace('.csv', '') + "_bug_id.json", "w") as f:
            json.dump(bug_id, f)

            
def main(rounds: List[str]):
    tag_rounds:defaultdict[str, List[str]] = defaultdict(list)
    assert all(rnd in ALL_ROUNDS for rnd in rounds)
    for rnd in rounds:
        for tag, rnds in ROUNDS.items():
            if rnd in rnds:
                tag_rounds[tag].append(rnd)
                break

    outputDir = Path("meta_failures")
    tag_results = {tag: calculateForRounds(rnds) for tag,rnds in tag_rounds.items()}
    for tag, rnd_results in tag_results.items():
        cnt: Dict[str, Dict[str, int|float|Set[str]]] = {project: {"Total": 0, "FP": 0, "BUG": 0, "#Bug": set()} for project in PROJECTS_MAPPING}
        for rnd, result in rnd_results.items():
            if result is not None:
                dumpTableToCsv(result, outputDir / f"{rnd}.csv")
                for project, project_result in result.items():
                    if project_result != None:
                        for k in ["Total", "FP", "BUG", "#Bug"]:
                            if type(cnt[project][k]) is set:
                                cnt[project][k] = cnt[project][k] | project_result[k]
                            else:
                                cnt[project][k] += project_result[k]
        for result in cnt.values():
            result["FP%"] = result["FP"] / result["Total"]
            result["BUG%"] = result["BUG"] / result["Total"]
        dumpTableToCsv(cnt, outputDir / f"{tag}.csv", True)



if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python calculate_unique_failures <rounds>")
        exit(1)
    rounds = sys.argv[1].split(",")
    main(rounds)
