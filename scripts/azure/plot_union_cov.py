# the script for plotting the unioned jacoco coverage
# will try to plot everything and average everything
from copy import deepcopy
from pathlib import Path
from typing import List, Tuple, Dict
from zipfile import ZipFile
from config import COV_DATA_DIR, FUZZING_MODE, PROJECTS_MAPPING, MODE_COLOR, ALL_ROUNDS, ROUNDS, ABLATION_MODE
import sys
import matplotlib.pyplot as plt
import numpy as np
from scipy import stats 
from util import get_cov_dir, get_mode

def plot_smooth_figure(data, tag, col):
    # Separate x and y values from the data
    x_values, y_values = zip(*data)
    # put 0 at the beginning
    # x_values = list(x_values) + [1801]
    # y_values = [0] + list(y_values)
    # Plot the smooth curve
    plt.plot(x_values, y_values, label=tag, linewidth=4, color=col)

def getConfidenceBound(time_covs) -> List[Tuple[int, ]]:
    confidence_level = 0.95
    bounds = []
    assert len(set(len(x[1]) for x in time_covs)) == 1 and all(all(type(c) is int for c in covs) for time,covs in time_covs)
    for time, covs in time_covs:
        # Calculate the sample mean and standard error
        sample_mean = np.mean(covs)
        standard_error = stats.sem(covs)
        # Calculate the degrees of freedom (n-1 for sample data)
        degrees_of_freedom = len(covs) - 1
        # Compute the confidence interval using t.interval
        assert sample_mean != np.nan and standard_error != np.nan
        bounds.append((time, stats.t.interval(confidence_level, df=degrees_of_freedom, loc=sample_mean, scale=standard_error)))
    return bounds

def plot_fill_between(data, col):
    x_values = list(map(lambda x: x[0], data))
    lower_y, upper_y = list(zip(*map(lambda x: x[1], data)))
    # Generate a smooth curve using a higher resolution of x values
    x_smooth = np.linspace(min(x_values), max(x_values), 900)
    lower_y_smooth = np.interp(x_smooth, x_values, lower_y)
    upper_y_smooth = np.interp(x_smooth, x_values, upper_y)
    plt.fill_between(x_smooth, lower_y_smooth, upper_y_smooth, alpha=0.2, color=col)
    #plt.fill_between(x_values, lower_y, upper_y, alpha=0.2, color=col)


def mergeCurves(curves: List[List[Tuple[int, float]]]) -> List[Tuple[int, float]]:
    curves = [c for c in curves if c != []]
    times = sorted(set(sum([list(map(lambda y: y[0], x)) for x in curves], []) + [1800]))
    #assert times[-1] <= 1800, curves
    lens = [len(c) for c in curves]
    idxs = [0] * len(curves)
    curCov:List[float] = [0] * len(curves)
    ret = []
    for t in times:
        for i, idx in enumerate(idxs):
            if idx == lens[i]:
                continue
            if curves[i][idx][0] == t:
                curCov[i] = curves[i][idx][1]
                idxs[i] += 1
        ret.append((t, deepcopy(curCov)))
    return ret

def main(rounds: List[str], tools: List[str] = FUZZING_MODE, fuzzRounds: List[str] = ALL_ROUNDS):
    #r8091-confuzz-cov
    #r8091-confuzz-cov-output
    #flinkflinkcore_input_7290
    for project, projId in PROJECTS_MAPPING.items():
        cov_curves:Dict[str, List[Tuple[int, float]]] = {}
        for rnd in rounds:
            covDir: Path = get_cov_dir(rnd, projId)
            if not covDir.exists():
                continue
            for zipFile in filter(lambda x: projId in x.name, covDir.glob("**/*.zip")):
                fuzzRnd:str = zipFile.name.split("_")[6]
                mode = get_mode(fuzzRnd)
                assert mode is not None
                if mode not in tools or fuzzRnd not in fuzzRounds:
                    continue
                print(mode, project, fuzzRnd)
                with ZipFile(zipFile) as zf:
                    jacocoCsvs = [s for s in zf.namelist() if s.endswith("csv") and "outputs" in s]
                    if len(jacocoCsvs) == 0:
                        print("Ah? What the heck?", project, mode, fuzzRnd)
                        continue
                    rndCurve = []
                    for covCsv in jacocoCsvs:
                        ts = int(covCsv.split('/')[-1].split('.')[0].split('_')[-1])
                        with zf.open(covCsv) as f:
                            next(f)
                            rndCurve.append((ts, sum([int(line.decode("utf-8").split(",")[6]) for line in f])))
                    cov_curves[f"{rnd}-{fuzzRnd}-{mode}"] = sorted(rndCurve, key=lambda x: x[0])
                    #if 'yarn' in project.lower():
                    #    print(cov_curves[f"{rnd}-{fuzzRnd}"])
                    #print(jacocoCsvs)
        avg_curves = {}
        if len(cov_curves) != 0:
            for mode in tools:
                avg_curves[mode] = mergeCurves([v for k,v in cov_curves.items() if k.endswith(mode)])
                #if 'yarn' in project.lower():
                #    print(avg_curves[mode])
        for mode, curve in avg_curves.items():
            print(mode, project)
            plot_smooth_figure([(t, sum(v) / len(v)) for t,v in curve], mode, MODE_COLOR[mode])
            plot_fill_between(getConfidenceBound(curve), MODE_COLOR[mode])
            if project == "Zeppelin" and mode == "confuzz":
                print(curve[-1][1])
        plt.title(project)
        plt.legend()
#        plt.ylim(1, None)
#        plt.yscale("log")
        plt.savefig(f"union_figures_0/{project}.pdf", format="pdf")
        plt.clf()

if __name__ == "__main__":
    rounds = sys.argv[1].split(',')
    if len(sys.argv) == 3:
        tools = sys.argv[2].split(',')
        main(rounds, tools)
    elif len(sys.argv) == 4:
        tools = sys.argv[2].split(',')
        rnd = sys.argv[3].split(',')
        main(rounds, ABLATION_MODE, rnd)
    else:
        main(rounds)
