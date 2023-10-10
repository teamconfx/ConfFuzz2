repo=hive
sha=5e46e80bc7d059093aece81e3886ba5ee425ee95
module=ql
test="org.apache.hadoop.hive.ql.exec.vector.mapjoin.TestMapJoinOperator#testMultiKey2"
file="target/classes/hive-site.xml"
name="hive.mapjoin.optimized.hashtable.wbsize"
value=0

# inject
inject() {
    echo "<configuration><property><name>$name</name><value>$value</value></property></configuration>" > $file
}

# 1. Clone the repo and checkout
# 2. Build module and setup configuration
# 3. Run test
git clone git@github.com:apache/$repo \
    && cd $repo && git checkout $sha \
    && mvn install -DskipTests -pl $module -am \
    && cd $module && inject \
    && mvn surefire:test -Dtest=$test
