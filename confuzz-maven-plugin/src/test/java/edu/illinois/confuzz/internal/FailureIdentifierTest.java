package edu.illinois.confuzz.internal;

import edu.illinois.confuzz.DebugUtil;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Stack;

@RunWith(Parameterized.class)
public class FailureIdentifierTest {

    private static final String className = "foo.bar.TestClass";

    @Parameterized.Parameters
    public static Iterable<Object[]> params() {
        StackTraceElement testTrace = new StackTraceElement(className, "test", "", 1);
        StackTraceElement methodTrace = new StackTraceElement(className, "testa", "", 1);
        StackTraceElement irrelevantTrace1 = new StackTraceElement("foo.bar.Something", "f", "", 1);
        StackTraceElement irrelevantTrace2 = new StackTraceElement("bar.TestClass", "foo", "", 1);

        // throwable with no test name
        // throwable with only test name on the top
        // throwable with test name on the second and the first irrelevant
        // throwable with test name on the third
        // assertion with test name on the second
        return Arrays.asList(genParam(new Throwable(), new StackTraceElement[]{irrelevantTrace1, irrelevantTrace2}, 2),
                genParam(new Throwable(), new StackTraceElement[]{testTrace}, 1),
                genParam(new Throwable(), new StackTraceElement[]{irrelevantTrace1, testTrace}, 1),
                genParam(new Throwable(),
                        new StackTraceElement[]{irrelevantTrace1, methodTrace, testTrace, irrelevantTrace2}, 2),
                genParam(new AssertionError(), new StackTraceElement[]{methodTrace, testTrace}, 2));
    }

    private Throwable failure;
    private String expected;

    public FailureIdentifierTest(Throwable e, String expected) {
        this.failure = e;
        this.expected = expected;
    }

    @Test
    public void testFailureIdentifier() {
        Assert.assertEquals(expected, DebugUtil.hashThrowable(failure, className));
    }

    private static Object[] genParam(Throwable e, StackTraceElement[] stacktrace, int size) {
        e.setStackTrace(stacktrace);
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName()).append('\n');
        for (int i=0; i<size; i++) {
            sb.append(stacktrace[i].toString()).append('\n');
        }
        return new Object[]{e, sb.toString()};
    }

}
