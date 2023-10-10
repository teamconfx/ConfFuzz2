TEST_CLASS="edu.illinois.fakefuzz.TestDebug"
TEST_METHOD=$1
mvn confuzz:fuzz -Dconfuzz.generator=edu.illinois.fakefuzz.ConfigurationGenerator \
-Dmeringue.testClass=edu.illinois.fakefuzz.TestDebug -Dmeringue.testMethod=${TEST_METHOD} -DregexFile=src/test/resources/regex.json -Dmeringue.duration=PT10S
