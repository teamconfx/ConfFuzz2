from config import NEW_DELIMITER, PROJECTS_MAPPING, TEST_NUM
import matplotlib.pyplot as plt
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

    # Set up figure and axis
    fig, ax = plt.subplots()

    # Set the width of the bars
    bar_width = 0.2
    index = range(len(projects))

    # Create bars for each tag
    for i, tag in enumerate(tags):
        ax.bar([pos + i * bar_width for pos in index], counts[tag], bar_width, label=tag)

    # Add labels, title, and legend
    ax.set_xlabel('Projects')
    ax.set_ylabel('Counts')
    ax.set_title('#Unique Failures Each Tool Found')
    ax.set_xticks([pos + 0.5 * bar_width for pos in index])
    ax.set_xticklabels(projects, fontsize=8)
    ax.legend()

    # Show the plot
    # plt.tight_layout()



def main(result_dirs: dict):
    failure_numbers = {key: {} for key in PROJECTS_MAPPING}
    failed_tests = {}
    for tag, result_dir in result_dirs.items():
        #print(str(result_dir))
        for failure_file in result_dir.glob("**/failures-output.csv"):
            project = get_project(failure_file)
            failed_tests[project] = set()
            with open(failure_file) as f:
                for line in f:
                    line = line.split(NEW_DELIMITER)
                    if line[0] not in failed_tests[project]:
                        failed_tests[project].add(line[0])
                    #print(line[1], line[7])
            """
                failure_numbers[get_project(unique_failure_file)][tag] = len(list(filter(
                    lambda x: x[1] == "" and x[6] == "REPRODUCIBLE",
                    map(lambda x: x.split(NEW_DELIMITER), f)
                    )))
            """
    print("\n".join([f"{k}: {len(v)}, {len(v)/TEST_NUM[k]}" for k,v in failed_tests.items()]))
    #plot_bar_figure(failure_numbers)

if __name__ == "__main__":
    data_dir = Path(sys.argv[1])
    tag_round_str = sys.argv[2:]
    tag_round = list(map(lambda x: x.split('='), tag_round_str))

    main({t: data_dir/f"r{rnd}-output" for t,rnd in tag_round})
    #plt.savefig(sys.argv[-1] if sys.argv[-1].endswith(".pdf") else "failures.pdf", format='pdf')

