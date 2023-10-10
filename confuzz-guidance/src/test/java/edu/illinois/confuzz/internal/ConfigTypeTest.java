package edu.illinois.confuzz.internal;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.illinois.confuzz.internal.types.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeoutException;

public class ConfigTypeTest {
    private static final long[] defaultPartitions = {-1, 1024, Short.MAX_VALUE, Integer.MAX_VALUE};

    @Before
    public void setUp() throws IOException, TimeoutException {
        System.setProperty("regex.file", "src/test/resources/regex.json");
        ConfigConstraints.init();
    }

    @Test
    public void testGenPartition() {
        Assert.assertArrayEquals(IntegerConf.genPartition(-2, 0), new int[]{-2, -1, 0});
        Assert.assertArrayEquals(IntegerConf.genPartition(Short.MAX_VALUE - 1,  Short.MAX_VALUE + 1),
                new int[]{Short.MAX_VALUE - 1, Short.MAX_VALUE, Short.MAX_VALUE + 1});
        Assert.assertArrayEquals(IntegerConf.genPartition(-3, -2), new int[]{-3, -2});
        Assert.assertArrayEquals(IntegerConf.genPartition(0, 3), new int[]{0, 3});
        Assert.assertArrayEquals(IntegerConf.genPartition(1023, 1025), new int[]{1023, 1024, 1025});
    }

    @Test
    public void testConfigTypes() {
        String fakeBoolean = "FAKE";
        Assert.assertTrue(ConfigTypes.getType("testRegex", fakeBoolean) instanceof RegexConf);
        Assert.assertFalse(ConfigTypes.getType("fake", fakeBoolean) instanceof RegexConf);

        int intInt = 1;
        Integer IntInt = 1;
        Assert.assertTrue(ConfigTypes.getType(null, intInt) instanceof IntegerConf);
        Assert.assertTrue(ConfigTypes.getType(null, IntInt) instanceof IntegerConf);

        String dataSize = "1KB";
        String dataSize2 = "1mb";
        Assert.assertTrue(ConfigTypes.getType(null, dataSize) instanceof DataSizeConf);
        Assert.assertTrue(ConfigTypes.getType(null, dataSize2) instanceof DataSizeConf);

        String durationSize = "1s";
        String durationSize2 = "1H";
        Assert.assertTrue(ConfigTypes.getType(null, durationSize) instanceof DurationConf);
        Assert.assertTrue(ConfigTypes.getType(null, durationSize2) instanceof DurationConf);

        Float floatFloat = 1.0f;
        Double doubleDouble = 1.0;
        Assert.assertTrue(ConfigTypes.getType(null, floatFloat) instanceof FloatConf);
        Assert.assertTrue(ConfigTypes.getType(null, doubleDouble) instanceof FloatConf);

        String stringInt = "1";
        Assert.assertTrue(ConfigTypes.getType(null, stringInt) instanceof IntegerConf);
        String stringFloat = "1.0";
        Assert.assertTrue(ConfigTypes.getType(null, stringFloat) instanceof FloatConf);
        String stringBoolean = "true";
        Assert.assertTrue(ConfigTypes.getType(null, stringBoolean) instanceof BooleanConf);
    }

    @Test
    public void testBooleanConf() {
        Boolean BooleanTrue = Boolean.TRUE;
        Boolean BooleanFalse = Boolean.FALSE;
        boolean booleanTrue = true;
        boolean booleanFalse = false;

        // 1. check type is BooleanConf
        Assert.assertTrue(ConfigTypes.getType(null, BooleanTrue) instanceof BooleanConf);
        Assert.assertTrue(ConfigTypes.getType(null, BooleanFalse) instanceof BooleanConf);
        Assert.assertTrue(ConfigTypes.getType(null, booleanTrue) instanceof BooleanConf);
        Assert.assertTrue(ConfigTypes.getType(null, booleanFalse) instanceof BooleanConf);

        // 2. check generate() returns a boolean
        BooleanConf BooleanTrueConf = new BooleanConf(null, BooleanTrue);
        BooleanConf BooleanFalseConf = new BooleanConf(null, BooleanFalse);
        BooleanConf booleanTrueConf = new BooleanConf(null, booleanTrue);
        BooleanConf booleanFalseConf = new BooleanConf(null, booleanFalse);

        Assert.assertTrue(isBoolean(BooleanTrueConf.generate(null, new SourceOfRandomness(new Random()))));
        Assert.assertTrue(isBoolean(BooleanFalseConf.generate(null, new SourceOfRandomness(new Random()))));
        Assert.assertTrue(isBoolean(booleanTrueConf.generate(null, new SourceOfRandomness(new Random()))));
        Assert.assertTrue(isBoolean(booleanFalseConf.generate(null, new SourceOfRandomness(new Random()))));
    }

