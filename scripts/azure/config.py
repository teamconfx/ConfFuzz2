from pathlib import Path
from typing import List, Dict
from configparser import ConfigParser
DELIMITER="@;@"
NEW_DELIMITER="¬"
EVALUATED_PROJECTS=["Hive", "HCommon", "HDFS", "HBase", "MapReduce", "Yarn", "Kylin", "Zeppelin"]
#EVALUATED_PROJECTS=["Hive", "HCommon", "HDFS", "HBase", "MapReduce", "Yarn", "Flink", "Kylin", "Zeppelin", "Alluxio"]
PROJECTS_MAPPING={
    "Hive": "hiveql",
    "HCommon": "hadoophadoopcommon",
    "HDFS": "hadoophadoophdfs",
    "HBase": "hbasehbaseserver",
    "MapReduce": "hadoophadoopmapreduceclientcore",
    "Yarn": "hadoophadoopyarncommon",
    "Flink": "flinkflinkcore",
    "Kylin": "kylincorecube",
    "Zeppelin": "zeppelinzeppelinzengine",
    "Alluxio": "alluxiocommon"
}
FUZZ_MAPPING={
    "Hive": "fuzz-hive",
    "HCommon": "fuzz-hadoop",
    "HDFS": "fuzz-hadoop",
    "HBase": "fuzz-hbase",
    "MapReduce": "fuzz-hadoop",
    "Yarn": "fuzz-hadoop",
    "Flink": "fuzz-flink",
    "Kylin": "fuzz-kylin",
    "Zeppelin": "fuzz-zeppelin",
    "Alluxio": "fuzz-alluxio"
}
DIR_MAPPING = {
    "Hive": "hive/ql",
    "HCommon": "hadoop/hadoop-common-project/hadoop-common",
    "HDFS": "hadoop/hadoop-hdfs-project/hadoop-hdfs",
    "HBase": "hbase/hbase-server",
    "MapReduce": "hadoop/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core",
    "Yarn": "hadoop/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-common",
    "Flink": "flink/flink-core",
    "Kylin": "kylin/core-cube",
    "Zeppelin": "zeppelin/zeppelin-zengine",
    "Alluxio": "alluxio/core/common"
}
TEST_NUM = {
    "Hive": 1000,
    "Alluxio": 158,
    "Flink": 238,
    "HBase": 980,
    "Zeppelin": 182,
    "MapReduce": 232,
    "HDFS": 1000,
    "Yarn": 341,
    "HCommon": 998,
    "Kylin": 105
}
FUZZ_MODULES = {
    "Hive": "ql",
    "HCommon": "hadoop-common-project/hadoop-common",
    "HDFS": "hadoop-hdfs-project/hadoop-hdfs",
    "HBase": "hbase-server",
    "MapReduce": "hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core",
    "Yarn": "hadoop-yarn-project/hadoop-yarn/hadoop-yarn-common",
    "Flink": "flink-core",
    "Kylin": "core-cube",
    "Zeppelin": "zeppelin-zengine",
    "Alluxio": "core/common"
}
CONFIG_GENERATOR = {
    "Hive": "org.apache.hadoop.hive.conf.HiveConfGenerator",
    "HCommon": "org.apache.hadoop.conf.ConfigurationGenerator",
    "HDFS": "org.apache.hadoop.conf.ConfigurationGenerator",
    "HBase": "org.apache.hadoop.conf.ConfigurationGenerator",
    "MapReduce": "org.apache.hadoop.conf.ConfigurationGenerator",
    "Yarn": "org.apache.hadoop.conf.ConfigurationGenerator",
    "Flink": "org.apache.flink.configuration.ConfigurationGenerator",
    "Kylin": "org.apache.kylin.common.ConfigurationGenerator",
    "Zeppelin": "org.apache.zeppelin.conf.ConfigurationGenerator",
    "Alluxio": "alluxio.conf.ConfigurationGenerator"
}

