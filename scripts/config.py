import os
CUR_DIR = os.getcwd()
project_github_url = {
    "hcommon": "https://github.com/shuaiwang516/fuzz-hadoop.git",
    "hdfs": "https://github.com/shuaiwang516/fuzz-hadoop.git"
}

project_github_branch = {
    "hcommon": "confuzz",
    "hdfs": "confuzz"
}

project_root_path = {
    "hcommon": f"{CUR_DIR}/fuzz-hadoop/",
    "hdfs": f"{CUR_DIR}/fuzz-hadoop/"
}

project_module = {
    "hcommon": f"{CUR_DIR}/fuzz-hadoop/hadoop-common-project/hadoop-common",
    "hdfs": f"{CUR_DIR}/fuzz-hadoop/hadoop-hdfs-project/hadoop-hdfs"
}