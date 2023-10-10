# calculate how many failures do we need to inspect
import sys
from pathlib import Path
from typing import List, Dict, Set
from collections import defaultdict
from config import ALL_ROUNDS, PROJECTS_MAPPING, NEW_DELIMITER
from remove_duplicated import cluster_failures 
from infra import Failure, UniqueFailure 
from infra import dumpUniqueFailuresTo
from util import get_result_dir

def main(rounds: List[str]) -> None:
    assert all(rnd in ALL_ROUNDS for rnd in rounds)
    outputDir: Path = Path("uninspected")
    outputDir.mkdir(exist_ok=True)
    cnt = 0
    for project, projId in PROJECTS_MAPPING.items():
        clusters:defaultdict[UniqueFailure, Set[str]] = defaultdict(set)
        for rnd in rounds:
            with open(get_result_dir(rnd, projId) / "failures-output.csv") as f:
                next(f)
                results: List[Failure] = [Failure(line.strip().split(NEW_DELIMITER)) for line in f.readlines()]
            uniqueFailures: List[UniqueFailure] = cluster_failures(project, results, None, True)
            for uf in uniqueFailures:
                if uf.status == "" and uf.failures[0].reproStatus == "REPRODUCIBLE":
                    clusters[uf].add(rnd)
                elif uf.status in ["Repeated", "BUG"] and uf.bugId == "":
                    clusters[uf].add(rnd)
        dumpUniqueFailuresTo(outputDir / f"{project}.csv", list(clusters.keys()))
        cnt += len(clusters)
    print("#uninspected:", cnt)

if __name__ == "__main__":
    rounds: List[str] = sys.argv[1].split(',')
    main(rounds)