INJECTION_FILE = {
    "Hive": "target/classes/ctest.xml",
    "HCommon": "target/classes/ctest.xml",
    "HDFS": "target/classes/ctest.xml",
    "HBase": "target/classes/ctest.xml",
    "MapReduce": "target/classes/ctest.xml",
    "Yarn": "target/classes/ctest.xml",
    "Flink": "ctest.yaml",
    "Kylin": "target/classes/ctest.properties",
    "Zeppelin": "target/classes/zeppelin-site.xml",
    "Alluxio": "target/classes/ctest.properties"
}
REGEX_FILE = {
    "Hive": "../regex.json",
    "HCommon": "../../regex.json",
    "HDFS": "../../regex.json",
    "HBase": "../regex.json",
    "MapReduce": "../../../regex.json",
    "Yarn": "../../../regex.json",
    "Flink": "../regex.json",
    "Kylin": "../regex.json",
    "Zeppelin": "../regex.json",
    "Alluxio": "../../regex.json"
}
LOWER_TO_KEY = {
    "hive": "Hive",
    "hcommon": "HCommon",
    "hdfs": "HDFS",
    "hbase": "HBase",
    "mapreduce": "MapReduce",
    "yarn": "Yarn",
    "flink": "Flink",
    "kylin": "Kylin",
    "zeppelin": "Zeppelin",
    "alluxio": "Alluxio"
}
FAILURE_FP_LINK = {
    "Hive": "https://mir.cs.illinois.edu/~swang516/confuzz/azure_fp/hive/",
    "HCommon": "https://mir.cs.illinois.edu/~swang516/confuzz/azure_fp/hcommon/",
    "HDFS": "https://mir.cs.illinois.edu/~swang516/confuzz/azure_fp/hdfs/",
    "HBase": "https://mir.cs.illinois.edu/~swang516/confuzz/azure_fp/hbase/",
    "MapReduce": "https://mir.cs.illinois.edu/~swang516/confuzz/azure_fp/mapreduce/",
    "Yarn": "https://mir.cs.illinois.edu/~swang516/confuzz/azure_fp/yarn/",
    "Flink": "https://mir.cs.illinois.edu/~swang516/confuzz/azure_fp/flink/",
    "Kylin": "https://mir.cs.illinois.edu/~swang516/confuzz/azure_fp/kylin/",
    "Zeppelin": "https://mir.cs.illinois.edu/~swang516/confuzz/azure_fp/zeppelin/",
    "Alluxio": "https://mir.cs.illinois.edu/~swang516/confuzz/azure_fp/alluxio/"
    }
COLOR_MAPPING=MODE_COLOR = {
        "confuzz": "orange",
        "confuzz-": "blue",
        "random": "green",
        "+parameter": "red",
        "+default": "purple",
        "+prefix": "grey"
        }
ROUNDS: Dict[str, List[str]] = {
        'confuzz': ['7253', '7290', '7291', '7311', '7314'],
        'confuzz-': ['7292', '7294', '7301', '7312', '7315'],
        'random': ['7293', '7295', '7302', '7313', '7316'],
        '+parameter': ['7271'], 
        '+default': ['7272'],
        '+prefix': ['9122']
        }
ALL_ROUNDS:List[str] = sum(ROUNDS.values(), [])
FUZZING_MODE=["confuzz", "confuzz-", "random"]
ABLATION_MODE=["confuzz", "+prefix", "+parameter", "+default"]
DUMP_LIMIT=20

config = ConfigParser()
config.read('config.ini')
BUG_FILE:Path = Path(config['DEFAULT']['bugFile'])
DATA_DIR:Path = Path(config['DEFAULT']['dataDir'])
RESULT_DIR:Path = Path(config['DEFAULT']['resultDir'])
COV_DATA_DIR:Path = Path(config['DEFAULT']['covDir'])
PREV_RESULT_DIR:Path = Path(config['DEFAULT']['prevResultDir'])
PREV_COMPRESSED_RESULT_DIR:Path = Path(config['DEFAULT']['prevCompressedResultDir'])
FP_CONFIG_DIR:Path = Path(config['DEFAULT']['fpConfigDir'])
FP_RESULT_DIR:Path = Path(config['DEFAULT']['fpResultDir'])
FIGURE_DIR:Path = Path(config['DEFAULT']['figureDir'])
