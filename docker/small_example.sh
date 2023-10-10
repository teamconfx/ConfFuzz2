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

# # Hive
bash docker-fuzz.sh \
     ${TAG} \
     fuzz-hive \
     ql \
     /home/ctestfuzz/fuzz-hive/regex.json \
     org.apache.hadoop.hive.conf.HiveConfGenerator \
     /home/ctestfuzz/fuzz-hive/ql/target/classes/ctest.xml \
     60 \
     org.apache.hadoop.hive.ql.exec.vector.mapjoin.TestMapJoinOperator#testMultiKey3+org.apache.hadoop.hive.ql.udf.generic.TestGenericUDFOPPlus#testIntervalYearMonthPlusIntervalYearMonth+org.apache.hadoop.hive.ql.txn.compactor.TestWorker2#testDoesNotGatherStatsIfCompactionFails+org.apache.hadoop.hive.ql.exec.TestMsckCreatePartitionsInBatches#testEqualNumberOfPartitions+org.apache.hadoop.hive.ql.exec.TestFunctionRegistry#testRegisterPermanentFunction \
     ${FUZZMODE}

# bash docker-fuzz.sh \
#      ${TAG} \
#      fuzz-kylin \
#      core-cube \
#      /home/ctestfuzz/fuzz-kylin/regex.json \
#      org.apache.kylin.common.ConfigurationGenerator \
#      /home/ctestfuzz/fuzz-hive/ql/target/classes/ctest.properties \
#      60 \
#      org.apache.kylin.cube.AggregationGroupRuleTest#testBadDesc2 \
#      ${FUZZMODE}
