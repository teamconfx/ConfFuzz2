package edu.illinois.confuzz.internal.types;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;

import java.util.List;
import java.util.regex.Pattern;

public class DurationConf extends AbstractConfigType {
    private static final String[][] durationSuffixes = {{"ms", "s", "m", "h", "d"},
            {"sec", "min", "hour", "day", "hr"}, {"MS", "S", "M", "H", "D"}};
    private static final String[] durationRegex = {
           String.format("[0-9]+(%s)", String.join("|", durationSuffixes[0])),
           String.format("[0-9]+(%s)", String.join("|", durationSuffixes[1])),
           String.format("[0-9]+(%s)", String.join("|", durationSuffixes[2]))
    };
    private final String defaultVal;
    private final int suffixIdx;

    public DurationConf(String name, String val) {
        this.defaultVal = val;
        for (int i=0;i<durationRegex.length;i++) {
            if (val.matches(durationRegex[i])) {
                this.suffixIdx = i;
                return;
            }
        }
        throw new GuidanceException(String.format("%s is not a duration with value %s!", name, val));
    }

    @Override
    public String randomValue(Object value, List<Byte> bytes) {
        String suffix = null;
        if (suffixIdx == 1) {
            int idx = toInteger(bytes, 0, 10);
            suffix = durationSuffixes[idx / 5][idx % 5];
        } else {
            suffix = durationSuffixes[suffixIdx][toInteger(bytes, 0, 5)];
        }
        return Math.abs(toInteger(bytes)) + suffix;
    }

    @Override
    public int byteNum() {
        return 8;
    }

    @Override
    public String getDefault() {
        return defaultVal;
    }

    public String getDurationRegex() {
        if (suffixIdx == 1) {
            return durationRegex[0] + "|" + durationRegex[1];
        }
        return durationRegex[suffixIdx];
    }

    public static boolean check(String name, Object val) {
        if (val instanceof String) {
            String str = (String) val;
            return str.matches(durationRegex[0])||str.matches(durationRegex[1])||str.matches(durationRegex[2]);
        }
        return false;
    }
}
