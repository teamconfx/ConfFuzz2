# Usage: bash docker-coverage.sh $imageTag $app $testModule $covlink

imageTag=$1
app=$2
testModule=$3
regexFile=$4
configGenerator=$5
injectConfigFile=$6
failureDirLink=$7
startFailureIdx=$8
endFailureIdx=$9

containerName=fp
failureDir=confuzz_failure

# write a function to run the test module and generate the coverage report

run() {
    failureFileIdx=$1
    # test name is the last line of the failure file
    testName=$(tail -n 1 $failureDir/$failureFileIdx.json)
    testClass=$(echo $testName | cut -d '#' -f 1)
    testMethod=$(echo $testName | cut -d '#' -f 2)
    echo $containerName
    # remove the last line of the failure file
    sed -i '$ d' $failureDir/$failureFileIdx.json
    echo "Running confuzz:fuzz-fp goal for $testClass $testMethod"
    docker run --name ${containerName} -w "/home/ctestfuzz/${app}/${testModule}" -d -i -t "shuaiwang516/confuzz-image:${imageTag}" bash
    docker cp $failureDir/$failureFileIdx.json ${containerName}:/home/ctestfuzz/$app/$testModule
    docker exec ${containerName} mvn confuzz:fuzz-fp -Dconfuzz.generator=${configGenerator} -Dmeringue.testClass=${testClass} -Dmeringue.testMethod=${testMethod} -DregexFile=${regexFile} -DinjectConfigFile=${injectConfigFile} -DminConfigFile=${failureFileIdx}.json
    docker cp ${containerName}:/home/ctestfuzz/$app/$testModule/target/meringue/ result/$app
    docker stop ${containerName} && docker rm ${containerName}
}


mkdir -p result/$app

# download the failure files directory into a new directory called confuzz_failure
mkdir -p $failureDir
wget -r -nH -P $failureDir --cut-dirs=4 --no-parent --reject="index.html*" $failureDirLink

# for all the failure files from ${startFailureIdx}.json to ${endFailureIdx}.json
for ((i=$startFailureIdx; i<$endFailureIdx; i++)); do
    echo "Processing failure $i"
    run $i
done





    
