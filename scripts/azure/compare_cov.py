import os,sys

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
            data = splited_str[1:-1] # the first is test name and last is fuzzing mode
            cov_dict[test_name] = data
    return cov_dict


def find_common_fuzzed_test(dict1, dict2, dict3):
    common_test = []
    # find the common key in three dict
    for key in dict1.keys():
        if key in dict2.keys() and key in dict3.keys():
            common_test.append(key)
    return common_test


def get_highest_cov_proj(proj, confuzz_dict, jqf_dict, random_dict, common_test):
    same_param_cov = []
    same_code_cov = []
    confuzz_highest_param_cov = []
    confuzz_highest_code_cov = []
    jqf_highest_param_cov = []
    jqf_highest_code_cov = []
    random_highest_param_cov = []
    random_highest_code_cov = []

    confuzz_higher_than_jqf_code_cov = []
    confuzz_higher_than_jqf_param_cov = []
    confuzz_higher_than_random_code_cov = []
    confuzz_higher_than_random_param_cov = []
    jqf_higher_than_confuzz_code_cov = []
    jqf_higher_than_confuzz_param_cov = []
    jqf_higher_than_random_code_cov = []
    jqf_higher_than_random_param_cov = []
    random_higher_than_confuzz_code_cov = []
    random_higher_than_confuzz_param_cov = []
    random_higher_than_jqf_code_cov = []
    random_higher_than_jqf_param_cov = []
    
    for test in common_test:
        confuzz_param_cov = int(confuzz_dict[test][-2])
        confuzz_code_cov = int(confuzz_dict[test][-3])
        jqf_param_cov = int(jqf_dict[test][-2])
        jqf_code_cov = int(jqf_dict[test][-3])
        random_param_cov = int(random_dict[test][-2])
        random_code_cov = int(random_dict[test][-3])
        if confuzz_param_cov == jqf_param_cov and confuzz_param_cov == random_param_cov:
            same_param_cov.append(test)
        if confuzz_code_cov == jqf_code_cov and confuzz_code_cov == random_code_cov:
            same_code_cov.append(test)

        if confuzz_param_cov >= jqf_param_cov and confuzz_param_cov >= random_param_cov:
            confuzz_highest_param_cov.append(test)
        if confuzz_code_cov >= jqf_code_cov and confuzz_code_cov >= random_code_cov:
            confuzz_highest_code_cov.append(test)
        if jqf_param_cov >= confuzz_param_cov and jqf_param_cov >= random_param_cov:
            jqf_highest_param_cov.append(test)
        if jqf_code_cov >= confuzz_code_cov and jqf_code_cov >= random_code_cov:
            jqf_highest_code_cov.append(test)
        if random_param_cov >= confuzz_param_cov and random_param_cov >= jqf_param_cov:
            random_highest_param_cov.append(test)
        if random_code_cov >= confuzz_code_cov and random_code_cov >= jqf_code_cov:
            random_highest_code_cov.append(test)

        if confuzz_code_cov > jqf_code_cov:
            confuzz_higher_than_jqf_code_cov.append(test)
        if confuzz_param_cov > jqf_param_cov:
            confuzz_higher_than_jqf_param_cov.append(test)
        if confuzz_code_cov > random_code_cov:
            confuzz_higher_than_random_code_cov.append(test)
        if confuzz_param_cov > random_param_cov:
            confuzz_higher_than_random_param_cov.append(test)
        if jqf_code_cov > confuzz_code_cov:
            jqf_higher_than_confuzz_code_cov.append(test)
        if jqf_param_cov > confuzz_param_cov:
            jqf_higher_than_confuzz_param_cov.append(test)
        if jqf_code_cov > random_code_cov:
            jqf_higher_than_random_code_cov.append(test)
        if jqf_param_cov > random_param_cov:
            jqf_higher_than_random_param_cov.append(test)
        if random_code_cov > confuzz_code_cov:
            random_higher_than_confuzz_code_cov.append(test)
        if random_param_cov > confuzz_param_cov:
            random_higher_than_confuzz_param_cov.append(test)
        if random_code_cov > jqf_code_cov:
            random_higher_than_jqf_code_cov.append(test)
        if random_param_cov > jqf_param_cov:
            random_higher_than_jqf_param_cov.append(test)

    return same_param_cov, same_code_cov, confuzz_highest_param_cov, confuzz_highest_code_cov, jqf_highest_param_cov, jqf_highest_code_cov, random_highest_param_cov, random_highest_code_cov, confuzz_higher_than_jqf_code_cov, confuzz_higher_than_jqf_param_cov, confuzz_higher_than_random_code_cov, confuzz_higher_than_random_param_cov, jqf_higher_than_confuzz_code_cov, jqf_higher_than_confuzz_param_cov, jqf_higher_than_random_code_cov, jqf_higher_than_random_param_cov, random_higher_than_confuzz_code_cov, random_higher_than_confuzz_param_cov, random_higher_than_jqf_code_cov, random_higher_than_jqf_param_cov
    
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


