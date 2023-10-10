import os
import sys
import e2e_config
import json
# This file is used to check the result of the e2e testing
OUTPUT_DIR = 'target/meringue/edu.illinois.fakefuzz.TestDebug'
EXPECTED_DIR = 'src/test/resources/e2e_expected'
ERROR_MSG = ""
ERROR_TAG="[CONFUZZ-ERROR]"

def get_json(json_file):
    with open(json_file) as f:
        json_result = json.load(f)
    return json_result


def get_failure_json(file_dir, test_name):
    json_file = os.path.join(file_dir, test_name, "failures.json")
    return get_json(json_file)


def compare_json(test_name, expected_json, actual_json):
    global ERROR_MSG
    expected_debug = expected_json.get("debugResults")
    actual_debug = actual_json.get("debugResults")
    if len(expected_debug) != len(actual_debug):
        ERROR_MSG += ERROR_TAG + "The number of debug results is different for test: " + test_name + "\n"
        # put all the debug results in ERROR_MSG
        for i in range(len(expected_debug)):
            ERROR_MSG += "expected_debug[" + \
                str(i) + "]: " + str(expected_debug[i]) + "\n"
        for i in range(len(actual_debug)):
            ERROR_MSG += "actual_debug[" + \
                str(i) + "]: " + str(actual_debug[i]) + "\n"
        return False
    expected_result_set = set()
    actual_result_set = set()
    for i in range(len(expected_debug)):
        expected_debug_result = expected_debug[i]
        actual_debug_result = actual_debug[i]

        expected_failure = expected_debug_result.get("failure")
        expected_errorMessage = expected_debug_result.get("errorMessage")
        expected_reproStatus = expected_debug_result.get("reproStatus")

        expected_str = expected_failure + " , " + expected_errorMessage + \
            " , " + expected_reproStatus
        
        actual_failure = actual_debug_result.get("failure")
        actual_errorMessage = actual_debug_result.get("errorMessage")
        actual_reproStatus = actual_debug_result.get("reproStatus")

        actual_str = actual_failure + " , " + actual_errorMessage + \
            " , " + actual_reproStatus
        

        expected_result_set.add(expected_str)
        actual_result_set.add(actual_str)

    if expected_result_set != actual_result_set:
        ERROR_MSG += ERROR_TAG + "The debug results are different for test: " + test_name + "\n"
        return False


def compare_test_result(test_name):
    expected_json = get_failure_json(EXPECTED_DIR, test_name)
    actual_json = get_failure_json(OUTPUT_DIR, test_name)
    return compare_json(test_name, expected_json, actual_json)


def compare_all_test_result():
    for test_name in e2e_config.TEST_LIST:
        compare_test_result(test_name)


def main():
    global ERROR_MSG
    compare_all_test_result()
    print(ERROR_MSG)


if __name__ == "__main__":
    main()
