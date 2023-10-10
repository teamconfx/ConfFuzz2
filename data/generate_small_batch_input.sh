#!/bin/bash
TEST_NUM=$1
if [[ -z $TEST_NUM ]]; then
    echo "Please set the number of tests to run"
    exit 1
fi

for f in $(find . -name "ctest_list.txt");
do
    mkdir -p $(echo $f | sed -e 's/\/test_list\//\/fake_list\//g' -e 's/ctest_list.txt//g')         # create fake_list dir
    cat $f | shuf | head -n $TEST_NUM > $(echo $(echo $f | sed -e 's/\/test_list\//\/fake_list\//g'))      # randomly select ${TEST_NUM} tests to run
done
