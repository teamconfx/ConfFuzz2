package edu.illinois.confuzz.internal.types;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.illinois.confuzz.internal.ConfigConstraints;

import java.util.Arrays;
import java.util.List;

public class IntegerConf extends AbstractConfigType {

    // all the intervals are (]
    // consider adding 0 to the list
    private static final int[] defaultPartitions = {-1, 1024, Short.MAX_VALUE, Integer.MAX_VALUE};
    private final int defaultVal;
    private final boolean isString;
    private final int[] partitions;

    public IntegerConf(String name, Object val) {
        if (val instanceof String) {
            this.isString = true;
            this.defaultVal = Integer.parseInt((String) val);
        } else {
            this.isString = false;
            this.defaultVal = Integer.parseInt(String.valueOf(val));
        }

        // we must make sure both are not overflow
        int lowerBound = ConfigConstraints.getLowerBound(name, 0);
        int upperBound = ConfigConstraints.getUpperBound(name, Integer.MAX_VALUE);
        this.partitions = genPartition(lowerBound, upperBound);
    }

    public static int[] genPartition(int lowerBound, int upperBound) {
        // construct actual partitions
        // TODO: need to check lowerBound < upperBound
        int idxl = Arrays.binarySearch(defaultPartitions, lowerBound);
        int idxu = Arrays.binarySearch(defaultPartitions, upperBound);
        if (idxl >= 0) {
            idxl += 1;
        } else {
            idxl = - idxl - 1;
        }
        if (idxu >= 0) {
            idxu -= 1;
        } else {
            idxu = - idxu - 2;
        }

        // notice that the leftmost interval contains both endpoints
        int length = 2 + Math.max(idxu - idxl + 1, 0);
        int[] partitions = new int[length];
        partitions[0] = lowerBound;
        partitions[length-1] = upperBound;
        for (int i = idxl; i <= idxu; i++) {
            partitions[i - idxl + 1] = defaultPartitions[i];
        }
        return partitions;
    }

    @Override
    public Object randomValue(Object value, List<Byte> bytes) {
        int intervalIdx = toInteger(bytes, 0, partitions.length - 1);
        int lowerBound = partitions[intervalIdx], upperBound = partitions[intervalIdx+1];
        if (intervalIdx != 0) {
            lowerBound += 1;
        }
        int ret = toInteger(bytes, (long) lowerBound, ((long) upperBound) + 1);
        if (isString) {
            return String.valueOf(ret);
        }
        return ret;
    }

    @Override
    public int byteNum() {
        return 8;
    }

    @Override
    public Object getDefault() {
        if (isString) {
            return String.valueOf(defaultVal);
        }
        return defaultVal;
    }

    public static Boolean check(String name, Object val) {
        return (val instanceof Integer) || (val instanceof Long) || (val instanceof Short) ||
                ((val instanceof String) && isInteger((String) val));
    }

    private static boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }
}
