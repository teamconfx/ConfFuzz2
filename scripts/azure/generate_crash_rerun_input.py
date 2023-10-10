import os, shutil
import config

def generate_each_file(file_path, num_tests_per_vm):
    with open(file_path, 'r') as f:
        round, proj = file_path.split('/')[-1].split('_')
        output_dir = os.path.join('crash_rerun_input', round, config.PROJECTS_MAPPING[proj])
        lines = f.readlines()
        for line in lines:
            
            
