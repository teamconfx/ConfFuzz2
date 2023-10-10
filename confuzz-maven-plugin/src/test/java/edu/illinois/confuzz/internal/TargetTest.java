package edu.illinois.confuzz.internal;

import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class TargetTest {
    private static final String FAKE_FILE = System.getProperty("user.dir") + File.separator + "target/temp/fake.txt";
    @Test
    public void testFakeJvmLauncher() throws IOException {
        // Create directory of fake.txt
        File dir = new File("target/temp");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        BufferedWriter bw = new BufferedWriter(new FileWriter(FAKE_FILE));
        bw.write("fake");
        bw.close();
    }

    @Test
    public void testOneBugConfiguration() throws IOException {
        File configFile = MojoTestUtil.getConfigFile("inject.cfg");
        Map<String, String> config = ConfigUtils.getConfig(configFile, "=");
        if (config.containsKey("bug")) {
            if (config.get("bug").equals("true")) {
                throw new IllegalArgumentException();
            }
        }
    }

    @Test
    public void testTwoBugConfiguration() throws IOException {
        File configFile = MojoTestUtil.getConfigFile("inject.cfg");
        Map<String, String> config = ConfigUtils.getConfig(configFile, "=");
        if (config.containsKey("bug1") && config.containsKey("bug2")) {
            if (config.get("bug1").equals("true") && config.get("bug2").equals("true")) {
                throw new IllegalArgumentException();
            }
        }
    }

    @Test
    public void testDifferentBugsConfiguration() throws IOException {
        File configFile = MojoTestUtil.getConfigFile("inject.cfg");
        Map<String, String> config = ConfigUtils.getConfig(configFile, "=");
        if (config.containsKey("bug1")) {
            throw new IOException();
        }
        if (config.containsKey("bug2")) {
            throw new IOException();
        }
    }

    @Test
    public void noExceptionTargetTest() {
        // Do nothing
    }
}
