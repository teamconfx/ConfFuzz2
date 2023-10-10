import sys
sys.path.append('../azure/')
import json
import matplotlib.pyplot as plt
from matplotlib_venn import venn3
from plot_util import draw_venn_plot
from typing import List, Set, Dict
from pathlib import Path
from util import get_bug_id_json 
from collections import defaultdict
from config import FIGURE_DIR

def read_bug_id_from_json(mode: str) -> Set[str]:
    bug_id_set: Set[str] = []
    bug_id_json_path: Path = get_bug_id_json(mode)
    # read bug id from json file
    bug_id_json: Dict[str, List[str]] = json.loads(bug_id_json_path.read_text())
    bug_id_set = {bug_id for bug_id_list in bug_id_json.values() for bug_id in bug_id_list}
    return bug_id_set

        
def generate_overall_data_sets(modes: List[str]) -> Dict[str, Set[str]]:
    overall_data_sets: Dict[str, Set[str]] = defaultdict(set)
    for mode in modes:
        overall_data_sets[mode] = read_bug_id_from_json(mode)
    return overall_data_sets


def draw_bug_venn_plot(title: str, modes: List[str], output_file_name: str):
    overall_data_sets: Dict[str, Set[str]] = generate_overall_data_sets(modes)
    draw_venn_plot(title, overall_data_sets, FIGURE_DIR / "bugs" / f"{output_file_name}_bug_venn")


if __name__ == '__main__':
    if len(sys.argv) < 4:
        print("Usage: python3 draw_bug_venn_plot.py <title> <output_name> <mode1> <mode2> ...")
        sys.exit(1)
    title, output_name = sys.argv[1:3]
    modes: List[str] = sys.argv[3:]
    draw_bug_venn_plot(title, modes, output_name)
