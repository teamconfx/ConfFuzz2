import json
from infra import PreviousFailure
from pathlib import Path
from config import PREV_RESULT_DIR

output = open("inspected-stat.csv", "w")
output.write("project,#failures,#FPs,#Bugs,#Duplicates\n")

def printStats(failures, project):
    global output
    bugIds = set(f.bugId for f in failures)
    bugIds.remove('')
    FPs = [failure for failure in failures if failure.status == "FP"]
    bugs = [failure for failure in failures if failure.status not in ["FP", "Not-Reproducible", "CONFUZZ"]]
    output.write(','.join([project, str(len(failures)), f"{len(FPs)} ({round(len(FPs)/len(failures)*100, 1)}%)",
                    f"{len(bugIds)} ({round(len(bugIds)/len(failures)*100, 1)}%)", 
                    f"{len(bugs) - len(bugIds)} ({round((len(bugs) - len(bugIds))/len(failures)*100, 1)}%)"]) + '\n')

inspectedNum = 0
for round_dir in filter(lambda x: x.name.startswith('r'), Path(PREV_RESULT_DIR).iterdir()):
    print(round_dir.name, end=': ')
    all_failures = []
    for project_json in filter(lambda x: x.name.endswith('.json'), round_dir.iterdir()):
        project = project_json.name[:-5]
        with open(project_json) as f:
            failures = [PreviousFailure(d) for d in json.load(f)]
        printStats(failures, project)
        all_failures += failures
    printStats(all_failures, "Total")

