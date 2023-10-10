package edu.illinois.confuzz.internal.types;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class DataSizeConf extends AbstractConfigType {

    // Consider both uppercase and lowercase suffixes
    private static final String[] dataSizeSuffixes = {"B", "KB", "MB", "GB", "b", "kb", "mb", "gb"};
    private static final String dataSizeRegex = String.format("[0-9]+(%s)", String.join("|", dataSizeSuffixes));
    private final String defaultVal;

    public DataSizeConf(String name, String val) {
        this.defaultVal = val;
    }

    @Override
    public String randomValue(Object value, List<Byte> bytes) {
        return Math.abs(toInteger(bytes)) + dataSizeSuffixes[toInteger(bytes, 0, 8)];
    }

    @Override
    public int byteNum() {
        return 8;
    }

    @Override
    public Object getDefault() {
        return defaultVal;
    }

    public static Boolean check(String name, Object val) {
        if (val instanceof String) {
            return ((String) val).matches(dataSizeRegex);
        }
        return false;
    }

    public static String getDataSizeRegex() {
        return dataSizeRegex;
    }
}
