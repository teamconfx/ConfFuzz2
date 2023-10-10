import os, re, sys
method_re = 'test([a-zA-Z0-9]*?)\('
mapping_dir = './method_mapping/'


def start_parsing():
    for root, dirs, files in os.walk("."):
        for file in files:
            if "-output.txt" in file:
                # try:
                parse_test_class(file)
                # except Exception as e:
                #     print("[ERROR] Exception when parsing " + file + str(e))
                #     exit(-1)


def parse_test_class(file_name):
    os.makedirs(mapping_dir, exist_ok=True)
    method_dict = get_test_method(file_name)
    #print(method_dict)
    with open(file_name, 'r') as f:
        for line in f:
            #print(line)
            if "CTEST" not in line:
                continue
            newline = line.split("CTEST")[1]
            splitStr = newline.split(" ")
            if "test" not in line:
                print(line)
                for name in method_dict.keys():
                    if len(splitStr) < 3:
                        #print(line)
                        continue
                    else:
                        method_dict[name].add(splitStr[1] + "=" + splitStr[3])
            else:
                m = re.search(method_re, line)
                if m:
                    found = m.group(1) 
                    method_name = 'test' + found
                    method_dict[method_name].add(splitStr[1] + "=" + splitStr[3])
    #print(method_dict)
    for method in method_dict.keys():
        dict_to_file(file_name, method, method_dict)
        
            
def get_test_method(file_name):
    method_dict = {}
    with open(file_name, 'r') as f:
        for line in f:
            if "test" in line:
                #print(line)
                m = re.search(method_re, line)
                #print(m)
                if m:
                    #print(line)
                    found = m.group(1) 
                    method_name = 'test' + found
                    if method_name not in method_dict:
                        method_dict[method_name] = set()
    return method_dict


def dict_to_file(file_name, method_name, method_dict):
    file_path = mapping_dir + file_name.split('-output.txt')[0] + "#" + method_name
    print(file_path)
    with open(file_path, 'w') as f:
        for str in method_dict[method_name]:
            f.write(str + "\n")


#parse_test_class(sys.argv[1])
start_parsing()
