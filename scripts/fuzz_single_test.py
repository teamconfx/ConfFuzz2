import os, sys


def fuzz(build, test_name, app_dir, output_dir, constraint_file, regex_file, duration, total_timeout, java_home):
    test_class, test_method = test_name.split("#")
    print(f"======================Confuzz:fuzz {test_name} =========================", flush=True)
    if build.lower() == "true":
        os.system(f"cd {app_dir} && JAVA_HOME=\"{java_home}\" mvn clean install -DskipTests -Denforcer.skip")
    fuzz_cmd = f"cd {app_dir} && export JAVA_HOME=\"{java_home}\" && timeout {total_timeout} mvn confuzz:fuzz -Dmeringue.testClass={test_class}\
        -Dmeringue.testMethod={test_method} -Dmeringue.outputDirectory={output_dir}/ \
        -DconstraintFile={constraint_file} -DregexFile={regex_file} -Dmeringue.duration={duration} -DexpectedExceptions=java.lang.AssertionError && sudo rm -rf target/test/data"
    os.system(fuzz_cmd)


## mode can be 'analyze' or 'debug'
def check(mode, test_name, app_dir, output_dir, constraint_file, regex_file, java_home):
    test_class, test_method = test_name.split("#")
    print(f"======================Confuzz:{mode} {test_name} =========================", flush=True)
    check_cmd = f"cd {app_dir} && export JAVA_HOME=\"{java_home}\" && mvn confuzz:{mode} -Dmeringue.testClass={test_class}\
        -Dmeringue.testMethod={test_method} -Dmeringue.outputDirectory={output_dir}/ \
        -DconstraintFile={constraint_file} -DregexFile={regex_file} -Dmeringue.jacocoFormats="" && sudo rm -rf target/test/data"
    os.system(check_cmd)


if __name__ == '__main__':
    '''
    <mode> : 'fuzz', 'check'; must run 'fuzz' before 'check'
    <build> : 'true', 'false'; build application or not
    <test_name> : test_class#test_method
    <app_dir> : directory that contains pom.xml of target application
    <output_dir> : directory that stores fuzzing outputs
    <constraint_file> : file path that contains configuration parameter constraints
    <regex_file> : file path that contains regex constraints
    <fuzzing_duration> : timeout of fuzzing campaign
    <total_timeout> : timeout of running mvn, should be slightly larger than <fuzzing_duration>
    <JAVA9+_HOME> : requires java 9+
    '''
    if len(sys.argv) != 11:
        raise ValueError("Usage: python3 fuzz_single_test.py <mode> <build> <test_name> <app_dir> <output_dir> <constraint_file> <regex_file> <fuzzing_duration> <total_timeout> <JAVA9+_HOME>")
    mode, build, test_name, app_dir, output_dir, constraint_file, regex_file, duration, total_timeout, java_home, sudo_password = sys.argv[1:]
    abs_app_dir, abs_output_dir, abs_constraint_file, abs_regex_file = list(map(lambda x : os.path.abspath(x), [app_dir, output_dir, constraint_file, regex_file]))
    if mode.lower() == "fuzz":
        fuzz(build, test_name, abs_app_dir, abs_output_dir, abs_constraint_file, abs_regex_file, duration, total_timeout, java_home, sudo_password)
    elif mode.lower() == "check":
        check("analyze", test_name, abs_app_dir, abs_output_dir, abs_constraint_file, abs_regex_file, java_home, sudo_password)
        check("debug", test_name, abs_app_dir, abs_output_dir, abs_constraint_file, abs_regex_file, java_home, sudo_password)
    else:
        raise ValueError(f"mode must be fuzz or check, current mode = {mode}")