def compare_cov(confuzz_dir, jqf_dir, random_dir):
    confuzz_dict = read_mode_summary(confuzz_dir)
    jqf_dict = read_mode_summary(jqf_dir)
    random_dict = read_mode_summary(random_dir)

    for proj in confuzz_dict.keys():
        proj_confuzz_dict = confuzz_dict[proj]
        proj_jqf_dict = jqf_dict[proj]
        proj_random_dict = random_dict[proj]
        common_test = find_common_fuzzed_test(proj_confuzz_dict, proj_jqf_dict, proj_random_dict)
        same_param_cov, same_code_cov, confuzz_highest_param_cov, confuzz_highest_code_cov, jqf_highest_param_cov, jqf_highest_code_cov, random_highest_param_cov, random_highest_code_cov, confuzz_higher_than_jqf_code_cov, confuzz_higher_than_jqf_param_cov, confuzz_higher_than_random_code_cov, confuzz_higher_than_random_param_cov, jqf_higher_than_confuzz_code_cov, jqf_higher_than_confuzz_param_cov, jqf_higher_than_random_code_cov, jqf_higher_than_random_param_cov, random_higher_than_confuzz_code_cov, random_higher_than_confuzz_param_cov, random_higher_than_jqf_code_cov, random_higher_than_jqf_param_cov = get_highest_cov_proj(proj, proj_confuzz_dict, proj_jqf_dict, proj_random_dict, common_test)
        print("=============== Project: ", proj, " ===============")
        print("Number of common fuzzed tests: ", len(common_test))
        print()
        print("Param Coverage: ")
        print("All three have same param cov: ", len(same_param_cov))
        print("Confuzz has highest param cov: ", len(confuzz_highest_param_cov))
        print("JQF has highest param cov: ", len(jqf_highest_param_cov))
        print("Random has highest param cov: ", len(random_highest_param_cov))
        print("Confuzz has higher param cov than JQF: ", len(confuzz_higher_than_jqf_param_cov))
        print("Confuzz has higher param cov than Random: ", len(confuzz_higher_than_random_param_cov))
        print("JQF has higher param cov than Confuzz: ", len(jqf_higher_than_confuzz_param_cov))
        print("JQF has higher param cov than Random: ", len(jqf_higher_than_random_param_cov))  
        print("Random has higher param cov than Confuzz: ", len(random_higher_than_confuzz_param_cov))
        print("Random has higher param cov than JQF: ", len(random_higher_than_jqf_param_cov))

        print()
        print("Code Coverage: ")
        print("All three have same code cov: ", len(same_code_cov))
        print("Confuzz has highest code cov: ", len(confuzz_highest_code_cov))
        print("JQF has highest code cov: ", len(jqf_highest_code_cov))
        print("Random has highest code cov: ", len(random_highest_code_cov))
        print("Confuzz has higher code cov than JQF: ", len(confuzz_higher_than_jqf_code_cov))
        print("Confuzz has higher code cov than Random: ", len(confuzz_higher_than_random_code_cov))
        print("JQF has higher code cov than Confuzz: ", len(jqf_higher_than_confuzz_code_cov))
        print("JQF has higher code cov than Random: ", len(jqf_higher_than_random_code_cov))
        print("Random has higher code cov than Confuzz: ", len(random_higher_than_confuzz_code_cov))
        print("Random has higher code cov than JQF: ", len(random_higher_than_jqf_code_cov))
        print("=" * 60)
        print()

    
if __name__ == "__main__":
    if len(sys.argv) != 4:
        print("Usage: python3 compare_cov.py confuzz_dir jqf_dir random_dir")
        exit(1)
    confuzz_dir, jqf_dir, random_dir = sys.argv[1:]
    compare_cov(confuzz_dir, jqf_dir, random_dir)
    
