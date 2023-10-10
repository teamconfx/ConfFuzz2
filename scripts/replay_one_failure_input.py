# This script is used to replay a single failure input
# with `mvn confuzz:single-analyze` and `mvn confuzz:single-debug`

# Steps to replay a failure:
# 1. Clone, build the app
# 2. Get one failure input from the argument
# 3. Search where is the failure input in the given output directory, copy it to the output directory
# 4. Run `mvn confuzz:single-analyze` and `mvn confuzz:single-debug` to replay the failure, the output
#    should be copied to the output directory

import os, sys
from fuzz_on_azure import install_project, MVN
from config import *

# Copy failure input files (config_XXX.json, id_XXX, parent_XXX, replay_id_XXX) to output directory
# under the test class and test method directory
def copy_failure_input(test_name, failure_id, replay_data_dir, output_dir):
    test_class, test_method = test_name.split("#")
    failure_output_dir = os.path.join(output_dir, test_class, test_method, "campaign", "failures")
        # Create output directory if not exist
    if not os.path.exists(failure_output_dir):
        os.makedirs(failure_output_dir)
    
    failure_no = failure_id.split("_")[1]
    # Find directory path that ends with test_class/test_method
    target_dir = None
    for root, dirs, files in os.walk(replay_data_dir):
        if root.endswith(f"{test_class}/{test_method}"):
            #print(root)
            target_dir = root
            break

    for root, dirs, files in os.walk(target_dir):
        for file in files:
            if root.endswith("failures") and failure_no in file:
                # copy file to output_dir
                #print(f"{root}/{file}")
                os.system(f"cp {root}/{file} {failure_output_dir}")

# Execute `mvn confuzz:single-analyze` and `mvn confuzz:single-debug` to replay the failure
def replay_and_analyze_failure_input(test_name, failure_id, output_dir, execution_dir):
    test_class, test_method = test_name.split("#")
    replay_output_dir = os.path.join(output_dir, test_class, test_method)
    confuzz_argument = f"-Dmeringue.testClass={test_class} -Dmeringue.testMethod={test_method} -DconstraintFile=constraint -DregexFile=regex -DfailureId={failure_id} -Dmeringue.outputDirectory={output_dir}"
    # Run `mvn confuzz:single-analyze` and `mvn confuzz:single-debug`
    analyze_cmd = f"cd {execution_dir} && {MVN} confuzz:single-analyze {confuzz_argument} >> {replay_output_dir}/{failure_id}.log"
    debug_cmd = f"cd {execution_dir} && {MVN} confuzz:single-debug {confuzz_argument} >> {replay_output_dir}/{failure_id}.log"
    #print(analyze_cmd)
    #print(debug_cmd)
    os.system(analyze_cmd)
    os.system(debug_cmd)


# Main function to replay a single failure input
def analyze_one_input(project_name, test_name, failure_id, replay_data_dir, output_dir):
    CUR_DIR = os.getcwd()
    copy_failure_input(test_name, failure_id, replay_data_dir, output_dir)
    execution_dir = os.path.join(CUR_DIR, project_module[project_name])
    abs_output_dir = os.path.join(CUR_DIR, output_dir)
    replay_and_analyze_failure_input(test_name, failure_id, abs_output_dir, execution_dir)


if __name__ == '__main__':
    '''
    <project_name> : project name; e.g. hcommon
    <install_proj> : boolean value; e.g. True
    <test_name> : testClass#testMethod; e.g. TestHdfsConfigurationFields
    <failure_id> : failure id; e.g. id_000000
    <replay_data_dir> : directory that stores replay data
    <output_dir> : directory that (1) stores copied replay data; (2) argument for -Dmeringue.outputDirectory
    '''
    # dir = sys.argv[1]
    # output_dir = sys.argv[2]
    # copy_failure_input("org.apache.hadoop.service.launcher.TestServiceLauncher#testRunService", "id_000000", dir, output_dir)
    
    if len(sys.argv) != 7:
        ValueError("Usage: python3 replay_one_failure_input.py <project_name> <install_proj> <test_name> <failure_id> <replay_data_dir> <output_dir>")
    
    project_name, install_proj, test_name, failure_id, replay_data_dir, output_dir = sys.argv[1:]
    
    if install_proj.lower() == "true":
        #print("Install project")
        install_project(project_name, project_module[project_name])
    
    analyze_one_input(project_name, test_name, failure_id, replay_data_dir, output_dir)
    
    
    