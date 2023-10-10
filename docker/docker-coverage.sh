# Usage: bash docker-coverage.sh $imageTag $app $testModule $zipLink

imageTag=$1
app=$2
testModule=$3
zipLink=$4
containerName=coverage

mkdir -p result/$app

# download the coverage exec file from zipLink

#wget -O coverage.zip $zipLink
#rm -rf covDir && unzip coverage.zip -d covDir && rm coverage.zip


# launch the docker container for calculating coverage
docker run --name ${containerName} -v $(pwd)/covDir:/home/ctestfuzz/${app}/${testModule}/covDir -w "/home/ctestfuzz/${app}/${testModule}" -d -i -t "shuaiwang516/confuzz-image:${imageTag}" bash


# docker cp covDir ${containerName}:/home/ctestfuzz/$app/$testModule/

# rm -rf covDir coverage.zip

# run the exec file generation script to dump jed to exec
bash docker-generate-exec.sh $imageTag $app $testModule $zipLink $containerName

# run the jacoco maven goal inside the docker container
docker exec ${containerName} bash -c 'mkdir outputs && for f in $(find -name "*exec" -type f); do ts=$(echo $f | rev | cut -d/ -f1 | rev | cut -d'.' -f1); mvn org.jacoco:jacoco-maven-plugin:0.8.7:report -Djacoco.dataFile=$f; mv target/site/jacoco/jacoco.csv outputs/${ts}.csv; done'

ZIP_FILE_NAME=$(echo $zipLink | rev | cut -d'/' -f1 | rev | cut -d'.' -f1)

docker cp ${containerName}:/home/ctestfuzz/${app}/${testModule}/outputs/ result/$app/${ZIP_FILE_NAME}_csv_output

docker stop ${containerName} && docker rm -f ${containerName}
