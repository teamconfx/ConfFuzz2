import os, sys
import json
import config
import shutil
from zipfile import ZipFile
from pathlib import Path


def get_all_test_coverage_trend(search_dir: Path, mode: str):
    # Search for all the zip files in the search_dir and get "coverage_trend.csv" from each zip file
    # Return a list of coverage_trend.csv
    ret = {}
    zip_files = search_dir.glob("**/*.zip")
    for zip_file in zip_files:
        with ZipFile(zip_file) as zf:
            csv_files = filter(lambda x: x.endswith("coverage_trend.csv"), zf.namelist())
            for fname in csv_files:
                project_name = fname.split("/")[0].split("_")[0]
                test_name = fname.split("/")[-3] + "#" + fname.split("/")[-2]
                key = project_name + "#" + test_name
                with zf.open(fname) as infile:
                    # read the second line of the csv file if there are more than 2 lines
                    lines = infile.readlines()
                    if len(lines) > 1:
                        if mode == "0_min":
                            line = lines[1].decode("utf-8")
                        elif mode == "30_min":
                            line = lines[-1].decode("utf-8")
                        else:
                            print("Error: invalid mode")
                            sys.exit(1)
                        coverage_trend = int(line.split(",")[1].strip())
                        ret[key] = coverage_trend

    return ret


def compare_coverage_trend(confuzz_search_dir: Path, jqf_search_dir: Path, output_dir):
    confuzzRndId = confuzz_search_dir.name.split("/")[-1]
    jqfRndId = jqf_search_dir.name.split("/")[-1]

    # remove the output_dir if it exists and create a new one
    if output_dir.exists():
        shutil.rmtree(output_dir)
    os.mkdir(output_dir)

    
    modes = ["0_min", "30_min"]
    metrics = ["higher", "lower", "equal"]
    for mode in modes:
        confuzz_cov_dict = get_all_test_coverage_trend(confuzz_search_dir, mode)
        jqf_cov_dict = get_all_test_coverage_trend(jqf_search_dir, mode)
        for metric in metrics:
            compare(confuzz_cov_dict, jqf_cov_dict, confuzzRndId, jqfRndId, metric, mode, output_dir)

def compare(confuzz_cov_dict, jqf_cov_dict, confuzzRndId, jqfRndId, high_or_low, mode, output_dir):
    output_file = output_dir / "{}_{}_{}_{}.csv".format(confuzzRndId, mode, high_or_low, jqfRndId)
    count = 0
    output_str = ""
    # output_str = "project_name test_name confuzz_cov jqf_cov diff\n"
    ret = {}
    for key in confuzz_cov_dict.keys():
        if key in jqf_cov_dict.keys():
            if high_or_low == "lower":
                if confuzz_cov_dict[key] < jqf_cov_dict[key]:
                    project_name = key.split("#")[0]
                    output_str += "{} {} {} {} {}\n".format(jqf_cov_dict[key] - confuzz_cov_dict[key], project_name, key, confuzz_cov_dict[key], jqf_cov_dict[key])
                    count += 1
            elif high_or_low == "higher":
                if confuzz_cov_dict[key] > jqf_cov_dict[key]:
                    project_name = key.split("#")[0]
                    output_str += "{} {} {} {} {}\n".format(confuzz_cov_dict[key] - jqf_cov_dict[key], project_name, key, confuzz_cov_dict[key], jqf_cov_dict[key])
                    count += 1
            elif high_or_low == "equal":
                if confuzz_cov_dict[key] == jqf_cov_dict[key]:
                    project_name = key.split("#")[0]
                    output_str += "{} {} {} {} {}\n".format(0, project_name, key, confuzz_cov_dict[key], jqf_cov_dict[key])
                    count += 1
            else:
                print("Error: invalid metric")
                sys.exit(1)
                    
    print("At {}, {}-Confuzz has {} tests {} than/to {}-JQF".format(mode, confuzzRndId, count, high_or_low, jqfRndId))
    with open(output_file, "w") as outfile:
        outfile.write(output_str)
    

if __name__ == "__main__":
    if len(sys.argv) != 4:
        print("Usage: python3 get_confuzz_low_start_point_test.py <confuzz_search_dir> <jqf_search_dir> <output_dir>")
        sys.exit(1)
    confuzz_search_dir = Path(sys.argv[1])
    jqf_search_dir = Path(sys.argv[2])
    output_dir = Path(sys.argv[3])
    compare_coverage_trend(confuzz_search_dir, jqf_search_dir, output_dir)
    
    
