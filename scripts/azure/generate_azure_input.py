import os, sys
import config

# Input format: gitURL,sha,dockerTag,app,projmodule,regexFile,configGenerator,injectConfigFile,duration,test1+test2+test3
#gitURL=$(echo ${line} | cut -d',' -f1)
#sha=$(echo ${line} | cut -d',' -f2)
#dockerTag=$(echo ${line} | cut -d',' -f3)
#app=$(echo ${line} | cut -d',' -f4)
#projmodule=$(echo ${line} | cut -d',' -f5)
#regexFile=$(echo ${line} | cut -d',' -f6)
#configGenerator=$(echo ${line} | cut -d',' -f7)
#injectConfigFile=$(echo ${line} | cut -d',' -f8)
#duration=$(echo ${line} | cut -d',' -f9)
#testlist=$(echo ${line} | cut -d',' -f10)
#fuzzMode=$(echo ${line} | cut -d',' -f11)
confuzz_git_url = "https://github.com/xlab-uiuc/confuzz.git"

def generate_input_str(git_url, commit, docker_tag, app, project_module_path, regex_file_path, configGenerator, injection_config_file, duration, test_list, fuzz_mode):
    test_str = "+".join(test_list)
    return f"{git_url},{commit},{docker_tag},{app},{project_module_path},{regex_file_path},{configGenerator},{injection_config_file},{duration},{test_str},{fuzz_mode}"


def get_test_list_from_file(file_path):
    with open(file_path, "r") as f:
        test_list = f.readlines()
    test_list = [test.strip() for test in test_list]
    return test_list


# split the test list into chunks
def split_test_list(test_list, chunk_size):
    test_list_chunks = []
    for i in range(0, len(test_list), chunk_size):
        test_list_chunks.append(test_list[i:i + chunk_size])
    return test_list_chunks


# generate input for each chunk
def generate_input_for_each_chunk(git_url, commit, docker_tag, app, project_module_path, regex_file_path, configGenerator, injection_config_file, test_list, fuzz_mode, duration, chunk_size):
    test_list_chunks = split_test_list(test_list, chunk_size)
    input_str_list = []
    for test_list_chunk in test_list_chunks:
        input_str = generate_input_str(git_url, commit, docker_tag, app, project_module_path, regex_file_path, configGenerator, injection_config_file, duration, test_list_chunk, fuzz_mode)
        input_str_list.append(input_str)
    return input_str_list


# Create input files for each chunk
def create_input_files(app, project_module_path, input_str_list, output_dir):
    # create output directory if not exist, delete all files in the directory if exist
    print(project_module_path)
    projname = project_module_path.split("/")[-1].replace("-", "")
    dir_name = app.replace("fuzz-", "") + projname
    output_dir = os.path.join(output_dir, dir_name)
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
    else:
        for file in os.listdir(output_dir):
            os.remove(os.path.join(output_dir, file))
    for i, input_str in enumerate(input_str_list):
        with open(os.path.join(output_dir, f"{projname}_input_{i}.csv"), "w") as f:
            f.write(input_str)


def generate_single_file(commit, docker_tag, app, project_module_path, regex_file_path, configGenerator, injection_config_file, test_list_file, fuzz_mode, duration, test_num_per_vm, output_dir):
    fuzz_mode = fuzz_mode.lower()
    if fuzz_mode not in config.FUZZING_MODE:
        print(f"Invalid fuzzing mode: {fuzz_mode}.")
        sys.exit(1)
    test_list = get_test_list_from_file(test_list_file)
    create_input_files(app, project_module_path, generate_input_for_each_chunk(confuzz_git_url, commit, docker_tag, app, project_module_path, regex_file_path, configGenerator, injection_config_file, test_list, fuzz_mode, duration, int(test_num_per_vm)), output_dir)
