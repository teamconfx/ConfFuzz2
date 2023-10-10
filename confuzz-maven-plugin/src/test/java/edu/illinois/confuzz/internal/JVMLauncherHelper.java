package edu.illinois.confuzz.internal;

import edu.neu.ccs.prl.meringue.JvmLauncher;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JVMLauncherHelper {
    public static JvmLauncher setupJVM(String testClass, String testMethod) {
        String javaHome = System.getProperty("java.home") + "/bin/java";
        File javaExecutable = new File(javaHome);
        List<String> javaOptions = new ArrayList();
        javaOptions.add("-Dclass=" + testClass);
        javaOptions.add("-Dmethod=" + testMethod);
        javaOptions.add("-cp");
        String classPath = System.getProperty("java.class.path");
        javaOptions.add(classPath);
        String DEBUG = System.getProperty("binary.debug");
        if (DEBUG != null && DEBUG.toLowerCase().equals("true")) {
            javaOptions.add(JvmLauncher.DEBUG_OPT + "5005");
        }
        String [] arguments = {testClass, testMethod, System.getProperty("user.dir").toString()};
        return JvmLauncher.fromMain(javaExecutable, DebugForkMain.class.getName(), javaOptions.toArray(new String[0]), true, arguments, null, new HashMap<>());
    }
}
