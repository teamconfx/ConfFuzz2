package edu.illinois.confuzz.internal.types;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.illinois.confuzz.internal.ConfigTracker;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class DefaultConf extends AbstractConfigType {
    private final Object defaultVal;
    private final List<Object> values;
    public DefaultConf(String name, Object val) {
        this.defaultVal = val;
        this.values = new LinkedList<>(ConfigTracker.getDefaultValues(name));
        if (! this.values.contains(val)) {
            this.values.add(val);
        }
    }

    @Override
    public Object randomValue(Object value, List<Byte> bytes) {
        return values.get(toInteger(bytes, 0, values.size()));
    }

    @Override
    public int byteNum() {
        return 4;
    }

    @Override
    public Object getDefault() {
        return this.defaultVal;
    }

    public int valuesSize() {
        return this.values.size();
    }

    public static boolean check(String name, Object val) {
        return true;
    }
}
