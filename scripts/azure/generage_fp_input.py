import os, shutil, sys
import config
import math

OUTPUT_DIR="fp_input"
IMAGE_TAG="AUG21"


def get_input_text(proj):
    proj = config.LOWER_TO_KEY[proj]
    app = config.FUZZ_MAPPING[proj]
    testModule = config.FUZZ_MODULES[proj]
    regexFile = config.REGEX_FILE[proj]
    configGenerator = config.CONFIG_GENERATOR[proj]
    injectionFile = config.INJECTION_FILE[proj]
    failureDirLink = config.FAILURE_FP_LINK[proj]
    text = f"{IMAGE_TAG},{app},{testModule},{regexFile},{configGenerator},{injectionFile},{failureDirLink},{start_failure_index},{end_failure_index}"
    return text


def generate_fp_input(vm_index, start_failure_index, end_failure_index, proj):
    output_dir = os.path.join(OUTPUT_DIR, proj)
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
    print(output_dir)
    with open(os.path.join(output_dir, f"{proj}_input_{vm_index}.csv"), "w") as f:
        proj = config.LOWER_TO_KEY[proj]
        app = config.FUZZ_MAPPING[proj]
        testModule = config.FUZZ_MODULES[proj]
        regexFile = config.REGEX_FILE[proj]
        configGenerator = config.CONFIG_GENERATOR[proj]
        injectionFile = config.INJECTION_FILE[proj]
        failureDirLink = config.FAILURE_FP_LINK[proj]
        text = f"{IMAGE_TAG},{app},{testModule},{regexFile},{configGenerator},{injectionFile},{failureDirLink},{start_failure_index},{end_failure_index}"
        f.write(text)
        
        
def generate_file_for_each_proj(json_file_dir, proj, num_test_per_vm):
    # count how many json file under json_file_dir, then split them into num_test_per_vm groups
    json_files = os.listdir(json_file_dir)
    num_json_files = len(json_files)
    num_vm = math.ceil(num_json_files / num_test_per_vm)
    startIndex = 0
    print(f"{num_vm} VMs will be generated for {proj}")
    for i in range(num_vm):
        endIndex = startIndex + num_test_per_vm
        if endIndex > num_json_files:
            endIndex = num_json_files
        generate_fp_input(i, startIndex, endIndex, proj)
        startIndex = endIndex

        
def generate(root_dir, num_test_per_vm):
    # walk through root_dir, find all json files
    for proj in config.LOWER_TO_KEY:
        print(proj)
        generate_file_for_each_proj(os.path.join(root_dir, proj), proj, num_test_per_vm)


if __name__ == "__main__":
    if len(sys.argv) != 5:
        print("Usage: python3 generate_fp_input.py image_tag root_dir output_dir num_test_per_vm")
        exit(1)
    IMAGE_TAG, root_dir, OUTPUT_DIR, num_test_per_vm = sys.argv[1:]
    num_test_per_vm = int(num_test_per_vm)
    generate(root_dir, num_test_per_vm)
