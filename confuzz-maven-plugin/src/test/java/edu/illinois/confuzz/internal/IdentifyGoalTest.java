package edu.illinois.confuzz.internal;

import org.junit.Assert;
import org.junit.Test;

public class IdentifyGoalTest {

    @Test
    public void testA() {
        String s1 = System.getProperty("test.prefix") + "fake";
        Assert.assertEquals("test-" + "fake", s1);
        ConfigTracker.trackGet("property", "success");
    }

    @Test
    public void testThreeGetParams() {
        ConfigTracker.trackGet("key1", "value1");
        ConfigTracker.trackGet("key2", "value2");
        ConfigTracker.trackGet("key1", "");
        ConfigTracker.trackGet("key3", "value3");
    }

    @Test
    public void testThreeGetOneSetParam() {
        ConfigTracker.trackGet("key1", "");
        ConfigTracker.trackGet("key2", "value20");
        ConfigTracker.trackGet("key3", "value20");
        ConfigTracker.trackGet("key2", "value40");
        ConfigTracker.trackGet("key2", "value20");
    }

    @Test
    public void testGetAfterSet() {
        ConfigTracker.trackGet("key1", "value1");
        ConfigTracker.trackSet("key1", "value2");
    }

    @Test
    public void testNoParam() {
        // Do nothing
    }

    @Test
    public void testOnlySetParams() {
        ConfigTracker.trackSet("key1", "value1");
        ConfigTracker.trackSet("key2", "value2");
    }
}
