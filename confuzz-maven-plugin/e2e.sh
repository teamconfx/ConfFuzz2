# This script is used to launch the end-to-end tests for the confuzz-maven-plugin.

TEST_LIST="testPollutedTest newFailureFromBinarySearchTest setAfterGetTest beforeAfterMethodTest beforeAfterClassTest"
OUTPUT_DIR="target/meringue/edu.illinois.fakefuzz.TestDebug"

# Run the tests
#for test in $TEST_LIST
#do
#    echo "Running test: $test"
#    bash fuzz.sh $test
#    bash debug.sh $test
#done

# Check the results
#python3 inspect-e2e.py > inspect-e2e.log

# if inspect-e2e.log does not contain string CONFUZZ-ERROR, then the tests passed
#if grep -q "CONFUZZ-ERROR" inspect-e2e.log;
#then
#    echo "End-to-end tests failed. See inspect-e2e.log for details."
#    exit 1
#else
#    echo "End-to-end tests passed."
#    exit 0
#fi

