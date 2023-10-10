package edu.illinois.confuzz.internal;

import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

public class ConfigTrackerTest {
    private static TestLogger LOG = new TestLogger();

    @Test
    public void testWriteToLog() {
        ConfigTracker.writeToLog(LOG, "Log for %s %d.", "foobar", 1);
        Assert.assertEquals("Log for foobar 1.", LOG.getMessage());
    }
}
