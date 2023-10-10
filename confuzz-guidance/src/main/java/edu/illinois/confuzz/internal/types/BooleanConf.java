package edu.illinois.confuzz.internal.types;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import java.util.List;

public class BooleanConf extends AbstractConfigType {
    private final boolean defaultVal;
    private final boolean isString;

    public BooleanConf(String name, Object val) {
        if (val instanceof Boolean) {
            this.defaultVal = (boolean) val;
            this.isString = false;
        } else {
            this.defaultVal = Boolean.parseBoolean((String) val);
            this.isString = true;
        }
    }

    @Override
    public Object randomValue(Object value, List<Byte> bytes) {
        boolean ret = toBoolean(bytes.get(0));
        if (isString) {
            return String.valueOf(ret);
        }
        return ret;
    }

    @Override
    public int byteNum() {
        return 1;
    }

    @Override
    public Object getDefault() {
        if (isString) {
            return String.valueOf(defaultVal);
        }
        return defaultVal;
    }

    public static boolean check(String name, Object val) {
        return (val instanceof Boolean) || ((val instanceof String) && isBoolean((String) val));
    }

    private static boolean isBoolean(String value) {
        String trimStr = value.toLowerCase().trim();
        if (trimStr.equals("true") || trimStr.equals("false")) {
            return true;
        }
        return false;
    }
}
