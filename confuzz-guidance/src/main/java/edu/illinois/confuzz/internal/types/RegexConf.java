package edu.illinois.confuzz.internal.types;

import edu.illinois.confuzz.internal.ConfigConstraints;
import edu.illinois.confuzz.internal.LogUtils;
import edu.illinois.confuzz.internal.RegexGenerator;

import java.util.List;

public class RegexConf extends AbstractConfigType {
    private final String defaultVal;
    private final String regex;

    public RegexConf(String name, String val) {
        this.defaultVal = val;
        this.regex = ConfigConstraints.getRegex(name);
        if (this.regex == null) {
            // TODO: consider automatically generate regex
            LogUtils.println("No regex for " + name + " !!!");
        }
    }

    @Override
    public String randomValue(Object value, List<Byte> bytes) {
        int seed = toInteger(bytes);
        String ret = RegexGenerator.generate(seed, regex);
        return ret;
    }

    @Override
    public int byteNum() {
        return 4;
    }

    @Override
    public Object getDefault() {
        return defaultVal;
    }

    public static Boolean check(String name, Object val) {
        return ConfigConstraints.hasRegex(name);
    }

    public String getRegex() {
        return this.regex;
    }
}
