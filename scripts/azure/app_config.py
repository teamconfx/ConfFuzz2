# This is the configuration file for building all the applications in docker 
import os

# fuzzing target modules
fuzz_modules = {
    'fuzz-hadoop': ['hadoop-common-project/hadoop-common', 
                    'hadoop-hdfs-project/hadoop-hdfs', 
                    'hadoop-yarn-project/hadoop-yarn/hadoop-yarn-common',
                    'hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core'],
    'fuzz-hbase': ['hbase-server'],
    'fuzz-hive': ['ql'],
    'fuzz-zeppelin': ['zeppelin-zengine'],
    'fuzz-alluxio': ['core/common'],
    'fuzz-kylin': ['core-cube'],
    'fuzz-flink': ['flink-core']
}

PREFIX = '/home/ctestfuzz/'
SUFFIX = '/regex.json'
regex_path = {
    'fuzz-hadoop': PREFIX + 'fuzz-hadoop' + SUFFIX,
    'fuzz-hbase': PREFIX + 'fuzz-hbase' + SUFFIX,
    'fuzz-hive': PREFIX + 'fuzz-hive' + SUFFIX,
    'fuzz-zeppelin': PREFIX + 'fuzz-zeppelin' + SUFFIX,
    'fuzz-alluxio': PREFIX + 'fuzz-alluxio' + SUFFIX,
    'fuzz-kylin': PREFIX + 'fuzz-kylin' + SUFFIX,
    'fuzz-flink': PREFIX + 'fuzz-flink' + SUFFIX
}

PREFIX = '/home/ctestfuzz/'

def get_inject_ctest_file_path(app, module):
    if app == 'fuzz-hadoop':
        return os.path.join(PREFIX, 'fuzz-hadoop', module, "target/classes/ctest.xml")
    elif app == 'fuzz-hbase':
        return os.path.join(PREFIX, 'fuzz-hbase', module, "target/classes/ctest.xml")
    elif app == 'fuzz-hive':
        return os.path.join(PREFIX, 'fuzz-hive', module, "target/classes/ctest.xml")
    elif app == 'fuzz-zeppelin':
        return os.path.join(PREFIX, 'fuzz-zeppelin', module, "target/classes/zeppelin-site.xml")
    elif app == 'fuzz-alluxio':
        return os.path.join(PREFIX, 'fuzz-alluxio', module, "target/classes/ctest.properties")
    elif app == 'fuzz-kylin':
        return os.path.join(PREFIX, 'fuzz-kylin', module, "target/classes/ctest.properties")
    elif app == 'fuzz-flink':
        return os.path.join(PREFIX, 'fuzz-flink', module, "ctest.yaml")
    else:
        return "Does not support {}".format(app)

def get_generator_class(app):
    if app == 'fuzz-hadoop':
        return 'org.apache.hadoop.conf.ConfigurationGenerator'
    elif app == 'fuzz-hbase':
        return 'org.apache.hadoop.conf.ConfigurationGenerator'
    elif app == 'fuzz-hive':
        return 'org.apache.hadoop.hive.conf.HiveConfGenerator'
    elif app == 'fuzz-zeppelin':
        return 'org.apache.zeppelin.conf.ConfigurationGenerator'
    elif app == 'fuzz-alluxio':
        return 'alluxio.conf.ConfigurationGenerator'
    elif app == 'fuzz-kylin':
        return 'org.apache.kylin.common.ConfigurationGenerator'
    elif app == 'fuzz-flink':
        return 'org.apache.flink.configuration.ConfigurationGenerator'
    else:
        return "Does not have generator for {}".format(app)
