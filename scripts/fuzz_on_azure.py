import os, sys
from config import *
import fuzz_single_test as single

JAVA_HOME = "/usr/lib/jvm/java-11-openjdk-amd64/"
MVN = f"JAVA_HOME=\"{JAVA_HOME}\" mvn -B"
CUR_DIR = os.getcwd()
# 1. run setup.sh to install dependencies
# 2. install corresponding project version
# 3. fuzz the project
# 4. copy the output to the output directory

def install_dependency():
    os.system("/bin/bash setup.sh")


def install_project(project_name, project_module_path):
    # delete the project directory if it exists
    project_path = project_root_path[project_name]
    if os.path.exists(project_path):
        print("Project directory exists, delete it first")
        os.system(f"rm -rf {project_path}")
    
    # Need to install HCommon first and then HDFS
    if project_name == "hdfs":
        hcommon_path = project_module["hcommon"]
        os.system(f"git clone {project_github_url[project_name]} && cd {project_module_path} \
            && git checkout -f {project_github_branch[project_name]} \
            && cd {hcommon_path} && {MVN} clean install -DskipTests -Denforcer.skip \
            && cd {project_module_path}/../ && {MVN} clean install -DskipTests -Denforcer.skip")
    else:
        os.system(f"git clone {project_github_url[project_name]} && cd {project_module_path} \
            && git checkout -f {project_github_branch[project_name]} && {MVN} clean install -DskipTests -Denforcer.skip")


def fuzz_project(project_module_path, test_list, output_dir, duration, total_timeout, constraint_file, regex_file):
    for test in test_list:
        single.fuzz("true", test, project_module_path, output_dir, constraint_file, regex_file, duration, total_timeout, JAVA_HOME)
        single.check("analyze", test, project_module_path, output_dir, constraint_file, regex_file, JAVA_HOME)
        single.check("debug", test, project_module_path, output_dir, constraint_file, regex_file, JAVA_HOME)


def main(project_name, test_list, output_dir, duration):
    project_module_path = os.path.join(CUR_DIR, project_module[project_name])
    constraint_file = os.path.join(project_module_path, "constraint")
    regex_file = os.path.join(project_module_path, "regex")
    total_timeout = int(duration) + 100
    duration = f"PT{duration}S"
    install_dependency()
    install_project(project_name, project_module_path)
    fuzz_project(project_module_path, test_list, output_dir, duration, total_timeout, constraint_file, regex_file)


if __name__ == '__main__':
    '''
    <project_name> : project name; e.g. hcommon
    <test_list> : list of tests to fuzz, separated by ";"
    <output_dir> : directory that stores fuzzing outputs
    <fuzzing_duration> : timeout of fuzzing campaign
    '''
    if len(sys.argv) != 5:
        raise ValueError("Usage: python3 fuzz_on_azure.py <project_name> <test_list> <output_dir> <fuzzing_duration>")
    project_name, test_list, output_dir, duration = sys.argv[1:]
    test_list = list(test_list.split(";"))
    main(project_name, test_list, output_dir, duration)