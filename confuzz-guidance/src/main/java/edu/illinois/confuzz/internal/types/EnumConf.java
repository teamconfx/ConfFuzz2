package edu.illinois.confuzz.internal.types;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import java.util.List;

public class EnumConf extends AbstractConfigType {
    private final Enum defaultVal;
    private final Object[] values;
    public EnumConf(String name, Enum val) {
        this.defaultVal = val;
        this.values = val.getClass().getEnumConstants();
    }

    @Override
    public Object randomValue(Object value, List<Byte> bytes) {
        return values[toInteger(bytes, 0, values.length)];
    }

    @Override
    public int byteNum() {
        return 4;
    }

    @Override
    public Enum getDefault() {
        return defaultVal;
    }

    public static boolean check(String name, Object val) {
        return val instanceof Enum;
    }
}
