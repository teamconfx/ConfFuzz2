# To use this script, you need to build the image first with docker-build.sh

# Usage: bash docker-fuzz.sh <imageTag> <app> <testModule> <regexFile> <injectConfigFile> <duration> <test1+test2+...+testN>
# assumes that constraintFile is named constraint and regex file name
# root dir is where the two files are stored
# Note that fuzz-hadoop is stored in /home/ctestfuzz/ in the docker

#!/bin/bash
imageTag=$1
app=$2
testModule=$3
regexFile=$4
configGenerator=$5
injectConfigFile=$6
duration=$7
tests=$8
fuzzMode=$9
fuzzContainerName=confuzz
debugContainerName=debug
coverageContainerName=coverage
# injectConfigFile=target/classes/ctest.xml
# regexFile=/home/ctestfuzz/fuzz-hadoop/regex.json

IFS="+" read -ra test_methods <<< "$tests"

mkdir -p result/
mkdir -p temp/meringue

for test in "${test_methods[@]}"; do
  # split the test string into the test class and test method using the "#" delimiter
  #echo $test
  testClass=$(echo $test | cut -d '#' -f 1)
  testMethod=$(echo $test | cut -d '#' -f 2)
  echo "Test Class: $testClass, Test Method: $testMethod"

  mkdir -p result/${testClass}/

  VOLUME=$testMethod
  docker volume create $VOLUME

  # Run confuzz:fuzz goal
  echo "Running confuzz:fuzz goal for $testClass $testMethod"
  docker run --name ${fuzzContainerName} --mount source=$VOLUME,target=/home/ctestfuzz/$app/$testModule/target/meringue -w "/home/ctestfuzz/${app}/${testModule}" -d -i -t "shuaiwang516/confuzz-image:${imageTag}" bash
  docker exec  ${fuzzContainerName} mvn confuzz:fuzz -nsu -Dconfuzz.generator=${configGenerator} -Dmeringue.testClass=${testClass} -Dmeringue.testMethod=${testMethod} -DregexFile=${regexFile} -Dmeringue.duration=PT${duration}S -DfuzzMode=${fuzzMode}
  docker cp ${fuzzContainerName}:/home/ctestfuzz/$app/$testModule/target/temp/meringue/confuzz.properties temp/meringue/
  docker stop ${fuzzContainerName} && docker rm ${fuzzContainerName}

  # Run confuzz:debug goal
  echo "Running confuzz:debug goal for $testClass $testMethod"

  docker run --name ${debugContainerName} --mount source=$VOLUME,target=/home/ctestfuzz/$app/$testModule/target/meringue -w "/home/ctestfuzz/${app}/${testModule}" -d -i -t "shuaiwang516/confuzz-image:${imageTag}" bash
  docker exec ${debugContainerName} mkdir -p /home/ctestfuzz/$app/$testModule/target/temp/meringue/
  docker cp temp/meringue/confuzz.properties  ${debugContainerName}:/home/ctestfuzz/$app/$testModule/target/temp/meringue/confuzz.properties
  docker exec  ${debugContainerName} mvn confuzz:debug -nsu -Dconfuzz.generator=${configGenerator} -Dmeringue.testClass=${testClass} -Dmeringue.testMethod=${testMethod} -DregexFile=${regexFile} -DinjectConfigFile=${injectConfigFile} -DfuzzMode=${fuzzMode}
  docker stop ${debugContainerName} && docker rm ${debugContainerName}

  # Run confuzz:coverage goal
  echo "Running confuzz:coverage goal for $testClass $testMethod"

  docker run --name ${coverageContainerName} --mount source=$VOLUME,target=/home/ctestfuzz/$app/$testModule/target/meringue -w "/home/ctestfuzz/${app}/${testModule}" -d -i -t "shuaiwang516/confuzz-image:${imageTag}" bash
  docker exec ${coverageContainerName} mkdir -p /home/ctestfuzz/$app/$testModule/target/temp/meringue/
  docker cp temp/meringue/confuzz.properties  ${coverageContainerName}:/home/ctestfuzz/$app/$testModule/target/temp/meringue/confuzz.properties
  docker exec ${coverageContainerName} mvn confuzz:coverage -nsu -Dconfuzz.generator=${configGenerator} -Dmeringue.testClass=${testClass} -Dmeringue.testMethod=${testMethod} -DregexFile=${regexFile} -DinjectConfigFile=${injectConfigFile} -DfuzzMode=${fuzzMode}
  docker cp ${coverageContainerName}:/home/ctestfuzz/$app/$testModule/target/meringue/${testClass}/${testMethod} result/${testClass}/
  docker stop ${coverageContainerName} && docker rm ${coverageContainerName}

  # Clean Volume
  docker volume rm $VOLUME

done

