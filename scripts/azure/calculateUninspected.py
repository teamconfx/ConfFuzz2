import pandas as pd
from pathlib import Path
from config import PROJECTS_MAPPING, NEW_DELIMITER
from collections import defaultdict
from remove_duplicated import main

rnd = "7292"

columns = ["Repeated", "FP", "", "Total"]
result = {}
for project, projId in PROJECTS_MAPPING.items():
    main(None, Path("tmp.csv"), Path(f"r{rnd}-output") / f"r{rnd}-{projId}-output" / "failures-output.csv", project)
    with open("tmp.csv") as f:
        next(f)
        statusCnt = defaultdict(int)
        for line in filter(lambda x: "REPRODUCIBLE" in x, f):
            statusCnt[line.split(NEW_DELIMITER)[1]] += 1
        result[project]=[statusCnt[c] for c in columns[:-1]] + [sum([v for v in statusCnt.values()])]
dataFrame = pd.DataFrame.from_dict(result, orient="index", columns=columns)
print(dataFrame)
print(sum(dataFrame["Total"]))

