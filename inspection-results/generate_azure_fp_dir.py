import os, shutil
import json


def load_json(json_file):
    res = {}
    with open(json_file, 'r') as f:
        json_data = json.load(f)
        for item in json_data:
            test_name = item['testClass'] + '#' + item['testMethod']
            if test_name not in res:
                res[test_name] = []
            res[test_name].append(item['minConfig'])
    return res


def distribute(json_file, output_dir):
    # rm the output_dir if exist
    if os.path.exists(output_dir):
        shutil.rmtree(output_dir)
    os.mkdir(output_dir)
        
    test_list = load_json(json_file)
    # split the test_list into num_of_each_vm parts and write to txt files
    count = 0
    for test_name, min_config_list in test_list.items():
        for min_config in min_config_list:
            file_path = os.path.join(output_dir, f'{count}.json')
            count += 1
            with open(file_path, 'w') as f:
                json.dump(min_config, f)
            # append the test_name to the end of the file
            with open(file_path, 'a') as f:
                f.write(f'\n{test_name}')
                
            
def main(json_file_dir, output_dir):
    # rm the output_dir if exist
    if os.path.exists(output_dir):
        shutil.rmtree(output_dir)
    os.mkdir(output_dir)
    # for each json file in json_file_dir, call distribute
    for file in os.listdir(json_file_dir):
        sub_dir = os.path.join(output_dir, file.replace('.json', ''))
        # rm and mkdir
        if os.path.exists(sub_dir):
            shutil.rmtree(sub_dir)
        os.mkdir(sub_dir)

        if file.endswith('.json'):
            distribute(os.path.join(json_file_dir, file), sub_dir)

main('compressed-result/all_in_one', 'compressed-result/azure_fp')




        

        
        
        


    
    
