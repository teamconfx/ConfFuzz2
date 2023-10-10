#!/bin/bash
set -e

# A list that contains all the projects that we want to test
supported_proj="hcommon hdfs"

# This script is used to test the azure scripts locally
proj=$1
if [ -z "$proj" ]; then
    echo "Usage: $0 <project>"
    exit 1
fi

# check whether proj is supported
if [[ " ${supported_proj[@]} " =~ " ${proj} " ]]; then
    echo "Testing $proj"
else
    echo "Project $proj is not supported yet"
    exit 1
fi

# ranomly read two line from test_list file and assign it to variable test
test_list_file=../data/$proj/test_list.txt
echo "$test_list_file"
test_list=$(shuf -n 1 $test_list_file | tr '\n' ';' | sed 's/;$//')
echo $test_list

# switch whehter proj is hdfs or hcommon
if [ "$proj" == "hdfs" ]; then
    python3 fuzz_on_azure.py hdfs $test_list ./hdfs-local-test-output/ 300
elif [ "$proj" == "hcommon" ]; then
    python3 fuzz_on_azure.py hcommon $test_list ./hcommon-local-test-output/ 300
fi