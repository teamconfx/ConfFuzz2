package edu.illinois.fakefuzz;

import org.junit.*;

import java.io.IOException;
import java.util.Objects;

public class TestDebug {
    private static int classCounter = 0;
    private int methodCounter = 0;

    @BeforeClass
    public static void beforeClass() {
        classCounter += 1;
    }

    @AfterClass
    public static void afterClass() {
        classCounter -= 1;
    }

    @Before
    public void beforeMethod() {
        methodCounter += 1;
    }

    @After
    public void afterMethod() {
        methodCounter -= 1;
    }


    /** We expect no failure of this test */
    @Test
    public void beforeAfterClassTest() throws IOException {
        // This test will fail if BeforeClass or AfterClass is not executed
        Configuration conf = new Configuration();
        String lab_name = conf.getStr("lab_name");

        if (classCounter != 1) {
            throw new IllegalArgumentException("BeforeClass or AfterClass is not executed");
        }
    }

    /** We expect no failure of this test */
    @Test
    public void beforeAfterMethodTest() throws IOException {
        // This test will fail if Before or After is not executed
        Configuration conf = new Configuration();
        String lab_name = conf.getStr("lab_name");

        if (methodCounter != 1) {
            throw new IllegalArgumentException("Before or After is not executed");
        }
    }

    /**
     * Confuzz will get "CS" as the value from "lab_name", but it should NOT set it
     * since this value is tracked from a set() call.
      * @throws IOException We expect no failure of this test
     */
    @Test
    public void setAfterGetTest() throws IOException {
        Configuration conf = new Configuration();
        String lab_name = conf.getStr("lab_name");
        String univ = conf.getStr("univ_name");
        if (!lab_name.equals("xlab")) {
            throw new IllegalArgumentException("lab_name should be xlab");
        }
        conf.set("lab_name", "CS");
        lab_name = conf.getStr("lab_name");
        if (!lab_name.equals("CS")) {
            throw new IllegalArgumentException("lab_name should be CS");
        }
    }

    /**
     * This test will fail by its own and in debug goal should show up as polluted test
     */
    @Test
    public void testPollutedTest() throws IOException {
        Configuration conf = new Configuration();
        String lab_name = conf.getStr("lab_name");
        if (!Objects.equals(System.getProperty("confuzz.goal"), null) && !lab_name.equals("POLLUTION")) {
            throw new IllegalArgumentException("This test is expected to have this failure as polluted test");
        }
    }

    /**
     * This test expect to have only one failure, but debug goal will have three failures
     * since the second failure is due to a separation of configuration set in binary debugging
     *
     * The logic is based on the fact that conf map will record failure1 first, then failure2 then failure3.
     * When doing binary search, failure1 will be in the left config and will not cause failure
     * failure2 and failure3 will be in the right config.
     * Failure 2 will occur the right config. While debugging the failure2, failure 3 will be in the right config.
     * In this way debug goal should reveal the two failures from right side, but only failure1 should be found by the fuzzing.
     */
    @Test
    public void newFailureFromBinarySearchTest() throws IOException {
        Configuration conf = new Configuration();
        String failure1 = conf.getStr("failure1");
        String failure2 = conf.getStr("failure2");
        String failure3 = conf.getStr("failure3");
        String goal = System.getProperty("confuzz.goal");
        Boolean isFuzz = Objects.equals(goal, "fuzz");

        if (isFuzz && failure1.equals("failure1")) {
            throw new IOException("failure1 should be failure1");
        } else {
            // Then failure 2 will happen in the first "whole" binary search round
            if (failure2.equals("failure2")) {
                throw new IOException("failure2 should be failure2");
            }
            // Failure 3 only happens when debugging failure 2
            if (failure3.equals("failure3")) {
                throw new IOException("failure3 should be failure3");
            }
        }
    }
}