    @Test
    public void testDataSize() {
        String dataSize = "1KB";
        String dataSize2 = "1mb";

        // 1. check type is DataSizeConf
        Assert.assertTrue(ConfigTypes.getType(null, dataSize) instanceof DataSizeConf);
        Assert.assertTrue(ConfigTypes.getType(null, dataSize2) instanceof DataSizeConf);

        // 2. check generate() returns matching data size regex pattern
        DataSizeConf dataSizeConf = new DataSizeConf(null, dataSize);
        DataSizeConf dataSizeConf2 = new DataSizeConf(null, dataSize2);
        String value1 = dataSizeConf.generate(null, new SourceOfRandomness(new Random())).toString();
        String value2 = dataSizeConf2.generate(null, new SourceOfRandomness(new Random())).toString();
        Assert.assertTrue(value1.matches(DataSizeConf.getDataSizeRegex()));
        Assert.assertTrue(value2.matches(DataSizeConf.getDataSizeRegex()));
    }

    @Test
    public void testDefault() {
        String defaultStr = "default";
        String defaultStr2 = "DEFAULT";

        // 1. check type is DefaultConf
        Assert.assertTrue(ConfigTypes.getType(null, defaultStr) instanceof DefaultConf);

        // 2. check generate() returns default value
        DefaultConf defaultConf = new DefaultConf(null, defaultStr);
        DefaultConf defaultConf2 = new DefaultConf(null, defaultStr2);
        Assert.assertEquals(defaultConf.generate(null, new SourceOfRandomness(new Random())).toString(), defaultStr);
        Assert.assertEquals(defaultConf2.generate(null, new SourceOfRandomness(new Random())).toString(), defaultStr2);
    }


    @Test
    public void testDuration() {
        String[] durations = {"1s", "1H", "1sec"};

        for (int i=0;i<durations.length;i++) {
            String duration = durations[i];
            // 1. check type is DurationConf
            ConfigType type = ConfigTypes.getType(null, duration);
            Assert.assertTrue(type instanceof DurationConf);

            // 2. check generate() returns matching duration regex pattern
            DurationConf configType = (DurationConf) type;
            String value = configType.generate(null, new SourceOfRandomness(new Random())).toString();
            Assert.assertTrue(value.matches(configType.getDurationRegex()));

        }
    }

    @Test
    public void testFloat() {
        float floatFloat = 1.0f;
        double doubleDouble = 1.0;
        Float FloatFloat = 1.0f;
        Double DoubleDouble = 1.0;
        String stringFloat = "1.0f";

        // 1. check type is FloatConf
        Assert.assertTrue(ConfigTypes.getType(null, floatFloat) instanceof FloatConf);
        Assert.assertTrue(ConfigTypes.getType(null, doubleDouble) instanceof FloatConf);
        Assert.assertTrue(ConfigTypes.getType(null, FloatFloat) instanceof FloatConf);
        Assert.assertTrue(ConfigTypes.getType(null, DoubleDouble) instanceof FloatConf);
        Assert.assertTrue(ConfigTypes.getType(null, stringFloat) instanceof FloatConf);

        // 2. check generate() returns a Double
        FloatConf floatConf = new FloatConf(null, floatFloat);
        FloatConf doubleConf = new FloatConf(null, doubleDouble);
        FloatConf FloatConf = new FloatConf(null, FloatFloat);
        FloatConf DoubleConf = new FloatConf(null, DoubleDouble);
        FloatConf stringConf = new FloatConf(null, stringFloat);

        Assert.assertTrue(floatConf.generate(null, new SourceOfRandomness(new Random())) instanceof Double);
        Assert.assertTrue(doubleConf.generate(null, new SourceOfRandomness(new Random())) instanceof Double);
        Assert.assertTrue(FloatConf.generate(null, new SourceOfRandomness(new Random())) instanceof Double);
        Assert.assertTrue(DoubleConf.generate(null, new SourceOfRandomness(new Random())) instanceof Double);
        Assert.assertTrue(stringConf.generate(null, new SourceOfRandomness(new Random())) instanceof String);
    }

