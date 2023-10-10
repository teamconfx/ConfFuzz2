import os
import sys

# Find all file paths that under the given directory named "failures.csv"
def find_failures_csv(dir):
    res = []
    for root, dirs, files in os.walk(dir):
        for file in files:
            if file == "failures.csv":
                res.append(os.path.join(root, file))
    return res

# Parse a single failures.csv file, return content and skip first line of the file
# Return a empty list if the file only have one line
def parse_failures_csv(file):
    with open(file, "r") as f:
        lines = f.readlines()
        if len(lines) == 1:
            return []
        else:
            return lines[1:]


# Parse all failures.csv files under the given directory
def parse_all_failures_csv(dir):
    res = []
    for file in find_failures_csv(dir):
        res += parse_failures_csv(file)
    return res


# Create a new file named "summary.csv" under the given directory that stores 
# all return value from parse_all_failures_csv()
def create_summary_csv(dir, cur_dir):
    res = sorted(parse_all_failures_csv(dir))
    with open(os.path.join(cur_dir, "summary.csv"), "w") as f:
        f.write("test,failure_file,config,exception,message,stacktrace")
        for line in res:
            f.write(line)


if __name__ == '__main__':
    if len(sys.argv) != 2:
        raise ValueError("Usage: python3 parse_failures_csv.py <output_dir>")
    dir = sys.argv[1]
    cur_dir = os.getcwd()
    res = parse_all_failures_csv(dir)
    create_summary_csv(dir, cur_dir)

