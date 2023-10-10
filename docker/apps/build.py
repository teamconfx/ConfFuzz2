import os
import sys
import build_config

ROOT_DIR = "."

def build_app(app_name):
    if app_name == "fuzz-hbase":
        return
    app_dir = os.path.join(ROOT_DIR, app_name)
    values = build_config.build_modules[app_name]
    branch = values[0]
    modules = values[1]
    module_str = ",".join(modules)
    mvn_cmd = "mvn -B clean install -DskipTests -am -pl {} -Drat.skip -Denforcer.skip -Dcheckstyle.skip".format(module_str)
    build_cmd = "cd {} && git checkout {} && {}".format(app_dir, branch, mvn_cmd)
    print("Confuzz-Building " + app_name + "=======================================")
    print(build_cmd)
    os.system(build_cmd)


def build_hadoop_for_hbase():
    app_name = "fuzz-hadoop"
    app_dir = os.path.join(ROOT_DIR, app_name)
    branch = "confuzz-hbase"
    module = 'hadoop-common-project/hadoop-common'
    mvn_cmd = "mvn -B clean install -DskipTests -am -pl {}".format(module)
    build_cmd = "cd {} && git checkout {} && {}".format(app_dir, branch, mvn_cmd)
    print("Confuzz-Building HCommon for HBase =======================================")
    print(build_cmd)
    os.system(build_cmd)


def build_hbase():
    build_hadoop_for_hbase()
    app_name = "fuzz-hbase"
    app_dir = os.path.join(ROOT_DIR, app_name)
    values = build_config.build_modules[app_name]
    branch = values[0]
    modules = values[1]
    build_cmd = "cd {} && git checkout {} && cd {} && mvn -B clean install -DskipTests && cd - && cd {} && mvn -B clean install -DskipTests".format(app_dir, branch, modules[0], modules[1])
    print("Confuzz-Building " + app_name + "=======================================")
    print(build_cmd)
    os.system(build_cmd)


def build():
    apps = build_config.build_modules.keys()
    # just directly build hbase in case later fuzz-hadoop is still in confuzz-hbase branch
    build_hbase()
    for app in apps:
        build_app(app)

build()
