import os

title = ['Total number of test fuzzed:',
         'Total number of failures:',
         'Total number of unique failures:',
         'Total number of failures we need to inspect after filter:'
         ]

file_map = {
    'confuzz': '/home/shuai/xlab/configuration-fuzzing/azure-confuzz-data/r7132-new-summary.txt',
    'jqf': '/home/shuai/xlab/configuration-fuzzing/azure-confuzz-data/r7142-JQF-summary.txt',
    'random': '/home/shuai/xlab/configuration-fuzzing/azure-confuzz-data/r7143-random-summary.txt',
}

def read_one_file(file_name, mode_name):
    proj_data = {}
    with open(file_name, 'r') as f:
        lines = f.readlines()
        cur_proj = ""
        for line in lines:
            # check whehter the line is only have space
            if line.isspace():
                continue
            if "Proj" in line:
                cur_proj = line.split(" ")[2]
                #print(cur_proj) 
                if cur_proj not in proj_data:
                    proj_data[cur_proj] = []
            elif "=" not in line:
                value = mode_name + ":" + line.split(" ")[-1].strip()
                proj_data[cur_proj].append(value)
    #print(proj_data)
    return proj_data

def compare_all(file_map):
    projects_data = {}
    for mode, file_path in file_map.items():
        cur_proj_data = read_one_file(file_path, mode)
        projects_data[mode] = cur_proj_data
    
    first_mode = list(file_map.keys())[0]
    for fuzzing_proj_name in projects_data[first_mode].keys():
        print("\n=================== Project: " + fuzzing_proj_name + " ===================")
        for i in range(len(title)):
            print_str = title[i] + " "
            for mode in projects_data.keys():
                print_str += " " + projects_data[mode][fuzzing_proj_name][i]
            print(print_str)
        print("=" * 60)

compare_all(file_map)







#read_one_file("/home/shuai/xlab/configuration-fuzzing/azure-confuzz-data/r7132-new-summary.txt", "confuzz")