import sys
sys.path.append('../azure/')
from pathlib import Path
from collections import defaultdict
from io import StringIO
from typing import Dict
from util import get_mode
from config import MODE_COLOR
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from typing import Set, List
from matplotlib_venn import venn3

color_list = ['orange', 'blue', 'green', 'red', 'gray']


def draw_venn_plot(title: str, sets: Dict[str, Set], save_path: Path):
    sets_list, sets_lable = [], []
    for k, v in sets.items():
        sets_list.append(v)
        sets_lable.append(k)
        
    venn = venn3(sets_list, sets_lable)
    plt.title(title)
    print(f"save_path: {save_path}")
    plt.savefig(f"{save_path}.pdf")
    plt.savefig(f"{save_path}.png")
    # plt.show()
        

def draw_bar_plot(title: str, x_lable: str, y_lable: str, data: str, save_path: Path):
    # New data in CSV-like format
    '''
    data = """project,total,C_total,R_total,P_total,D_total
    Hive,18,15,15,14,10
    HCommon,40,34,32,33,27
    HDFS,41,32,25,35,23
    HBase,25,20,19,17,19
    MapReduce,14,12,14,14,11
    Yarn,16,15,12,12,10
    Kylin,5,5,5,5,2
    Zeppelin,4,4,3,4,3"""
    '''
    if not save_path.parent.exists():
        save_path.parent.mkdir(parents=True)
        
    # Define colors for each metric
    modes = data.split('\n')[0].split(',')[1:]
    print(modes)
    for i, mode in enumerate(modes):
        # check whether mode is numeric string
        if mode.isnumeric():
            modes[i] = get_mode(mode)
    print(modes)

    colors: Dict[str, str] = {}
    for i, mode in enumerate(modes):
        colors[mode] = MODE_COLOR[mode] if mode in MODE_COLOR else color_list[i]
    print(colors)

    # Read data into a DataFrame
    df = pd.read_csv(StringIO(data))

    # Number of metrics (excluding 'project' columns)
    n_metrics = len(df.columns) - 1
    barWidth = 0.1

    # Set position of bars on X axis
    ind = np.arange(len(df['project']))
    positions = [ind + barWidth * i for i in range(n_metrics)]

    # Plotting
    plt.figure(figsize=(8, 6))

    # Create bars for each metric
    for i, column in enumerate(df.columns[1:]):  # Skip 'project' columns
        plt.bar(positions[i], df[column], width=barWidth, label=get_mode(column), color=MODE_COLOR[get_mode(column)])

    # Add some text for labels, title, and custom x-axis tick labels, etc.
    plt.title(title)
    plt.xlabel(x_lable)
    plt.ylabel(y_lable)
    plt.xticks(ind + barWidth, df['project'], rotation=45)
    plt.legend()

    plt.tight_layout()
    plt.savefig(f"{save_path}.pdf")
    plt.savefig(f"{save_path}.png")
    plt.show()
