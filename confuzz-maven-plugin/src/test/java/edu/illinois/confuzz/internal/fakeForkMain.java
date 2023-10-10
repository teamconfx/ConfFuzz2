package edu.illinois.confuzz.internal;

import org.junit.internal.TextListener;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;

import java.util.ArrayList;

public class fakeForkMain {

    public static void main(String[] args) {
        try {
            String testClassName = TargetTest.class.getName();
            Class<?> testClass = Class.forName(testClassName, true, fakeForkMain.class.getClassLoader());
            getTestClassAndMethod(testClass);
            String testClassName2 = JVMLauncherHelper.class.getName();
            Class<?> testClass2 = Class.forName(testClassName2, true, fakeForkMain.class.getClassLoader());
            getTestClassAndMethod(testClass2);
            String testClassName3 = NotReproduceTest.class.getName();
            Class<?> testClass3 = Class.forName(testClassName3, true, fakeForkMain.class.getClassLoader());
            getTestClassAndMethod(testClass3);
            String testClassName4 = ExtensionTest.class.getName();
            Class<?> testClass4 = Class.forName(testClassName4, true, fakeForkMain.class.getClassLoader());
            getTestClassAndMethod(testClass4);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            System.exit(-1);
        }
    }

    private static void getTestClassAndMethod(Class<?> testClass) {
        try {
            JUnitCore junit = new JUnitCore();
            junit.addListener(new TextListener(System.out));
            // Create a JUnit Request
            Request request = Request.aClass(testClass);
            ArrayList<Description> des =  request.getRunner().getDescription().getChildren();
            // print des

            for (Description d : des) {
                if (!d.getDisplayName().contains("initializationError") && d.isTest()) {
                    System.out.println(d.testCount() + " - " + d.getClassName() + "#" + d.getMethodName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
