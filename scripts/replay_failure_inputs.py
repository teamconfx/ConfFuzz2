# This script is used to replay and analyze multiple failure inputs from a list
# list is formatted as:
# test_name	failure_id (separated by tab)
# e.g., org.apache.hadoop.crypto.key.TestKeyShell#testAttributes	id_000000

import sys
from replay_one_failure_input import analyze_one_input
from fuzz_on_azure import install_project
from config import *

def analyze_multiple_inputs(project_name, test_list_file, replay_data_dir, output_dir):
    with open(test_list_file, "r") as f:
        for input in f:
            test_name, failure_id = input.strip().split("\t")
            analyze_one_input(project_name, test_name, failure_id, replay_data_dir, output_dir)


if __name__ == '__main__':
    '''
    <project_name> : project name; e.g. hcommon
    <install_proj> : boolean value; e.g. True
    <test_list_file> : file that stores test_name and failure_id separated by tab
    <replay_data_dir> : directory that stores replay data
    <output_dir> : directory that (1) stores copied replay data; (2) argument for -Dmeringue.outputDirectory
    '''
    if len(sys.argv) != 5:
        ValueError("Usage: python3 replay_failure_inputs.py <project_name> <install_proj> <test_list_file> <replay_data_dir> <output_dir>")
    
    project_name, install_proj, test_list_file, replay_data_dir, output_dir = sys.argv[1:]
    
    if install_proj.lower() == "true":
        #print("Install project")
        install_project(project_name, project_module[project_name])
    
    analyze_multiple_inputs(project_name, test_list_file, replay_data_dir, output_dir)



