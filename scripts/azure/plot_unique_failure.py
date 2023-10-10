from config import NEW_DELIMITER, PROJECTS_MAPPING, COLOR_MAPPING, RESULT_DIR
import matplotlib.pyplot as plt
import numpy as np
from pathlib import Path
import sys

def get_project(unique_file: Path):
    assert any([identifier in unique_file.parent.name for identifier in PROJECTS_MAPPING.values()])
    return next(filter(lambda x: x[1] in unique_file.parent.name, PROJECTS_MAPPING.items()))[0]

def plot_bar_figure(data):
    # Extract project names and corresponding tags and counts
    projects = list(data.keys())
    tags = list(data[projects[0]].keys())
    counts = {tag: [data[project][tag] for project in projects] for tag in tags}
    errors = {k: list(map(lambda x: np.std(x), v)) for k,v in counts.items()}
    counts = {k: list(map(lambda x: np.mean(x), v)) for k,v in counts.items()}
    print(errors)

    # Set up figure and axis
    fig, ax = plt.subplots()

    # Set the width of the bars
    bar_width = 0.2
    index = range(len(projects))

    # Create bars for each tag
    for i, tag in enumerate(tags):
        ax.bar([pos + (i-0.5) * bar_width for pos in index], counts[tag], bar_width, label=tag, color=COLOR_MAPPING[tag], yerr = errors[tag])

    # Add labels, title, and legend
    ax.set_xlabel('Projects')
    ax.set_ylabel('Count')
    ax.set_title('#Unique Failures Each Tool Found (normalized over Zest)')
    ax.set_xticks([pos + 0.5 * bar_width for pos in index])
    ax.set_xticklabels(projects, fontsize=8)
    ax.legend()

    # Show the plot
    # plt.tight_layout()

def main(result_dirs: dict):
    failure_numbers = {key: {tag: [] for tag in result_dirs.keys()} for key in PROJECTS_MAPPING}
    for tag, result_dir_list in result_dirs.items():
        for result_dir in result_dir_list:
            for unique_failure_file in result_dir.glob("**/unique_failures.csv"):
                with open(unique_failure_file) as f:
                    cnt = 0
                    for line in f:
                        line = line.split(NEW_DELIMITER)
                        #print(line[1], line[7])
                        if line[7] == "REPRODUCIBLE":
                        #if line[1] == "" and line[7] == "REPRODUCIBLE":
                            cnt += 1
                failure_numbers[get_project(unique_failure_file)][tag].append(cnt)
            """
                failure_numbers[get_project(unique_failure_file)][tag] = len(list(filter(
                    lambda x: x[1] == "" and x[6] == "REPRODUCIBLE",
                    map(lambda x: x.split(NEW_DELIMITER), f)
                    )))
            """
    #print(failure_numbers)
    """
    for project, tag_dict in failure_numbers.items():
        denom = tag_dict["Zest"]
        for tag in tag_dict:
            tag_dict[tag] /= denom
            #failure_numbers[get_project(unique_failure_file)][tag] = cnt
    """
    #print(failure_numbers)
    plot_bar_figure(failure_numbers)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python calculate_unique_failures <tag1>=<rounds1> <tag2>=<rounds2>...")
        exit(1)
    data_dir = RESULT_DIR
    tag_round_str = sys.argv[1:]
    tag_round = list(map(lambda x: (x[0], x[1].split(',')), map(lambda x: x.split('='), tag_round_str)))

    main({t: [data_dir/f"r{rnd}-output" for rnd in rnds] for t,rnds in tag_round})
    plt.savefig(sys.argv[-1] if sys.argv[-1].endswith(".pdf") else "failures.pdf", format='pdf')

