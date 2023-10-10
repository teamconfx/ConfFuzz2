# This is the configuration file for building all the applications in docker 
import os
build_modules = {
    'fuzz-hbase': ['confuzz', ['hbase-common', 'hbase-server']],
    'fuzz-hadoop': ['confuzz', ['hadoop-common-project/hadoop-common', 
                            'hadoop-hdfs-project/hadoop-hdfs', 
                            'hadoop-yarn-project/hadoop-yarn/hadoop-yarn-common',
                            'hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core']],
    'fuzz-hive': ['confuzz', ['common', 'ql']],
    'fuzz-zeppelin': ['confuzz', ['zeppelin-common', 'zeppelin-interpreter', 'zeppelin-zengine']],
    'fuzz-alluxio': ['confuzz', ['core/common']],
    'fuzz-kylin': ['confuzz', ['core-common', 'core-cube', 'core-job', 'core-metadata', 'core-metrics', 'core-storage']],
    'fuzz-flink': ['confuzz', ['flink-core']],
}
