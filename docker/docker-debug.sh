#!/bin/bash

# testModule
# yarn-common: hadoop-yarn-project/hadoop-yarn/hadoop-yarn-common
# mapreduce-client-core: hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core

# injectConfigFile
# yarn-common: target/classes/ctest.xml
# mapreduce-client-core: target/classes/ctest.xml

imageTag=$1
testModule=$2
test=$3
azure_data_dir=$4
containerName=confuzz-debug-container
constraintFile=constraint
regexFile=/home/ctestfuzz/fuzz-hadoop/regex.json
injectConfigFile=target/classes/ctest.xml

testClass=$(echo $test | cut -d '#' -f 1)
testMethod=$(echo $test | cut -d '#' -f 2)

targetDataDir=$(find ${azure_data_dir} -wholename ${testModule}/${testMethod} -type d)
# remove the content after last slash
targetDataDir=${targetDataDir%/*}
echo "Target data dir: ${targetDataDir}"

# run a debug container
docker run --name ${containerName}  -w "/home/ctestfuzz/fuzz-hadoop/${testModule}" -d -i -t "shuaiwang516/confuzz-image:${imageTag}" bash

# create meringue dir in the container
docker exec ${containerName} mkdir -p /home/ctestfuzz/fuzz-hadoop/$testModule/target/meringue

# copy the target data dir to the container
docker cp ${targetDataDir} ${containerName}:/home/ctestfuzz/fuzz-hadoop/$testModule/target/meringue/

# run the analyze command
# docker exec ${containerName} mvn confuzz:analyze -Dmeringue.testClass=${testClass} -Dmeringue.testMethod=${testMethod} -DregexFile=${regexFile}

# run the debug command
docker exec ${containerName} mvn confuzz:debug -Dmeringue.testClass=${testClass} -Dmeringue.testMethod=${testMethod} -DregexFile=${regexFile} -DinjectConfigFile=${injectConfigFile}

