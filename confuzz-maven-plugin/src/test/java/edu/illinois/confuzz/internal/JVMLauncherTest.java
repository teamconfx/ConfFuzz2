package edu.illinois.confuzz.internal;

import edu.neu.ccs.prl.meringue.JvmLauncher;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.concurrent.TimeUnit;

public class JVMLauncherTest {
    private JvmLauncher launcher;
    private static final String FAKE_FILE = System.getProperty("user.dir") + File.separator + "target/temp/fake.txt";
    @Test
    public void test1() throws IOException, InterruptedException {
        launcher = JVMLauncherHelper.setupJVM(TargetTest.class.getCanonicalName(), "testFakeJvmLauncher");
        Process p = launcher.launch();
        boolean notTimeout = p.waitFor(60, TimeUnit.SECONDS);
        Assert.assertTrue(notTimeout);
        Assert.assertEquals(0, p.exitValue());
        BufferedReader br = new BufferedReader(new FileReader(FAKE_FILE));
        String line = br.readLine();
        br.close();
        File file = new File(FAKE_FILE);
        if (file.exists()) {
            file.delete();
        }
        Assert.assertEquals("fake", line.trim());
    }

}
