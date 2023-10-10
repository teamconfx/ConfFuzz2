package edu.illinois.confuzz.internal.types;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import java.util.List;

public class FloatConf extends AbstractConfigType {

    private final double defaultVal;
    private final boolean isString;

    public FloatConf(String name, Object val) {
        if ((val instanceof Float) || (val instanceof Double)) {
            this.defaultVal = Double.valueOf(val.toString());
            this.isString = false;
        } else {
            this.defaultVal = Double.parseDouble((String) val);
            this.isString = true;
        }
        // TODO: check here the range of val
    }

    @Override
    public Object randomValue(Object value, List<Byte> bytes) {
        double ret = toFloat(bytes);
        if (isString) {
            return String.valueOf(ret);
        }
        return Math.abs(ret);
    }

    @Override
    public int byteNum() {
        return 4;
    }

    @Override
    public Object getDefault() {
        if (isString) {
            return String.valueOf(defaultVal);
        }
        return defaultVal;
    }

    public static Boolean check(String name, Object val) {
        return (val instanceof Float) || (val instanceof Double) || ((val instanceof String) && isFloat((String) val));
    }

    private static boolean isFloat(String value) {
        if (value.contains(".")) {
            try {
                Float.parseFloat(value);
            } catch (NumberFormatException e) {
                return false;
            }
            return true;
        }
        return false;
    }
}
