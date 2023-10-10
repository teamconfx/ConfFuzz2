import os
import json


def merge_json_files(file1, file2, output_file):
    with open(file1, 'r') as f1, open(file2, 'r') as f2:
        data1 = json.load(f1)
        data2 = json.load(f2)
    # data1 and data2 are two lists
    data = data1 + data2
    with open(output_file, 'w') as f:
        json.dump(data, f, indent=4)


def merge_all(input_dir):
    # find all the json files in the input_dir
    json_files = []
    for root, dirs, files in os.walk(input_dir):
        for file in files:
            if file.endswith('.json'):
                json_files.append(os.path.join(root, file))
    for file in json_files:
        merge_json_files(file, file.replace('r7181', 'rALL'), file.replace('r7181', 'all_in_one'))

        
merge_all('r7181')
    
    
