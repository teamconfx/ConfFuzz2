# This script is used for getting the key metrics of a project from its parsed results
import os, sys
import config
# The metrics will be output from this script
# 1. Total number of test fuzzed (in summary.csv)
# 2. Total number of failures (in failures-output.csv)
# 3. Total number of unique failures (in unique_failures.csv)
# 4. The type of exceptions and their occurrence times (in exception_summary.csv)

changed_delimiter="`"

# return the path of the given file_name in the given dir_name
def get_file_from_dir(dir_name, file_name):
    for root, dirs, files in os.walk(dir_name):
        if file_name in files:
            return os.path.join(root, file_name)
    return None

def get_csv_file_lines(dir_name, file_name):
    csv_file_path = get_file_from_dir(dir_name, file_name)
    if csv_file_path is None:
        print(f"Error: {file_name} not found in {dir_name}")
        return None
    with open(csv_file_path, "r") as summary_csv:
        lines = summary_csv.readlines()
        if file_name == 'unique_failures.csv':
            return len(lines), len(list(filter(lambda x: x[0] == "" and x[6] == "REPRODUCIBLE", map(lambda x: x.split(changed_delimiter), lines))))
        return len(lines)

def parse(dir_name):
    # 1. Total number of test fuzzed
    total_num = get_csv_file_lines(dir_name, "summary.csv")
    if total_num is None:
        return None

    # 2. Total number of failures
    failure_num = get_csv_file_lines(dir_name, "failures-output.csv")
    if failure_num is None:
        return None

    # 3. Total number of unique failures
    unique_failure_num, num_after_filter = get_csv_file_lines(dir_name, "unique_failures.csv")
    if unique_failure_num is None:
        return None

    exception_dict = {}
    # # 4. The type of exceptions and their occurrence times
    # exception_summary_path = get_file_from_dir(dir_name, "exception_summary.csv")
    # if exception_summary_path is None:
    #     print("Error: exception_summary.csv not found in " + dir_name)
    #     return None
    # with open(exception_summary_path, "r") as exception_summary:
    #     lines = exception_summary.readlines()
    #     exception_dict = {}
    #     for line in lines:
    #         try:
    #             exception_type = line.split(changed_delimiter)[0]
    #             exception_num = int(line.split(changed_delimiter)[1])
    #             exception_dict[exception_type] = exception_num
    #         except Exception as e:
    #             raise AssertionError("{}{}".format(line, e))
    return total_num, failure_num, num_after_filter, unique_failure_num, exception_dict

def print_result(dir_name):
    total_num, failure_num, num_after_filter, unique_failure_num, exception_dict = parse(dir_name)
    if total_num is None or failure_num is None or unique_failure_num is None or exception_dict is None:
        return
    print("Total number of test fuzzed: " + str(total_num))
    print("Total number of failures: " + str(failure_num))
    print("Total number of unique failures: " + str(unique_failure_num))
    print("Total number of failures we need to inspect after filter: " + str(num_after_filter))
    # print("The type of exceptions and their occurrence times:")  # 
    # for exception_type in exception_dict:
    #     print("    " + exception_type + ": " + str(exception_dict[exception_type]))

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python get_proj_key_metrics.py <dir_name>")
        sys.exit(1)
    dir_name = sys.argv[1]
    print_result(dir_name)
    
