import pandas as pd
import glob
import numpy as np
import matplotlib.pyplot as plt
import matplotlib


def read_failure_df():
    files = glob.glob("data/unique_failures*tsv")
    df = pd.DataFrame()
    for file in files:
        current = pd.read_table(file, delimiter="\t")
        df = pd.concat([df, current], axis=0)
    print(df)
    return df


def read_bug_df():
    df = pd.read_table("data/bug_tracker.tsv", delimiter="\t")
    print(df)
    return df


# def compute_cdf(x):
#     x = np.sort(x)
#     y = np.arange(len(x)) / float(len(x))
#     return x, y

# def plot_cdf(df):
#     fig, axes = plt.subplots(figsize=(6, 3))
#     # duration from jenkins json
#     durations = df[df["project"] == project]["ts_duration"] / (60 * 60)
#     x, y = compute_cdf(durations.values)
#     plt.plot(x, y, label="ts_duration", alpha=0.5, linewidth=4)


def stats():
    fdf = read_failure_df()
    bdf = read_bug_df()
    fdf = fdf.dropna(subset=["status"])
    print("number of failures inspected:", len(fdf.index))
    # get failures with bug label
    fdf = fdf.dropna(subset=["Bug_ID"])
    # stats
    print("number of unique failures as bugs:", len(fdf.index))
    print("number of unique bugs:", len(bdf.index))
    print("number of unique bug labels in unique_failures.tsv:", len(list(set(fdf["Bug_ID"].values.tolist()))))
    # plot distribution
    values = fdf["Bug_ID"].value_counts()
    values.to_csv("num_failure_per_bug.csv")
    bugs = values.index.values
    # get bugs not labeled
    unlabel_bugs =  list(set(bdf["Bug ID"].values.tolist()) - set(bugs))
    print("unlabel bugs: ", ", ".join(sorted(unlabel_bugs)))

    fig, ax = plt.subplots()
    # the histogram of the data
    n, bins, patches = ax.hist(values.values.tolist(), 20, density=True)
    ax.set_xticks(range(20))
    ax.set_xlabel('Number of failures per bug')
    ax.set_ylabel('Precentage of bugs')
    # ax.set_title('number of duplicate failures per bug')
    plt.show()


if __name__ == "__main__":
    stats()
