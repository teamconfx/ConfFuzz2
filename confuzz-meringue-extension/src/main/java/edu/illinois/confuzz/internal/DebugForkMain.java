package edu.illinois.confuzz.internal;

import edu.neu.ccs.prl.meringue.SystemPropertyUtil;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.io.*;
import java.nio.file.Files;
import java.util.List;


public final class DebugForkMain {
    public static final String PROPERTIES_KEY = "confuzz.properties";
    private static final File FAILURE_FILE = new File("confuzz-failure.temp");
    private DebugForkMain() {
        throw new AssertionError();
    }

    public static void main(String[] args) {
        try {
            Utils.setMavenGoal("debug");
            String testClassName = args[0];
            String testMethodName = args[1];
            SystemPropertyUtil.loadSystemProperties(PROPERTIES_KEY);
            Class<?> testClass = Class.forName(testClassName, true, DebugForkMain.class.getClassLoader());
            if (runTestWithConfig(testClass, testMethodName)) {
                System.exit(0);
            } else {
                System.exit(-1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            System.exit(-1);
        }
    }

    private static boolean runTestWithConfig(Class<?> testClass, String testMethodName) {
        try {
            JUnitCore junit = new JUnitCore();
            //junit.addListener(new TextListener(System.out));
            // Create a JUnit Request
            Result res = junit.run(Request.method(testClass, testMethodName));
            // Write the result to a file
            sendBackResults(res);
            // Return true if the test passed
            if (res.wasSuccessful()) {
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Write the result of the test to a temporary file
     * @param result
     * @throws IOException
     */
    private static void sendBackResults(Result result) throws IOException {
        if (FAILURE_FILE.exists()) {
            // throw new IOException("Failure file not properly cleaned!");
            // More robust to just delete the file
            FAILURE_FILE.delete();
        }
        List<Failure> failures = result.getFailures();
        if (!failures.isEmpty()) {
            edu.illinois.confuzz.internal.Failure failure = new edu.illinois.confuzz.internal.Failure(
                    getRootCause(failures.get(0).getException()));
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(FAILURE_FILE.toPath()))) {
                out.writeObject(failure);
            }
        }
    }


    public static Throwable getRootCause(Throwable e) {
        Throwable cause = null;
        Throwable result = e;

        while(null != (cause = result.getCause())  && (result != cause) ) {
            result = cause;
        }
        return result;
    }
}
