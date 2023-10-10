package edu.illinois.confuzz.internal;

import edu.illinois.confuzz.DebugUtil;
import edu.neu.ccs.prl.meringue.JvmLauncher;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class NotReproduceTest {
    @Test @Ignore
    public void testGuidanceException() throws IOException, ParserConfigurationException, InterruptedException, TransformerException {
        File injectConfigFile = getConfigFile("inject.cfg");
        JvmLauncher launcher = JVMLauncherHelper.setupJVM(TargetTest.class.getCanonicalName(), "noExceptionTargetTest");
        StackTraceElement[] trace = new StackTraceElement[1];
        //trace[0] = new StackTraceElement("", "", "", 1);
        Throwable expectedFailure = new ReplayDiffException("");
        expectedFailure.setStackTrace(trace);
        DebugUtil debugUtil = new DebugUtil(launcher, injectConfigFile, new SystemStreamLog(), new Failure(expectedFailure),
                ConfigUtils.getConfigMapsFromJSON(getConfigFile("one.json")).get(1), new File("."),
                TargetTest.class.getName(), "noExceptionTargetTest");
        Map<String, String> buggyConfig = debugUtil.searchBuggyConfig(new LinkedList<>(), new HashMap<>());

        Assert.assertEquals(1, buggyConfig.size());
        Assert.assertTrue(buggyConfig.containsKey("REPLAY_DIFF_EXCEPTION"));
        Assert.assertEquals("true", buggyConfig.get("REPLAY_DIFF_EXCEPTION"));
    }

    public class FakeException extends Exception {}

    @Test
    public void testNotReproducedFailure() throws IOException,
            ParserConfigurationException, InterruptedException, TransformerException {
        File injectConfigFile = getConfigFile("inject.cfg");
        JvmLauncher launcher = JVMLauncherHelper.setupJVM(
                TargetTest.class.getCanonicalName(), "noExceptionTargetTest");
        StackTraceElement[] trace = new StackTraceElement[1];
        trace[0] = new StackTraceElement("", "", "", 1);
        // This exception is faked and can't be reproduced
        Throwable expectedFailure = new FakeException();
        expectedFailure.setStackTrace(trace);
        DebugUtil debugUtil = new DebugUtil(launcher, injectConfigFile, new SystemStreamLog(), new Failure(expectedFailure),
                ConfigUtils.getConfigMapsFromJSON(getConfigFile("one.json")).get(1), new File("."),
                TargetTest.class.getName(), "noExceptionTargetTest");
        try {
            Map<String, String> buggyConfig = debugUtil.searchBuggyConfig(new LinkedList<>(), new HashMap<>());
            throw new AssertionError("binarySearch should throw AssertionError!");
        } catch (AssertionError e) {
            // test passes
        }
    }

    private File getConfigFile(String fileName) {
        ClassLoader classLoader = this.getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        return file;
    }
}
