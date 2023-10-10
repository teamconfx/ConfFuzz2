import os, sys
import config
from pathlib import Path
# find and parse all summary.txt files in the given directory recursively

title_col = ["test_name", "minConfigSize", "maxConfigSize", "num_of_executed_rounds", "num_of_valid_rounds", "unique_failures", "init_branch_coverage", "total_branch_coverage", "total_param_coverage", "fuzzing_time", "fuzzing_mode"]
def write_summary_files(search_dir: Path):
    failed_outfile = open(search_dir / "fuzz_failed.txt", "w")
    outfile = open(search_dir / "summary.csv", "w")
    title_str = config.DELIMITER.join(title_col) + "\n"
    outfile.write(title_str)
    for summary_file in search_dir.glob("**/summary.txt"):
        test_name = summary_file.parents[2].name + "#" + summary_file.parents[1].name
        with open(summary_file) as infile:
            lines = infile.readlines()
            if len(lines) == 2:
                context = lines[1]
                context_str = config.DELIMITER.join(context.split(","))
                outfile.write(f"{test_name}{config.DELIMITER}{context_str}\n")
            else:
                failed_outfile.write(f"Error: {test_name} has {len(lines)} lines, not 2\n")

    failed_outfile.close()
    outfile.close()


def main(search_dir: Path):
    write_summary_files(search_dir)

