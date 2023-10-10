# This script requires the overall bug csv data inside meta_failures
# For example: meta_failures/confuzz.csv
import os, sys
sys.path.append("../azure/")
from typing import List, Dict
from config import ROUNDS, EVALUATED_PROJECTS, FIGURE_DIR
from pathlib import Path
from util import get_bug_csv
from collections import defaultdict
from plot_util import draw_bar_plot

# each mode has a {mode}.csv file in meta_failures
# that contains the overall bug results for each project
def read_data_from_csv(mode: str, failure_or_bug: str="bug") -> Dict[str, int]:
    bug_dict: Dict[str, int] = defaultdict(int)
    bug_csv_path: Path = get_bug_csv(mode)
    with open(bug_csv_path, 'r') as f:
        # skip the first line
        for line in f.readlines()[1:]:
            project = line.strip().split(",")[0]
            if failure_or_bug == "bug":
                print("bug")
                bug_dict[project] = int(line.strip().split(",")[1])
            elif failure_or_bug == "failure":
                print("failure")
                bug_dict[project] = int(line.strip().split(",")[-1])
    return bug_dict

def generate_oveall_data_csv(modes: List[str], failure_or_bug: str) -> str: 
    # assert that all mode in modes are in config.ROUNDS
    #assert all([mode in ROUNDS for mode in modes])
    # key: mode, value: bug_dict for that mode
    all_bug_dict: Dict[str, Dict[str, int]] = defaultdict(dict)
    for mode in modes:
        print(read_data_from_csv(mode, failure_or_bug))
        all_bug_dict[mode] = read_data_from_csv(mode, failure_or_bug)
    # generate a csv-format string
    csv_str: str = "project," + ",".join(modes) + "\n"
    for proj in EVALUATED_PROJECTS:
        csv_str += proj + "," + ",".join([str(all_bug_dict[mode][proj]) for mode in modes]) + "\n"
    print(csv_str)
    return csv_str
    
def draw_bug_bar_plot(title: str, csv_data_str: str, output_file_name: str, failure_or_bug: str) -> None:
    y_label = "Bug Count" if failure_or_bug == "bug" else "Failure Count"
    draw_bar_plot(title, "Projects", y_label, csv_data_str, FIGURE_DIR / "bugs" / f"{output_file_name}_{failure_or_bug}")
    
if __name__ == "__main__":
    if len(sys.argv) < 4:
        print("Usage: python3 draw_bug_bar_plot.py [bug_or_failure] [output_pdf_name] [mode1] [mode2] ...")
        sys.exit(1)

    failure_or_bug, title, figure_pdf_name = sys.argv[1:4]
    modes: List[str] = sys.argv[4:]
    draw_bug_bar_plot(title, generate_oveall_data_csv(modes, failure_or_bug), figure_pdf_name, failure_or_bug)
        
    
