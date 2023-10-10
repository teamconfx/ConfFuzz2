import os, sys, csv
from bisect import bisect_right
import config
from pathlib import Path
from itertools import groupby
import matplotlib.pyplot as plt
import numpy as np
from scipy import stats
import json
import pandas as pd
import rpy2.robjects as robjects
from rpy2.robjects import pandas2ri
from rpy2.robjects.conversion import localconverter

# find and parse all summary.txt files in the given directory recursively
common_tests = set()

def gen_tukey(tag_data):
    r = robjects.r
    r['source']('tukey.R')
    get_hsd_r = robjects.globalenv["get_hsd"]

    input_data = {"label": [], "value": []}
    for tag, data in tag_data.items():
        for d in data:
            input_data["label"].append(tag)
            input_data["value"].append(d)
    summary_table = pd.DataFrame.from_dict(input_data)

    with localconverter(robjects.default_converter + pandas2ri.converter):
        df_result = get_hsd_r(summary_table)
    print(df_result)
    #df_result.to_csv(output_file, index=False)


def plot_smooth_figure(data, label, style, col):
    # Separate x and y values from the data
    x_values, y_values = zip(*data)

    # Generate a smooth curve using a higher resolution of x values
    x_smooth = np.linspace(min(x_values), max(x_values), 18000)
    y_smooth = np.interp(x_smooth, x_values, y_values)


    # Plot the smooth curve
    plt.plot(x_smooth, y_smooth, label=label, linestyle=style, color=col, linewidth=4)

def plot_fill_between(data, col):
    x_values = list(map(lambda x: x[0], data))
    lower_y, upper_y = list(zip(*map(lambda x: x[1], data)))
    # Generate a smooth curve using a higher resolution of x values
    x_smooth = np.linspace(min(x_values), max(x_values), 18000)
    lower_y_smooth = np.interp(x_smooth, x_values, lower_y)
    upper_y_smooth = np.interp(x_smooth, x_values, upper_y)
    plt.fill_between(x_smooth, lower_y_smooth, upper_y_smooth, alpha=0.2, color=col)

def get_time_cov_pairs(plot_data: Path):
    test_name = plot_data.parents[1].name + "#" + plot_data.parents[0].name
    with open(plot_data) as infile:
        data = [tuple(map(lambda x: x.strip(), line.split(','))) for line in infile.readlines()[1:]]
    # time, cov
    return test_name, [(int(line[0]), int(line[1])) for line in data]

def is_valid(plot_data: Path):
    test_name = plot_data.parents[1].name + "#" + plot_data.parents[0].name
    return test_name, get_time_cov_pairs(plot_data)[1] != []

