#!/bin/bash
imageTag=$1
app=$2
testModule=$3
zipLink=$4
execContainerName=$5

wget -O jeds.zip $zipLink
ZIP_FILE_NAME=$(echo $zipLink | rev | cut -d'/' -f1 | rev | cut -d'.' -f1)
echo "Downloading $ZIP_FILE_NAME from Mir"
mkdir -p result/$app/${ZIP_FILE_NAME}_csv_output

echo "Generating Exec File for $zipLink"

EXEC_OUTPUT_DIR=/home/ctestfuzz/${app}/${testModule}/exec_output
JED_DIR=/home/ctestfuzz/${app}/${testModule}/jeds

# Run confuzz:dumpCov goal to dump the confuzz jed file to jacoco coverage exec file 
echo "Running confuzz:fuzz goal for generating exec zip file $zipLink"
docker run --name ${execContainerName} -w "/home/ctestfuzz/${app}/${testModule}" -d -i -t "shuaiwang516/confuzz-image:${imageTag}" bash
docker exec ${execContainerName} apt install -y zip unzip
docker exec ${execContainerName} wget -O jeds.zip $zipLink
docker exec ${execContainerName} unzip jeds.zip -d ${JED_DIR}
docker exec ${execContainerName} mkdir -p ${EXEC_OUTPUT_DIR}
docker exec -e "_JAVA_OPTIONS=-Xmx18g" ${execContainerName} mvn confuzz:dumpCov -Ddata.dir=${JED_DIR} -Doutput.dir=${EXEC_OUTPUT_DIR}
docker exec ${execContainerName} zip -jr ${ZIP_FILE_NAME}_exec.zip ${EXEC_OUTPUT_DIR}
docker cp ${execContainerName}:/home/ctestfuzz/$app/$testModule/${ZIP_FILE_NAME}_exec.zip result/$app
# docker stop ${execContainerName} && docker rm ${execContainerName}
