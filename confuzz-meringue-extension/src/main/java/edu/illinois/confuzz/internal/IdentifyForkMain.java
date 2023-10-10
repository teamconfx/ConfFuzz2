package edu.illinois.confuzz.internal;

import edu.neu.ccs.prl.meringue.SystemPropertyUtil;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;

import java.io.IOException;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

public class IdentifyForkMain {

    public static final String PROPERTIES_KEY = "confuzz.properties";
    private static String IDENTIFY_DATA_OBJECT_FILE = ".confuzz_IdentifyData";
    private IdentifyForkMain() {
        throw new AssertionError();
    }

    public static void main(String[] args) {
        try {
            String testClassName = args[0];
            String testMethodName = args[1];
            SystemPropertyUtil.loadSystemProperties(PROPERTIES_KEY);
            Class<?> testClass = Class.forName(testClassName, true, IdentifyForkMain.class.getClassLoader());
            runTest(testClass, testMethodName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            System.exit(-1);
        }
    }

    private static void runTest(Class<?> testClass, String testMethodName) {
        try {
            JUnitCore junit = new JUnitCore();
            //junit.addListener(new TextListener(System.out));
            // Create a JUnit Request
            ConfigTracker.clearConfigMap();
            junit.run(Request.method(testClass, testMethodName));
            // Write the result to a file
            IdentifyData id = new IdentifyData(testClass.getName(), testMethodName, ConfigTracker.getTotalConfigMap());
            saveKeySetObject(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Write the result of the test to a object file
     * @param id
     * @throws IOException
     */
    private static void saveKeySetObject(IdentifyData id) {
        try {
            // Open the file
            FileOutputStream fileOut = new FileOutputStream(IDENTIFY_DATA_OBJECT_FILE);

            // Create the ObjectOutputStream
            ObjectOutputStream out = new ObjectOutputStream(fileOut);

            // Write the object to the file
            out.writeObject(id);

            // Close the ObjectOutputStream and the file output stream
            out.close();
            fileOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
