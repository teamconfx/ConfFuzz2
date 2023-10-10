import os, sys
from bisect import bisect_right
import config
from pathlib import Path
from itertools import groupby
import matplotlib.pyplot as plt
import numpy as np
import json
# find and parse all summary.txt files in the given directory recursively
common_tests = set()

def plot_smooth_figure(data, label):
    # Separate x and y values from the data
    x_values, y_values = zip(*data)

    # Generate a smooth curve using a higher resolution of x values
    x_smooth = np.linspace(min(x_values), max(x_values), 18000)
    y_smooth = np.interp(x_smooth, x_values, y_values)


    # Plot the smooth curve
    plt.plot(x_smooth, y_smooth, label=label)

def get_time_cov_pairs(plot_data: Path):
    test_name = plot_data.parents[2].name + "#" + plot_data.parents[1].name
    with open(plot_data) as infile:
        data = [tuple(map(lambda x: x.strip(), line.split(','))) for line in infile.readlines()[1:]]
    # time, cov
    return test_name, [(int(line[0]), float(line[-5][:-1])) for line in data]

def is_valid(plot_data: Path):
    test_name = plot_data.parents[2].name + "#" + plot_data.parents[1].name
    return test_name, get_time_cov_pairs(plot_data)[1] != []

def put_figure(search_dir, label):
    global common_tests
    all_results = {}
    all_times = set()
    cnt = 0
    for summary_file in search_dir.glob("**/plot_data"):
        test_name, result = get_time_cov_pairs(summary_file)
        assert test_name not in common_tests or result != [], test_name
        if result == [] or test_name not in common_tests:
            continue
        cnt += 1
        #print(test_name, result)
        init_time = int(result[0][0])
        transformed_result = {}
        times = []
        maximum = 0
        for time, pairs in groupby(result, lambda x: x[0]):
            pairs = list(pairs)
            size = len(pairs)
            for idx, pair in enumerate(pairs):
                transformed_result[time - init_time + (idx / size)] = pair[1]
                maximum = max(maximum, pair[1])
                times.append(time - init_time + (idx / size))
                all_times.add(time - init_time + (idx / size))
        transformed_result[1800] = maximum
        times.append(1800)
        all_results[test_name] = (times, transformed_result)
    print("valid:", cnt, label)
    # prepare the dots
    all_times = sorted(list(all_times))
    dots = []
    prev = {test: -1 for test in all_results.keys()}
    for idx, time in enumerate(all_times):
        #print(f"{idx} / {len(all_times)}")
        avg = 0
        for test, result in all_results.items():
            index = bisect_right(result[0], time) - 1
            assert index >= prev[test], (index, prev[test])
            assert result[0][index] <= time, (result[0], time, result[0][index], result[0][index+1])
            if index+1 < len(result[0]):
                assert result[0][index+1] > time, (result[0], time, result[0][index], result[0][index+1])
            #print(index, len(result[0]), len(result))
            avg += result[1][result[0][index]]
            prev[test] = index
        avg /= len(all_results)
        dots.append((time, avg))
    plot_smooth_figure(dots, label)
    return dots


def get_result(project, search_dir: Path, search_dir1, search_dir2, output = "figure.pdf"):
    global common_tests
    confuzz_tests = set()
    jqf_tests = set()
    random_tests = set()
    for summary_file in search_dir.glob("**/plot_data"):
        test_name, result = is_valid(summary_file)
        if result:
            confuzz_tests.add(test_name)
    for summary_file in search_dir1.glob("**/plot_data"):
        test_name, result = is_valid(summary_file)
        if result:
            jqf_tests.add(test_name)
    for summary_file in search_dir2.glob("**/plot_data"):
        test_name, result = is_valid(summary_file)
        if result:
            random_tests.add(test_name)
    common_tests = {a for a in jqf_tests if a in confuzz_tests and a in random_tests}

    print(len(common_tests))

    # Create the plot and set the figure size
    plt.figure(figsize=(8, 6))


    dots = {}
    dots['jqf'] = put_figure(search_dir1, "jqf")
    dots['confuzz'] = put_figure(search_dir, "confuzz")
    dots['random'] = put_figure(search_dir2, "random")

    with open(output.parents[0]/f"{project}.json", "w") as f:
        json.dump(dots, f)

    # Set plot title and labels
    plt.title(project)
    plt.xlabel('Time (s)')
    plt.ylabel('Percentage of Coverage Hash Table Occupied')

    # Display a legend
    plt.legend()

    # Show the plot
    plt.savefig(output, format='pdf')


if __name__ == "__main__":
    if len(sys.argv) != 5:
        print("Usage: python3 plot.py data_dir confuzz_round jqf_round random_round")
        sys.exit(1)
    projects = ["alluxiocommon","hadoophadoopcommon","hadoophadoopmapreduceclientcore","hbasehbaseserver","kylincorecube","flinkflinkcore","hadoophadoophdfs","hadoophadoopyarncommon","hiveql","zeppelinzeppelinzengine"]
    data_dir, confuzz_round, jqf_round, random_round = sys.argv[1:]
    data_dir = Path(data_dir)
    figures_dir = data_dir/f"r{confuzz_round}_{jqf_round}_{random_round}_figures"
    figures_dir.mkdir(parents=True, exist_ok=True)
    
    for project in projects:
        print(project)
        new = data_dir/f"r{confuzz_round}/r{confuzz_round}-{project}/r{confuzz_round}-{project}-output/"
        jqf = data_dir/f"r{jqf_round}-JQF/r{jqf_round}-{project}/r{jqf_round}-{project}-output/"
        random = data_dir/f"r{random_round}-random/r{random_round}-{project}/r{random_round}-{project}-output/"
        get_result(project, Path(new), Path(jqf), Path(random), figures_dir/f"{project}.pdf")

