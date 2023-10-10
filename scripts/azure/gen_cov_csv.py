from pathlib import Path
import config
import sys, subprocess

projects = list(config.PROJECTS_MAPPING.keys())
tags = ["Confuzz", "JQF", "Random"]

def get_test_name(p):
    return str(p.parents[1].relative_to(p.parents[3])).replace("/", "#")

def get_common_tests(dir_dict):
    common_tests = {}
    all_tests = {}
    for project, tag_dirs in dir_dict.items():
        valid_tests = {}
        for tag, d in tag_dirs.items():
            #print(d.absolute())
            valid_tests[tag] = set([get_test_name(p) for p in d.glob("**/*.jed")])
            if project not in common_tests:
                common_tests[project] = valid_tests[tag]
            else:
                common_tests[project] = common_tests[project].intersection(valid_tests[tag])
        assert project in common_tests and common_tests[project] != set()
    return common_tests

def main(docker_tag, dir_dict, output_dir):
    global client
    proj_common_tests = get_common_tests(dir_dict)
    """
    container = client.containers.run(f'shuaiwang516/confuzz-image:{docker_tag}', "bash", detach=True)
    for proj, common_tests in proj_common_tests.items():
        #print(proj, common_tests)
        for tag, d in dir_dict[proj].items():
            with tarfile.open(f"{proj}-{tag}.tar", "w") as tar:
                for f in filter(lambda x: get_test_name(x) in common_tests, d.glob("**/*")):
                    tar.add(f.absolute(), arcname=f"{tag}/{get_test_name(f)}-{f.name}")
            mount_dir = "/home/ctestfuzz/fuzz-" + config.DIR_MAPPING[proj]
            with open("{proj}-{tag}.tar", 'rb') as f:
                container.put_archive(mount_dir, f.read())
    """
    for proj, common_tests in proj_common_tests.items():
        #print(proj, common_tests)
        for tag, d in dir_dict[proj].items():
            mount_dir = "/home/ctestfuzz/fuzz-" + config.DIR_MAPPING[proj]
            #container_id = subprocess.run(["docker run", "-d", "-t", "--mount type=bind,source={d.absolute()},target={mount_dir}/tmp,readonly", f"shuaiwang516/confuzz-image:{docker_tag}"], stdout=subprocess.PIPE, check=True, shell=True)
            #subprocess.run(['ls', '-l'], stdout=subprocess.PIPE).stdout.decode('utf-8')
            #container = client.containers.run(f'shuaiwang516/confuzz-image:{docker_tag}',
            #        mounts = [docker.types.Mount(mount_dir + "/tmp",
            #            str(d.absolute()), type = "bind",  read_only = True)],
            #        detach=True)
            with open(f"common-tests/{tag}-{proj}.txt", "w") as output:
                for f in filter(lambda x: get_test_name(x) in common_tests, d.glob("**/*.jed")):
                    output.write(tag + '/' + str(f.relative_to(d)) + '\n')
            #container_id = subprocess.run(f"docker cp jeds.txt {mount_dir}/jeds.txt", shell=True, check=True)


if __name__ == "__main__":
    if len(sys.argv) != 7:
        print("Usage: python3 <this file> data_dir confuzz_round jqf_round random_round output_dir tag")
        sys.exit(1)

    #projects = ["hiveql"]
    data_dir, confuzz_round, jqf_round, random_round, output_dir, docker_tag = sys.argv[1:]
    data_dir = Path(data_dir)
    dir_dict = {
        proj: {
            "Confuzz": data_dir/f"r{confuzz_round}/r{confuzz_round}-{iden}/r{confuzz_round}-{iden}-output/",
            "JQF": data_dir/f"r{jqf_round}/r{jqf_round}-{iden}/r{jqf_round}-{iden}-output/",
            "Random": data_dir/f"r{random_round}/r{random_round}-{iden}/r{random_round}-{iden}-output/"
        }
    for proj, iden in config.PROJECTS_MAPPING.items()}
    main(docker_tag, dir_dict, Path(output_dir))
