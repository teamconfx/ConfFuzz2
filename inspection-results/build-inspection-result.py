import sys, json
from pathlib import Path

# line should be like: * status time test type message trace reprotype replay replay replay config
def transform_line(line) -> dict:
    config = json.loads(line[-2])
    assert line[1] not in ["BUG", "Repeated"] or line[0] != ''
    return {
            "status": line[2] if line[2] not in ["BUG", "Repeated"] else "BUG",
            "bugId": [b.strip() for b in sorted(line[1].split(','))] if line[2] in ["BUG", "Repeated"] else [],
            "testClass": line[4].split('#')[0],
            "testMethod": line[4].split("#")[1],
            "failure": line[5],
            "errorMessage": line[6],
            "stackTrace": [trace.strip() for trace in line[7].split(",") if trace != ""],
            "reproStatus": line[8],
            "minConfig": config
           }

def transform_file(file):
    meta_len = 4
    #print(file)
    with open(file) as f:
        next(f)
        lines = list(map(lambda x: [s.strip() for s in x.split("\t")], filter(lambda x: x.strip() != "", f.readlines())))
        #print(lines)
        entries = []
        statuses = [line[8] for line in lines]
        assert all([s in ('REPRODUCIBLE', 'FLAKY', 'DIFFERENT', 'POLLUTED', 'TIMEOUT', 'PASS') for s in statuses]), set(statuses)
        for line in filter(lambda x: x[8] == "REPRODUCIBLE" and x[2] != "Filtered", lines):
            try:
                entries.append(transform_line(line[:meta_len] + line[meta_len:meta_len+11]))
            except AssertionError as e:
                print(file)
                print(line)
                print(e)
    return entries

def build_dataset(round_sign):
    raw_dir = Path('raw') / round_sign
    if not raw_dir.exists():
        print(f"round {round_sign} does not exist! check raw/ again")
        return
    (Path('compressed-result') / round_sign).mkdir(exist_ok=True)
    for file in raw_dir.glob("*.tsv"):
        print(str(file))
        result = transform_file(file)
        output_file_name: str = str(file.name).split('-')[2].split('.')[0][1:].lower()
        output_file = Path("compressed-result") / round_sign / f"{output_file_name}.json"
        with open(output_file, "w") as output:
            json.dump(result, output, indent = 4)


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python build-inspection-result.py <round>")
        print("e.g., python build-inspection-result.py r6284")
        exit(1)
    build_dataset(sys.argv[1])

