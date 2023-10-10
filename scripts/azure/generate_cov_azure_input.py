import os, sys
import config
import shutil

# dockerTag=$(echo ${line} | cut -d',' -f1)
# app=$(echo ${line} | cut -d',' -f2)
# projmodule=$(echo ${line} | cut -d',' -f3)
# coverageLink=$(echo ${line} | cut -d',' -f4
# JUL27,fuzz-hadoop,hadoop-common-project/hadoop-common,https://mir.cs.illinois.edu/~swang516/confuzz/coverage.zip

def generate_input_str(docker_tag, app, project_module_path, coverageLink):
    return f"{docker_tag},{app},{project_module_path},{coverageLink}"


def generate_coverage_link(round, proj):
    return f"https://mir.cs.illinois.edu/~swang516/confuzz/jed_zips/r{round}_{proj}.zip"


def generate_single_file(tool, round, proj, docker_tag, app, project_module_path, output_dir):
    coverageLink = generate_coverage_link(round, proj)
    input_str = generate_input_str(docker_tag, app, project_module_path, coverageLink)
    # write the input_str to the file under output_dir / tool
    output_dir = os.path.join(output_dir, tool)
    if not os.path.exists(output_dir):
        os.mkdir(output_dir)
    output_file = os.path.join(output_dir, f"{proj}_input_{round}.csv")
    with open(output_file, "w") as f:
        f.write(input_str)
    return input_str


def generate(tool, docker_tag, round, output_dir):
    # get the key and value from config.PROJECT_MAPPING
    for proj, proj_full_name in config.PROJECTS_MAPPING.items():
        app=config.FUZZ_MAPPING[proj]
        module = config.FUZZ_MODULES[proj]
        generate_single_file(tool, round, proj_full_name, docker_tag, app, module, output_dir)
            

def main():
    if len(sys.argv) != 6:
        print("Usage: python3 generate_cov_azure_input.py <docker_tag> <confuzz_round> <jqf_round> <random_round> <output_dir>")
        sys.exit(1)
    docker_tag, confuzz_round, jqf_round, random_round, output_dir = sys.argv[1:]
    # remove and create the output_dir
    if os.path.exists(output_dir):
        shutil.rmtree(output_dir)
    os.mkdir(output_dir)
    for round in confuzz_round.split(","):
        generate("confuzz", docker_tag, round, output_dir)
    for round in jqf_round.split(","):
        generate("jqf", docker_tag, round, output_dir)
    for round in random_round.split(","):
        generate("random", docker_tag, round, output_dir)

main()   
