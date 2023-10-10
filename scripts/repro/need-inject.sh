repo=hadoop
sha=""
module=hadoop-common-project/hadoop-common
test=""
file="target/test-classes/core-site.xml"
name=""
value=""

# inject the configs
inject() {
    sed -i -e "/<\/configuration>/i\\
    <property><name>$name</name><value>$value</value></property>
    " "$file"
}

# 1. Clone the repo and checkout
# 2. Build module and setup configuration
# 3. Run test
git clone git@github.com:apache/$repo \
    && cd $repo && git checkout $sha \
    && mvn install -DskipTests -pl $module -am \
    && cd $module && inject \
    && mvn surefire:test -Dtest=$test