    @Test
    public void testInteger() {
        int intInt = 1;
        long longLong = 1L;
        Integer IntegerInt = 1;
        Long LongLong = 1L;
        String stringInt = "1";

        // 1. check type is IntegerConf
        Assert.assertTrue(ConfigTypes.getType(null, intInt) instanceof IntegerConf);
        Assert.assertTrue(ConfigTypes.getType(null, longLong) instanceof IntegerConf);
        Assert.assertTrue(ConfigTypes.getType(null, IntegerInt) instanceof IntegerConf);
        Assert.assertTrue(ConfigTypes.getType(null, LongLong) instanceof IntegerConf);
        Assert.assertTrue(ConfigTypes.getType(null, stringInt) instanceof IntegerConf);

        // 2. check generate() returns a Long
        IntegerConf intConf = new IntegerConf(null, intInt);
        IntegerConf longConf = new IntegerConf(null, longLong);
        IntegerConf IntegerConf = new IntegerConf(null, IntegerInt);
        IntegerConf LongConf = new IntegerConf(null, LongLong);
        IntegerConf stringConf = new IntegerConf(null, stringInt);

        Assert.assertTrue(intConf.generate(null, new SourceOfRandomness(new Random())) instanceof Integer);
        Assert.assertTrue(longConf.generate(null, new SourceOfRandomness(new Random())) instanceof Integer);
        Assert.assertTrue(IntegerConf.generate(null, new SourceOfRandomness(new Random())) instanceof Integer);
        Assert.assertTrue(LongConf.generate(null, new SourceOfRandomness(new Random())) instanceof Integer);
        Assert.assertTrue(stringConf.generate(null, new SourceOfRandomness(new Random())) instanceof String);
    }

    @Test
    public void testRegex() {
        String regex = "FAKE";
        String regex2 = "456";

        // 1. check type is RegexConf
        Assert.assertTrue(ConfigTypes.getType("testRegex", regex) instanceof RegexConf);
        Assert.assertFalse(ConfigTypes.getType(null, regex2) instanceof RegexConf);

        // 2. check generate() returns matching regex pattern
        RegexConf regexConf = new RegexConf("testRegex", regex);
        String value = regexConf.generate(null, new SourceOfRandomness(new Random())).toString();
        Assert.assertTrue(value.matches(regexConf.getRegex()));
    }

    @Test
    public void testList() {
        String[] validLists = {"a,b,c", "a1;b2;c3;d5", "a-1;b-324;1-32_c", "foaf;afoiawhe;fweofi;jwoafih;f;a;b;c;d"};
        String[] invalidLists = {"a,b", "a=1;b=2;;", ""};
        for (int i=0;i<validLists.length;i++) {
            // 1. check the valid is ListConf
            ConfigType type = ConfigTypes.getType(null, validLists[i]);
            Assert.assertTrue(type instanceof ListConf);

            // 2. check generate() returns valid value
            ListConf listConf = (ListConf) type;
            String val = listConf.generate(null, new SourceOfRandomness(new Random())).toString();
            Assert.assertNotNull(val, ListConf.check(val, listConf.getDelimiterIdx(), false));
        }
        for (int i=0;i<invalidLists.length;i++) {
            // 3. check invalid is not ListConf
            Assert.assertFalse(ConfigTypes.getType(null, invalidLists[i]) instanceof ListConf);
        }
    }

    // ==================== Helper ====================
    private boolean isBoolean(Object o) {
        if (o == null || (o instanceof Boolean)) {
            return true;
        }
        if (!(o instanceof String)) {
            return false;
        }
        String trimStr = ((String)o).toLowerCase().trim();
        if (trimStr.equals("true") || trimStr.equals("false")) {
            return true;
        }
        return false;
    }


}
