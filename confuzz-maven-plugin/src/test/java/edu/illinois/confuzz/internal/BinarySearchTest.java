package edu.illinois.confuzz.internal;

import edu.illinois.confuzz.DebugUtil;
import edu.neu.ccs.prl.meringue.JvmLauncher;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.util.*;

@RunWith(Enclosed.class)
public class BinarySearchTest {
    public static final String className = TargetTest.class.getName();

    public static class BinarySearchOtherTest {
        public static final Throwable expectedFailure = new IOException();
        public static final Throwable newFailure = new IOException();

        static {
            StackTraceElement[] expectedTrace = {new StackTraceElement(TargetTest.class.getName(),
                    "testDifferentBugsConfiguration", "TargetTest.java", 52)};
            expectedFailure.setStackTrace(expectedTrace);

            StackTraceElement[] newTrace = {new StackTraceElement(TargetTest.class.getName(),
                    "testDifferentBugsConfiguration", "TargetTest.java", 55)};
            newFailure.setStackTrace(newTrace);
        }

        private boolean backup;

        @Before
        public void setUp() {
            backup = Boolean.getBoolean("addBinaryDiff");
            System.setProperty("addBinaryDiff", "true");
        }

        @After
        public void cleanUp() {
            System.setProperty("addBinaryDiff", String.valueOf(backup));
        }

        @Test
        public void testNewException()
                throws IOException, ParserConfigurationException, InterruptedException, TransformerException {
            File injectConfigFile = MojoTestUtil.getConfigFile("inject.cfg");
            JvmLauncher launcher = JVMLauncherHelper.setupJVM(className, "testDifferentBugsConfiguration");
            DebugUtil debugUtil = new DebugUtil(launcher, injectConfigFile, new SystemStreamLog(), new Failure(expectedFailure),
                    ConfigUtils.getConfigMapsFromJSON(MojoTestUtil.getConfigFile("different.json")).get(0),
                    new File("."), TargetTest.class.getName(), "testDifferentBugsConfiguration");

            Queue<DebugEntry> debugEntries = new LinkedList<>();
            Map<String, DebugEntry> entryMap = new HashMap();
            Map<String, String> buggyConfig = debugUtil.searchBuggyConfig(debugEntries, entryMap);
            // Assert that the buggy config found is correct
            Assert.assertEquals(1, buggyConfig.size());
            Assert.assertTrue(buggyConfig.containsKey("bug1"));

            // Assert that the new exception is found
            Assert.assertEquals(1, debugEntries.size());
            Assert.assertEquals(1, entryMap.size());
            String identifier = DebugUtil.hashThrowable(newFailure, className);
            Assert.assertTrue(entryMap.containsKey(identifier));
            DebugEntry entry = debugEntries.remove();
            Assert.assertEquals(identifier, DebugUtil.hashThrowable(entry.getFailure(), className));
            Assert.assertEquals(1, entry.getBugConfigs().size());
            Assert.assertEquals(1, entry.getBugConfigs().get(0).size());
            Assert.assertEquals("true", entry.getBugConfigs().get(0).get("bug2"));
        }

        @Test
        public void testExistingException()
                throws IOException, ParserConfigurationException, InterruptedException, TransformerException {
                File injectConfigFile = MojoTestUtil.getConfigFile("inject.cfg");
                JvmLauncher launcher = JVMLauncherHelper.setupJVM(className, "testDifferentBugsConfiguration");
                DebugUtil debugUtil = new DebugUtil(launcher, injectConfigFile, new SystemStreamLog(), new Failure(expectedFailure),
                        ConfigUtils.getConfigMapsFromJSON(MojoTestUtil.getConfigFile("different.json")).get(0),
                        new File("."), TargetTest.class.getName(), "testDifferentBugsConfiguration");

                String identifier = DebugUtil.hashThrowable(newFailure, className);
                Queue<DebugEntry> debugEntries = new LinkedList<>();
                Map<String, DebugEntry> entryMap = new HashMap();
                entryMap.put(identifier, new DebugEntry(newFailure));
                Map<String, String> buggyConfig = debugUtil.searchBuggyConfig(debugEntries, entryMap);
                // Assert that the buggy config found is correct
                Assert.assertEquals(1, buggyConfig.size());
                Assert.assertTrue(buggyConfig.containsKey("bug1"));

                // Assert that the new exception is found but not pushed into the queue
                Assert.assertEquals(0, debugEntries.size());
                Assert.assertEquals(1, entryMap.size());
                DebugEntry entry = entryMap.get(identifier);
                Assert.assertEquals(identifier, DebugUtil.hashThrowable(entry.getFailure(), className));
                Assert.assertEquals(1, entry.getBugConfigs().size());
                Assert.assertEquals(1, entry.getBugConfigs().get(0).size());
                Assert.assertEquals("true", entry.getBugConfigs().get(0).get("bug2"));
        }
    }
    @RunWith(Parameterized.class)
    public static class BinarySearchCorrectnessTest {
        @Parameterized.Parameters(name = "{index}: fib[{0}]={1}")
        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"left.json", "testOneBugConfiguration"},
                    {"left2.json", "testOneBugConfiguration"},
                    {"mid.json", "testOneBugConfiguration"},
                    {"right.json", "testOneBugConfiguration"},
                    {"right2.json", "testOneBugConfiguration"},
                    {"both.json", "testTwoBugConfiguration"},
                    {"both2.json", "testTwoBugConfiguration"},
                    {"one.json", "testOneBugConfiguration"}});
        }

        private String configFile;
        private String targetTest;

        public BinarySearchCorrectnessTest(String configFile, String targetTest) {
            this.configFile = configFile;
            this.targetTest = targetTest;
        }

        @Test
        public void testBinarySearch()
                throws IOException, ParserConfigurationException, InterruptedException, TransformerException {
            File injectConfigFile = MojoTestUtil.getConfigFile("inject.cfg");
            JvmLauncher launcher = JVMLauncherHelper.setupJVM(className, targetTest);
            int lineno = targetTest.equals("testTwoBugConfiguration") ? 42 : 31;
            StackTraceElement[] trace = new StackTraceElement[1];
            trace[0] = new StackTraceElement("edu.illinois.confuzz.internal.TargetTest",
                    targetTest, "TargetTest.java", lineno);
            Throwable expectedFailure = new IllegalArgumentException();
            expectedFailure.setStackTrace(trace);
            DebugUtil debugUtil = new DebugUtil(launcher, injectConfigFile, new SystemStreamLog(), new Failure(expectedFailure),
                    ConfigUtils.getConfigMapsFromJSON(MojoTestUtil.getConfigFile(configFile)).get(1),
                    new File("."), TargetTest.class.getName(), targetTest);

            Map<String, String> buggyConfig = debugUtil.searchBuggyConfig(new LinkedList<>(), new HashMap<>());

            if (configFile.equals("both.json") || configFile.equals("both2.json")) {
                Assert.assertEquals(2, buggyConfig.size());
                Assert.assertTrue(buggyConfig.containsKey("bug1"));
                Assert.assertTrue(buggyConfig.containsKey("bug2"));
                Assert.assertEquals("true", buggyConfig.get("bug1"));
                Assert.assertEquals("true", buggyConfig.get("bug2"));
            } else {
                Assert.assertEquals(1, buggyConfig.size());
                Assert.assertTrue(buggyConfig.containsKey("bug"));
                Assert.assertEquals("true", buggyConfig.get("bug"));
            }
        }
    }
}
