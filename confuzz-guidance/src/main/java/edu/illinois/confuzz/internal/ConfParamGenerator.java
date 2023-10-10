package edu.illinois.confuzz.internal;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import java.util.*;
import java.util.stream.Collectors;

import edu.illinois.confuzz.internal.types.ConfigType;
import edu.illinois.confuzz.internal.types.ConfigTypes;
import edu.illinois.confuzz.internal.types.DefaultConf;

public class ConfParamGenerator {

    private static final Map<String, ConfigType> configTypeMap = new HashMap<>();

    private static String curGenConfParam = "";

    public synchronized static void setCurGenConfParam(String param) {
        curGenConfParam = param;
    }

    public synchronized static String getCurGenConfParam() {
        return curGenConfParam;
    }

    /**
     * Return a random value based on the type of @param value
     */
    public static Object generate(String name, Object value, SourceOfRandomness random) {
        if (!configTypeMap.containsKey(name)) {
            configTypeMap.put(name, ConfigTypes.getType(name, value));
        }
        ConfParamGenerator.setCurGenConfParam(name);
        return configTypeMap.get(name).generate(value, random);
    }

    public static Object generate(String name, SourceOfRandomness random) {
        return generate(name, ConfigTracker.getTotalConfigMap().getOrDefault(name, null), random);
    }

    private static Set<String> dullConfigs = null;

    public static Set<String> getDullConfigs() {
        if (dullConfigs == null) {
            dullConfigs = configTypeMap.entrySet().stream()
                    .filter(e -> (e.getValue() instanceof DefaultConf) &&
                            ((DefaultConf) e.getValue()).valuesSize() <= 1)
                    .map(Map.Entry::getKey).collect(Collectors.toSet());
        }
        LogUtils.println("Dull configs: " + dullConfigs);
        return dullConfigs;
    }

    public static ConfigType register(String name, Object value) {
        ConfigType type = ConfigTypes.getType(name, value);
        configTypeMap.put(name, type);
        if ((type instanceof DefaultConf) && ((DefaultConf) type).valuesSize() <= 1) {
            getDullConfigs().add(name);
        }
        return type;
    }

    public static boolean observedBefore(String name) {
        return configTypeMap.containsKey(name);
    }

    /**
     * Get the map of all generated values
     * @return the map from String to Object for all the configs
     */
    public static Map<String, Object> getGeneratedMap() {
        Map<String, Object> ret = new HashMap<>();
        for (Map.Entry<String, ConfigType> entry: configTypeMap.entrySet()) {
            ret.put(entry.getKey(), entry.getValue().getGenerated());
        }
        return ret;
    }

    /** Helper Functions */

    public static boolean isNullOrEmpty(Object value) {
        if (value instanceof String) {
            return ((String) value).isEmpty() || Objects.equals(value, "null");
        }
        return value == null || Objects.equals(value, "null");
    }

    /** For Internal test */
    public static void printListMap(Map<String, List<String>> map) {
        LogUtils.println("In printing!!");
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            String name = entry.getKey();
            String value = "";
            for (String s : entry.getValue()) {
                value = value + ";" + s;
            }
            LogUtils.println(name + "=" + value);
        }
    }

    public static void printMap(Map<String, String> map) {
        LogUtils.println("In printing!!");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            LogUtils.println(name + "=" + value);
        }
        LogUtils.println("Number of map size = " + map.size());
    }

    public static void debugPrint(String str) {
        if (false) {
            LogUtils.println(str);
        }
    }

}
