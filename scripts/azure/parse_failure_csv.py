import os, sys
import json
import config
from zipfile import ZipFile
from pathlib import Path


# This script recursively finds all the failures.csv files from data dirs
# It does not merge anything!!! It just try to put all the lines from each failures.json to a single file called failures-output.csv

# delete the first line of each file and merge all files into one file named as failures.csv,
# and save it in the current directory
def merge_failure_json_files(search_dir: Path):
    ret = []
    zip_files = filter(lambda x: x.name.endswith(".zip"), search_dir.iterdir())
    with open(search_dir / "failures-output.csv", "w") as outfile:
        # write a title of the csv file
        outfile.write(
            config.NEW_DELIMITER.join(["test_name","failure","errorMessage","stackTrace","reproStatus","replayedFailure","replayedErrorMessage","replayedStackTrace","replayedFile","minConfig","debugFiles"]) + "\n")
        for zip_file in zip_files:
            with ZipFile(zip_file) as zf:
                json_files = filter(lambda x: x.endswith("failures.json"), zf.namelist())
                for fname in json_files:
                    try:
                        with zf.open(fname) as infile:
                            data = json.load(infile)
                            test_name = data['testClass'] + "#" + data['testMethod']
                            debug_results = data['debugResults']
                            for debug_result in debug_results:
                                outfile.write(config.NEW_DELIMITER.join([
                                    test_name, debug_result.get('failure', ""),
                                    debug_result.get('errorMessage', "").replace("\n", ""),
                                    debug_result.get('stackTrace', ""),
                                    debug_result.get('reproStatus', ""),
                                    debug_result.get('replayedFailure', ""),
                                    debug_result.get('replayedErrorMessage', "").replace("\n", ""),
                                    debug_result.get('replayedStackTrace', ""),
                                    debug_result.get('replayedFile', ""),
                                    json.dumps(debug_result.get('minConfig', "")),
                                    json.dumps(debug_result.get('debugFiles', ""))
                                ]) + "\n")
                                
                                if 'errorMessage' not in debug_result:
                                    debug_result['errorMessage'] = ''
                                else:
                                    debug_result['errorMessage'] = debug_result['errorMessage'].replace("\n", "")
                                if debug_result['reproStatus'] == 'DIFFERENT':
                                    if 'replayedFailure' not in debug_result:
                                        debug_result['reproStatus'] = "FLAKY"
                                if 'replayedErrorMessage' not in debug_result:
                                    debug_result['replayedErrorMessage'] = ''
                                else:
                                    debug_result['replayedErrorMessage'] = debug_result['replayedErrorMessage'].replace("\n", "")
                                if debug_result.get("replayedFile", "") != "":
                                    debug_result['replayedFile'] = debug_result['replayedFile'].split("/")[-1]
                                debug_result["stackTrace"] = debug_result["stackTrace"][:-1].split(",")
                                if debug_result.get("replayedStackTrace", "") != "":
                                    debug_result["replayedStackTrace"] = debug_result["replayedStackTrace"][:-1].split(",")
                                debug_result['testClass'] = data['testClass']
                                debug_result['testMethod'] = data['testMethod']
                                ret.append(debug_result)
                    except json.JSONDecodeError as e:
                        print(f"Error decoding JSON in file: {fname}")
                        print(e)
    return ret

def main(search_dir):
    return merge_failure_json_files(search_dir)
