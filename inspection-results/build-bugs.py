import csv, sys, json
from pathlib import Path

def main(file: Path, outputFile: Path):
    with open(file, 'r') as f:
        reader = csv.DictReader(f, delimiter='\t')
        with open(outputFile, 'w') as output:
            json.dump([{"bugId": line["Bug ID"], "type": line["Bug Type"], "project": line['Project']} for line in reader], output)

if __name__ == "__main__":
    inputFile = Path(sys.argv[1])
    main(inputFile, Path('bugs.json'))
