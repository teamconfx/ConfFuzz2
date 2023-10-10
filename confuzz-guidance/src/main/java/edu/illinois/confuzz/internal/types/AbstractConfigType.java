package edu.illinois.confuzz.internal.types;

import com.pholser.junit.quickcheck.internal.Ranges;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;

import java.util.LinkedList;
import java.util.List;

public abstract class AbstractConfigType implements ConfigType {
    protected Object generated;

    public abstract Object randomValue(Object value, List<Byte> bytes);
    public abstract int byteNum();

    @Override
    public Object generate(Object value, SourceOfRandomness random) {
        List<Byte> bytes = new LinkedList<>();
        for (byte b: random.nextBytes(byteNum())) {
            bytes.add(b);
        }
        Object ret = shiftBytes(bytes) ? null : randomValue(value, bytes);
        this.generated = ret;
        return ret;
    }

    protected boolean shiftBytes(List<Byte> bs) {
        for (int i=bs.size()-1; i >= 0; i--) {
            if (bs.get(i) != 0) {
                bs.set(i, (byte) (bs.get(i) - 1));
                for (int j=i+1;j<bs.size();j++) {
                    assert bs.get(j) == 0;
                    bs.set(j, (byte) 255);
                }
                return false;
            }
        }
        return true;
    }

    protected boolean toBoolean(byte b) {
        return b % 2 == 1;
    }

    protected int toInteger(List<Byte> bs, int size) {
        assert bs.size() >= size && size <= 4;
        int ret = 0;
        for (int i = 0; i < size; i++) {
            ret = ret << 8;
            ret += bs.remove(0);
        }
        return ret;
    }

    protected int toInteger(List<Byte> bs) {
        return toInteger(bs, 4);
    }

    // this function only works with integer or there would be overflow
    protected int toInteger(List<Byte> bs, long lower, long upper) {
        Math.addExact(lower, 0);
        if (lower == Integer.MIN_VALUE && upper == Integer.MAX_VALUE) {
            return toInteger(bs);
        }
        long range = Math.subtractExact(upper, lower);
        long ret = ((long) toInteger(bs)) % range;
        if (ret < 0) {
            ret = Math.addExact(ret, range);
        }
        System.out.println(ret + "+" + lower);
        return Math.toIntExact(Math.addExact(ret, lower));
    }

    protected float toFloat(List<Byte> bs) {
        assert bs.size() >= 3;
        int ret = toInteger(bs, 3);
        return ret / ((float) (1 << 24));
    }

    @Override
    public Object getGenerated() {
        return generated;
    }
}
