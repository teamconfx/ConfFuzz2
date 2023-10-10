# This script removes duplicated failures from xxx-failures.csv file
import os, sys, json, config, csv, re
from typing import List
from pathlib import Path
from infra import Failure, UniqueFailure, PreviousFailure
from infra import getInspectedFailures, dumpUniqueFailuresTo
from collections import defaultdict

def cluster_failures(project: str|None, results: List[Failure], output_file: Path|None, considerNotReproducible: bool = False, prev_results: List[PreviousFailure]|None = None):
    # load previous results
    if prev_results is None:
        prev_results = getInspectedFailures(project)
    # NOTE: we do not add status here to make the clustering most effective

    # group by TYPE, REPRO, MIN_CONFIG, TOP_STACK must be the same (diff -> diff)
    results_dict = defaultdict(list)
    for result in results + prev_results:
        results_dict[result].append(result)

    # NOTE: DUP is no longer checked here
    clusters: List[UniqueFailure] = [UniqueFailure(c, True, considerNotReproducible) for c in results_dict.values() 
                                     if any([type(x) is Failure for x in c])]

    # write to file
    if output_file != None:
        dumpUniqueFailuresTo(output_file, clusters)
    return clusters

def main(inputs: List[dict]|None, output_file, input_file: Path|None = None, project = None):
    assert inputs != None or input_file != None
    if inputs == None and input_file != None:
        with open(input_file) as f:
            next(f)
            results = [Failure(line.strip().split(config.NEW_DELIMITER)) for line in f.readlines()]
        return cluster_failures(project, results, output_file)
    elif inputs != None:
        results = [Failure(x) for x in inputs]
        return cluster_failures(project, results, output_file)
    assert False, "should not reach here"


if __name__ == "__main__":
    """
    if len(sys.argv) != 3:
        print("Usage: python3 filter.py <input_file> <output_file>")
        exit(1)
        """
    main(None, Path(sys.argv[1]), input_file = Path(sys.argv[2]), project = sys.argv[3])
