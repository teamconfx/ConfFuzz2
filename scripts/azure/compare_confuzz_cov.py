import os, sys

DELIMITER="¬"

'''
Return a dictionary of coverage information for each test in the project
'''
def read_proj_summary(proj, file_path):
    cov_dict = {}
    with open (file_path, 'r') as f:
        lines = f.readlines()[1:]
        for line in lines:
            splited_str = line.split(DELIMITER)
            test_name = splited_str[0]
            data = splited_str[1:-1]   # the first is test_name and last is fuzzing mode
            cov_dict[test_name] = data
    return cov_dict


def find_common_fuzzed_test(dict1, dict2):
    common_test = []
    # find the common key in three dict
    for key in dict1.keys():
        if key in dict2.keys():
            common_test.append(key)
    return common_test


def get_highest_cov_proj(proj, old_confuzz_dict, new_confuzz_dict, common_test):
    same_param_cov = []
    same_code_cov = []
    new_higher_param_cov = []
    new_higher_code_cov = []
    old_higher_param_cov = []
    old_higher_code_cov = []
    
    for test in common_test:
        old_param_cov = int(old_confuzz_dict[test][0])
        old_code_cov = int(old_confuzz_dict[test][1])
        new_param_cov = int(new_confuzz_dict[test][0])
        new_code_cov = int(new_confuzz_dict[test][1])
        if old_param_cov == new_param_cov:
            same_param_cov.append(test)
        elif old_param_cov > new_param_cov:
            old_higher_param_cov.append(test)
        else:
            new_higher_param_cov.append(test)
        if old_code_cov == new_code_cov:
            same_code_cov.append(test)
        elif old_code_cov > new_code_cov:
            old_higher_code_cov.append(test)
        else:
            new_higher_code_cov.append(test)
    return same_param_cov, same_code_cov, new_higher_param_cov, new_higher_code_cov, old_higher_param_cov, old_higher_code_cov
    
def read_mode_summary(mode_output_dir):
    # walk through the mode_output_dir and find the summary.txt file_path
    summary_dict = {}
    for root, dirs, files in os.walk(mode_output_dir):
        for file in files:
            if file == "summary.csv":
                file_path = os.path.join(root, file)
                project_name = file_path.split("/")[-2].split("-")[1]
                sumamry = read_proj_summary(project_name, file_path)
                summary_dict[project_name] = sumamry
    return summary_dict


def compare_cov(new_dir, old_dir):
    new_dict = read_mode_summary(new_dir)
    old_dict = read_mode_summary(old_dir)

    for proj in new_dict.keys():
        proj_new_dict = new_dict[proj]
        proj_old_dict = old_dict[proj]

        common_test = find_common_fuzzed_test(proj_new_dict, proj_old_dict)
        same_param_cov, same_code_cov, new_higher_param_cov, new_higher_code_cov, old_higher_param_cov, old_higher_code_cov = get_highest_cov_proj(proj, proj_old_dict, proj_new_dict, common_test)
        print("=============== Project: ", proj, " ===============")
        print("Number of common fuzzed tests: ", len(common_test))
        print()
        print("Param Coverage: ")
        print("Both same: ", len(same_param_cov))
        print("New Confuzz higher: ", len(new_higher_param_cov))
        print("Old Confuzz higher: ", len(old_higher_param_cov))
        print()
        print("Code Coverage: ")
        print("Both same: ", len(same_code_cov))
        print("New Confuzz higher: ", len(new_higher_code_cov))
        print("Old Confuzz higher: ", len(old_higher_code_cov))
        print("=" * 60)
        print()

    
if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python3 compare_cov.py new_dir old_dir")
        exit(1)
    new_dir, old_dir = sys.argv[1:]
    compare_cov(new_dir, old_dir)
    
