#!/bin/bash
imageTag=$1
app=$2
testModule=$3
regexFile=$4
configGenerator=$5
tests=$6
fuzzContainerName=deflaky
# injectConfigFile=target/classes/ctest.xml
# regexFile=/home/ctestfuzz/fuzz-hadoop/regex.json

IFS="+" read -ra test_methods <<< "$tests"

mkdir -p result/

for test in "${test_methods[@]}"; do
  # split the test string into the test class and test method using the "#" delimiter
  #echo $test
  testClass=$(echo $test | cut -d '#' -f1)
  testMethod=$(echo $test | cut -d '#' -f2)
  echo "Test Class: $testClass, Test Method: $testMethod"

  mkdir -p result/${testClass}/

  # Run confuzz:fuzz goal
  echo "Running confuzz:fuzz goal for deflaky $testClass $testMethod"
  docker run --name ${fuzzContainerName} -w "/home/ctestfuzz/${app}/${testModule}" -d -i -t "shuaiwang516/confuzz-image:${imageTag}" bash
  docker exec ${fuzzContainerName} mvn confuzz:fuzz -Dconfuzz.generator=${configGenerator} -Dmeringue.testClass=${testClass} -Dmeringue.testMethod=${testMethod} -DregexFile=${regexFile} -DonlyCheckDefault
  docker cp ${fuzzContainerName}:/home/ctestfuzz/$app/$testModule/target/meringue result/
  docker stop ${fuzzContainerName} && docker rm ${fuzzContainerName}

done


