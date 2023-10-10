dockerTag=JUL25
build_docker () {
    containerId=$(docker run -d -it \
        --mount type=bind,source=/home/tony/azure-confuzz-data/r7253/r7253-$3/r7253-$3-output,target=/home/ctestfuzz/$2/Confuzz,readonly \
        --mount type=bind,source=/home/tony/azure-confuzz-data/r7255/r7255-$3/r7255-$3-output,target=/home/ctestfuzz/$2/JQF,readonly \
        --mount type=bind,source=/home/tony/azure-confuzz-data/r7254/r7254-$3/r7254-$3-output,target=/home/ctestfuzz/$2/Random,readonly \
        shuaiwang516/confuzz-image:$dockerTag bash)
    docker cp common-tests/Confuzz-$1.txt $containerId:/home/ctestfuzz/$2/Confuzz.txt
    docker cp common-tests/JQF-$1.txt $containerId:/home/ctestfuzz/$2/JQF.txt
    docker cp common-tests/Random-$1.txt $containerId:/home/ctestfuzz/$2/Random.txt
    docker cp jacococli.jar $containerId:/home/ctestfuzz/$2/jacococli.jar
    docker cp gen_cov_in_docker.sh $containerId:/home/ctestfuzz/$2
    docker exec -w /home/ctestfuzz/confuzz $containerId git pull
    docker exec -w /home/ctestfuzz/confuzz $containerId git checkout read_from_list
    docker exec -w /home/ctestfuzz/confuzz $containerId mvn install -DskipTests
}

gen_csv() {
    project=$1
    id=$2
    path=$3
    filter=$4
    build_docker $project $path $id
    docker exec -w /home/ctestfuzz/$path $containerId ls
    docker exec -w /home/ctestfuzz/$path $containerId bash gen_cov_in_docker.sh $filter
    mkdir -p cov_csv/$project
    docker cp $containerId:/home/ctestfuzz/$2/Confuzz-output cov_csv/$project/Confuzz
    docker cp $containerId:/home/ctestfuzz/$2/JQF-output cov_csv/$project/JQF
    docker cp $containerId:/home/ctestfuzz/$2/Random-output cov_csv/$project/Random
    docker stop $containerId
    docker rm $containerId
}
gen_csv HCommon hadoophadoopcommon fuzz-hadoop/hadoop-common-project/hadoop-common org/apache/hadoop/common &
gen_csv HDFS hadoophadoophdfs fuzz-hadoop/hadoop-hdfs-project/hadoop-hdfs org/apache/hadoop/hdfs &
gen_csv Yarn hadoophadoopyarncommon fuzz-hadoop/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-common org/apache/hadoop/yarn &
gen_csv Mapreduce hadoophadoopmapreduceclientcore fuzz-hadoop/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core org/apache/hadoop/mapred &
gen_csv HBase hbasehbaseserver fuzz-hbase/hbase-server org/apache/hbase &
gen_csv Alluxio alluxiocommon fuzz-alluxio/core/common org/apache/alluxio &
gen_csv Kylin kylincorecube fuzz-kylin/core-cube org/apache/kylin &
gen_csv Flink flinkflinkcore fuzz-flink/flink-core org/apache/flink &
gen_csv Hive hiveql fuzz-hive/ql org/apache/hive &
gen_csv Zeppelin zeppelinzeppelinzengine fuzz-zeppelin/zeppelin-zengine org/apache/zeppelin &
wait < <(jobs -p)
