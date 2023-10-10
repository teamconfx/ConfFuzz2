# This script requires the overall bug csv data inside meta_failures
# For example: meta_failures/confuzz.csv
import os, sys
sys.path.append("../azure/")
from typing import List, Dict
from config import ROUNDS, EVALUATED_PROJECTS, FIGURE_DIR
from pathlib import Path
from util import get_cov_last_point_csv
from collections import defaultdict
from plot_util import draw_bar_plot

# each mode has a {mode}.csv file in meta_failures
# that contains the overall bug results for each project
def read_data_from_csv() -> Dict[str, int]:
    # cov_dict[proj][mode] = cov_num
    cov_dict: Dict[str, Dict[str, int]] = defaultdict(dict)
    cov_csv_path: Path = get_cov_last_point_csv()
    with open(cov_csv_path, "r") as f:
        for line in f.readlines()[1:]:
            proj, mode, cov_num = line.strip().split(",")[0:3]
            cov_dict[mode][proj] = float(cov_num)
    return cov_dict

def generate_oveall_data_csv(cov_dict: Dict[str, int]) -> str: 
    # assert that all mode in modes are in config.ROUNDS
    #assert all([mode in ROUNDS for mode in modes])
    # key: mode, value: bug_dict for that mode
    # generate a csv-format string
    
    modes = list(cov_dict.keys())
    csv_str: str = "project," + ",".join(modes) + "\n"
    for proj in EVALUATED_PROJECTS:
        csv_str += proj + "," + ",".join([str(cov_dict[mode][proj]) for mode in modes]) + "\n"
    print(csv_str)
    return csv_str
    
def draw_bug_bar_plot(title: str, csv_data_str: str, output_file_name: str) -> None:
    draw_bar_plot(title, "Projects", "Coverage Count After 30 Minutes ", csv_data_str, FIGURE_DIR / "coverage" / f"{output_file_name}_coverage")
    
if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python3 draw_bug_bar_plot.py [title] [output_pdf_name]")
        sys.exit(1)
    title, figure_pdf_name = sys.argv[1:]
    draw_bug_bar_plot(title, generate_oveall_data_csv(read_data_from_csv()), figure_pdf_name)
        
    
