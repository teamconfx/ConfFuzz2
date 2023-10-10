# use generate_azure_input.py to generate all apps
import app_config
import os,sys
import generate_azure_input

def generate(commit, docker_tag, duration, test_per_vm, ctest_list_dir, fuzz_mode, output_dir):
    for fuzz_app, modules in app_config.fuzz_modules.items():
        for module in modules:
            ctest_file_path = find_ctest_list_file(fuzz_app, module, ctest_list_dir)
            generate_azure_input.generate_single_file(commit, docker_tag, fuzz_app,\
                                                        module, app_config.regex_path[fuzz_app], app_config.get_generator_class(fuzz_app), \
                                                        app_config.get_inject_ctest_file_path(fuzz_app, module),\
                                                        ctest_file_path, fuzz_mode, duration, test_per_vm, output_dir)

def find_ctest_list_file(app, module, ctest_data_dir):
    # find all file that under $app/$module/confuzz_identify_ctest.csv
    app = app.replace('fuzz-', '')
    path = os.path.join(ctest_data_dir, app, module, "ctest_list.txt")
    if os.path.exists(path):
        print(path)
        return path
    else:
        print("Cannot find {}".format(path))
        sys.exit(1)

if __name__ == "__main__":
    if len(sys.argv) != 8:
        print("Usage: python3 generate_all_proj_input.py <commit> <docker_tag> <test_per_vm> <duration> <ctest_list_dir> <fuzz_mode> <output_dir>")
        sys.exit(1)
    commit, docker_tag, test_per_vm, duration, ctest_list_dir, fuzz_mode, output_dir = sys.argv[1:]
    generate(commit, docker_tag, duration, test_per_vm, ctest_list_dir, fuzz_mode, output_dir)
