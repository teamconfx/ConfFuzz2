#!/bin/bash
TAG=$1
FUZZMODE=$2
if [[ -z "$TAG" ]]; then
  echo "Please set TAG"
  exit 1
fi

if [[ -z "$FUZZMODE" ]]; then
    echo "Please set FUZZMODE"
    exit 1
fi

# Usage: bash docker-fuzz.sh <imageTag> <app> <testModule> <regexFile> <configGenerator> <injectConfigFile> <duration> <test1+test2+...+testN> <fuzzMode>

# HCommon
bash docker-fuzz.sh \
     ${TAG} \
     fuzz-hadoop \
     hadoop-common-project/hadoop-common \
     /home/ctestfuzz/fuzz-hadoop/regex.json \
     org.apache.hadoop.conf.ConfigurationGenerator \
     /home/ctestfuzz/fuzz-hadoop/hadoop-common-project/hadoop-common/target/classes/ctest.xml \
     60 \
     org.apache.hadoop.conf.TestDebug#test \
     ${FUZZMODE}


# HDFS
bash docker-fuzz.sh \
     ${TAG} \
     fuzz-hadoop \
     hadoop-hdfs-project/hadoop-hdfs/ \
     /home/ctestfuzz/fuzz-hadoop/regex.json \
     org.apache.hadoop.conf.ConfigurationGenerator \
     /home/ctestfuzz/fuzz-hadoop/hadoop-hdfs-project/hadoop-hdfs/target/classes/ctest.xml \
     60 \
     org.apache.hadoop.TestDebug#test \
     ${FUZZMODE}

# Yarn-common
bash docker-fuzz.sh \
     ${TAG} \
     fuzz-hadoop \
     hadoop-yarn-project/hadoop-yarn/hadoop-yarn-common \
     /home/ctestfuzz/fuzz-hadoop/regex.json \
     org.apache.hadoop.conf.ConfigurationGenerator \
     /home/ctestfuzz/fuzz-hadoop/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-common/target/classes/ctest.xml \
     60 \
     org.apache.hadoop.yarn.TestDebug#test \
     ${FUZZMODE}

# MapReduce: client-core     
bash docker-fuzz.sh \
     ${TAG} \
     fuzz-hadoop \
     hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core/ \
     /home/ctestfuzz/fuzz-hadoop/regex.json \
     org.apache.hadoop.conf.ConfigurationGenerator \
     /home/ctestfuzz/fuzz-hadoop/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core/target/classes/ctest.xml \
     60 \
     org.apache.hadoop.mapred.TestDebug#test \
     ${FUZZMODE}

     
# Kylin: core-cube
bash docker-fuzz.sh \
     ${TAG} \
     fuzz-kylin \
     core-cube \
     /home/ctestfuzz/fuzz-kylin/regex.json \
     org.apache.kylin.common.ConfigurationGenerator \
     /home/ctestfuzz/fuzz-kylin/core-cube/target/classes/ctest.properties \
     60 \
     org.apache.kylin.cube.TestDebug#test \
     ${FUZZMODE}     

# HBase: hbase-server
bash docker-fuzz.sh \
     ${TAG} \
     fuzz-hbase \
     hbase-server/ \
     /home/ctestfuzz/fuzz-hbase/regex.json \
     org.apache.hadoop.conf.ConfigurationGenerator \
     /home/ctestfuzz/fuzz-hbase/hbase-server/target/classes/ctest.xml \
     60 \
     org.apache.hadoop.hbase.TestDebug#test \
     ${FUZZMODE}     


# Flink: flink-core     
bash docker-fuzz.sh \
     ${TAG} \
     fuzz-flink \
     flink-core/ \
     /home/ctestfuzz/fuzz-flink/regex.json \
     org.apache.flink.configuration.ConfigurationGenerator \ 
     /home/ctestfuzz/fuzz-flink/flink-core/ctest.yaml \
     60 \
     org.apache.flink.TestDebug#test \
     ${FUZZMODE}

# Alluxio: core/common/
bash docker-fuzz.sh \
     ${TAG} \
     fuzz-alluxio \
     core/common/ \ 
     /home/ctestfuzz/fuzz-alluxio/regex.json \
     alluxio.conf.ConfigurationGenerator \
     /home/ctestfuzz/fuzz-alluxio/core/common/target/classes/ctest.properties \
     60 \
     alluxio.TestDebug#test \
     ${FUZZMODE}

# Zeppelin: zeppelin-zengine
bash docker-fuzz.sh \
     ${TAG} \
     fuzz-zeppelin \
     zeppelin-zengine \
     /home/ctestfuzz/fuzz-zeppelin/regex.json \
     org.apache.zeppelin.conf.ConfigurationGenerator \
     /home/ctestfuzz/fuzz-zeppelin/zeppelin-zengine/target/classes/zeppelin-site.xml \
     60 \
     org.apache.zeppelin.TestDebug#test \
     ${FUZZMODE}
