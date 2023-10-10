import json
from pathlib import Path

failures = []
for file in Path(".").glob("*.json"):
    with open(file) as f:
        failures += json.load(f)
#failures = list(filter(lambda x: x["status"] != "FP" and "Assertion" in x["type"], failures))
def f(a: dict):
    return all([len(str(x)) <= 3 or type(x) == bool for x in a.values()])
#print(len(list(filter(lambda x: f(x["min_config"]) and x["status"] != "FP", failures))))
#print(len(list(filter(lambda x: f(x["min_config"]) and x["status"] in ["Repeat-BUG", "BUG"], failures))))
types = set(map(lambda x: x["type"], failures))
output = []
for ty in types:
    fp = len([f for f in failures if f["status"] == "FP" and f['type'] == ty])
    al = len([f for f in failures if f["type"] == ty])
    output.append((round(fp/al, 3), fp, al, ty))
output.sort(key=lambda x: (x[0], x[1]))
print('\n'.join(map(lambda x: ','.join(map(lambda y: str(y), x)), output)))
print(len(failures))

#print(len(failures))


