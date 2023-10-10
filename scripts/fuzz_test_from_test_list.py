import os, sys
import fuzz_single_test as single


def build(app_dir, java_home):
    os.system(f"cd {app_dir} && JAVA_HOME=\"{java_home}\" mvn clean install -DskipTests -Denforcer.skip")


def get_test_list_from_file(list_file):
    with open(list_file, 'r') as f:
        return list(map(lambda x : x.strip(), f.readlines()))



def fuzz_from_list(test_list_file, app_dir, output_dir, constraint_file, regex_file, duration, total_timeout, java_home):
    build(app_dir, java_home)
    tests = get_test_list_from_file(test_list_file)
    for test in tests:
        single.fuzz("false", test, app_dir, output_dir, constraint_file, regex_file, duration, total_timeout, java_home)


def check_from_list(test_list_file, app_dir, output_dir, constraint_file, regex_file, java_home):
    build(app_dir, java_home)
    tests = get_test_list_from_file(test_list_file)
    for test in tests:
        single.check("analyze", test, app_dir, output_dir, constraint_file, regex_file, java_home)
        single.check("debug", test, app_dir, output_dir, constraint_file, regex_file, java_home)



if __name__ == '__main__':
    '''
    <mode> : 'fuzz', 'check'; must run 'fuzz' before 'check'
    <test_list_file> : file path that contains the list of fuzzing tests
    <app_dir> : directory that contains pom.xml of target application
    <output_dir> : directory that stores fuzzing outputs
    <constraint_file> : file path that contains configuration parameter constraints
    <regex_file> : file path that contains regex constraints
    <fuzzing_duration> : timeout of fuzzing campaign
    <total_timeout> : timeout of running mvn, should be slightly larger than <fuzzing_duration>
    <JAVA9+_HOME> : requires java 9+
    '''

    if len(sys.argv) != 10:
        raise ValueError("Usage: python3 fuzz_single_test.py <mode> <test_list_file> <app_dir> <output_dir> <constraint_file> <regex_file> <fuzzing_duration> <total_timeout> <JAVA9+_HOME>")
    mode, test_list_file, app_dir, output_dir, constraint_file, regex_file, duration, total_timeout, java_home = sys.argv[1:]
    abs_test_list_file, abs_app_dir, abs_output_dir, abs_constraint_file, abs_regex_file = list(map(lambda x : os.path.abspath(x), [test_list_file, app_dir, output_dir, constraint_file, regex_file]))
    if mode.lower() == "fuzz":
        fuzz_from_list(abs_test_list_file, abs_app_dir, abs_output_dir, abs_constraint_file, abs_regex_file, duration, total_timeout, java_home)
    elif mode.lower() == "check":
        check_from_list(abs_test_list_file, abs_app_dir, abs_output_dir, abs_constraint_file, abs_regex_file, java_home)
    else:
        raise ValueError(f"mode must be fuzz or check, current mode = {mode}")