def put_figure(search_dir, label):
    global common_tests, colors
    all_results = {}
    all_times = set()
    cnt = 0
    for summary_file in search_dir.glob("**/coverage_trend.csv"):
        test_name, result = get_time_cov_pairs(summary_file)
        assert test_name not in common_tests or result != [], '\n'.join((test_name, label, str(search_dir), str(summary_file)))
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
                t = (time - init_time + (idx/size))/1000
                transformed_result[t] = pair[1]
                maximum = max(maximum, pair[1])
                times.append(t)
                all_times.add(t)
        transformed_result[1800] = maximum
        times.append(1800)
        all_results[test_name] = (times, transformed_result)
    all_times.add(1800)
    # prepare the dots
    all_times = sorted(list(all_times))
    prev = {test: -1 for test in all_results.keys()}
    time_covs = {}
    for idx, time in enumerate(all_times):
        #print(f"{idx} / {len(all_times)}")
        covs = []
        avg = 0
        for test, result in all_results.items():
            index = bisect_right(result[0], time) - 1
            assert index >= prev[test], (index, prev[test])
            assert result[0][index] <= time, (result[0], time, result[0][index], result[0][index+1])
            if index+1 < len(result[0]):
                assert result[0][index+1] > time, (result[0], time, result[0][index], result[0][index+1])
            #print(index, len(result[0]), len(result))
            covs.append(result[1][result[0][index]])
            #avg += result[1][result[0][index]]
            prev[test] = index
        assert len(covs) == cnt
        time_covs[time] = covs
        #avg /= len(all_results)
        #dots.append((time, avg))
    col = colors.pop(0)

    """
    confidence_level = 0.95
    bounds = {}
    for time, covs in time_covs.items():
        # Calculate the sample mean and standard error
        sample_mean = np.mean(covs)
        standard_error = stats.sem(covs)
        # Calculate the degrees of freedom (n-1 for sample data)
        degrees_of_freedom = len(covs) - 1
        # Compute the confidence interval using t.interval
        bounds[time] = stats.t.interval(confidence_level, df=degrees_of_freedom, loc=sample_mean, scale=standard_error)
    """

    plot_smooth_figure([(t, sum(c)/len(c)) for t,c in time_covs.items()], label, "solid", col)
    #plot_fill_between(list(bounds.items()), col)
    #plot_smooth_figure([(t, min(c)) for t,c in time_covs.items()], label, "dotted", col)
    #plot_smooth_figure([(t, max(c)) for t,c in time_covs.items()], label, "dashed", col)
    return time_covs


def get_result(project, tag_dir, output = "figure.pdf"):
    global common_tests, colors
    tag_tests = [[name for name, r in map(is_valid, d.glob("**/coverage_trend.csv"))
                  if r] for t,d in tag_dir]
    all_tests = set(sum(tag_tests, []))
    common_tests = [t for t in all_tests if all([t in l for l in tag_tests])]
    print(project)
    for idx, tests in enumerate(tag_tests):
        print(f"{tag_dir[idx][0]}: {len(tests)}")
    print(len(common_tests), len(common_tests)/ len(all_tests))

    # Create the plot and set the figure size
    plt.figure(figsize=(8, 6))
    dots = {}
    colors = ["orange", "green", "blue"]
    for t,d in tag_dir:
        dots[t] = put_figure(d, t)
        #$dots = {t: put_figure(d, t) for t,d in tag_dir}
    #tukey_input = {t[0]:dots[t[0]][1800] for t in tag_dir}
    #gen_tukey(tukey_input)

    #with open(output.parents[0]/f"{project}.json", "w") as f:
    #    json.dump(dots, f)

    # Set plot title and labels
    tick_size = 18
    label_size = 22
    title_size = 25
    plt.title(project + f": {len(common_tests)} in total", fontsize=title_size)
    plt.xlabel('Time (s)', fontsize=label_size)
    plt.ylabel('Average Branch Coverage per Test', fontsize=label_size)
    plt.tick_params(axis='both', which='major', labelsize=tick_size)

    # Display a legend
    #plt.legend(fontsize)

    # Show the plot
    plt.savefig(output, format='pdf', bbox_inches='tight')


if __name__ == "__main__":
    projects = ["alluxiocommon","hadoophadoopcommon","hadoophadoopmapreduceclientcore","hbasehbaseserver","kylincorecube","flinkflinkcore","hadoophadoophdfs","hadoophadoopyarncommon","hiveql","zeppelinzeppelinzengine"]
    #projects = ["hiveql"]
    data_dir = Path(sys.argv[1])
    tag_round_str = sys.argv[2:]
    tag_round = list(map(lambda x: x.split('='), tag_round_str))

    figures_dir = data_dir/f"r{'_'.join([x[1] for x in tag_round])}_figures"
    figures_dir.mkdir(parents=True, exist_ok=True)

    for project in projects:
        tag_dir = [(t, data_dir/f"r{rnd}/r{rnd}-{project}/r{rnd}-{project}-output/") for t,rnd in tag_round]
        get_result(project, tag_dir, figures_dir/f"{project}.pdf")

