package edu.illinois.confuzz.internal;

import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.illinois.confuzz.internal.types.ConfigTypes;

import java.lang.reflect.Method;
import java.util.*;

/**
 * The bookkeeper class for tracking the configs throughout the fuzzing.
 */
public class ConfigTracker {

    private static final boolean isLogEnabled = Boolean.getBoolean("ctest.log");
    /** configMap records the exercised configuration parameter by the current fuzzed test */
    private static Map<String, Object> totalConfigMap = new LinkedHashMap<>();

    /** curConfigMap records the exercised configuration parameter by the current input */
    private static Map<String, Object> curConfigMap = new LinkedHashMap<>();

    /** settedConfigs records the configuration parameter that is set by the current fuzzed test */
    private static Set<String> setConfigs = new LinkedHashSet<>();

    //private static Map<String, Object> configKeyMap = new LinkedHashMap<>();

    /** Tracks the default values observed when first encountering a config */
    private static Map<String, Set<Object>> defaultValues = new HashMap<>();

    /** Record the maximum size of the configMap in fuzzed configuration execution */
    private static int maxConfigMapSize = 0;
    /** Record the minimum size of the configMap in fuzzed configuration execution */
    private static int minConfigMapSize = Integer.MAX_VALUE;

    /** Most common used tracker API */
    public synchronized static void trackSet(String name, Object value) {
        /** We actually do not want to exclude set because this will make our generator undeterministic and mutation may
         * have huge jump
         * For example A B C, and then B is removed due to set -- in this case C will be generated use previous B's bytes
        // If the configuration parameter is set by test case, remove it from configMap
        setConfigMap.add(name);
        configMap.remove(name);
         */
        if (name != null) {
            if (Utils.checkObjectSerilizable(value) && !ConfParamGenerator.observedBefore(name)) {
                if (!defaultValues.containsKey(name)) {
                    defaultValues.put(name, new HashSet<>());
                }
                defaultValues.get(name).add(value);
            }
            setConfigs.add(name);
            totalConfigMap.put(name, value);
            curConfigMap.put(name, value);
        }
    }

    public synchronized static void trackGet(String name, Object value) {
        if (name != null) {
            if (Utils.checkObjectSerilizable(value) && !ConfParamGenerator.observedBefore(name)) {
                if (!defaultValues.containsKey(name)) {
                    defaultValues.put(name, new HashSet<>());
                }
                defaultValues.get(name).add(value);
            }
            totalConfigMap.put(name, value);
            curConfigMap.put(name, value);
        }
    }

    /** For Configuration with Object key, should use the following two tracker APIs */
    public synchronized static void trackSet(String name, Object key, Object value) throws IllegalStateException {
        trackSet(name, value);
        //trackKey(name, key);
    }

    public synchronized static void trackGet(String name, Object key, Object value) throws IllegalStateException {
        trackGet(name, value);
        //trackKey(name, key);
    }

//    public synchronized static void trackKey(String name, Object key) throws IllegalStateException {
//        if (key == null) {
//            throw new IllegalStateException(String.format("The key for %s is null!", name));
//        }
//        if (!configKeyMap.containsKey(name)) {
//            configKeyMap.put(name, key);
//        } else if (!configKeyMap.get(name).equals(key)) {
//            throw new IllegalStateException(String.format("Multiple keys found for %s! (%s, %s)", name,
//                    configKeyMap.get(name), key));
//        }
//    }

    /** Tracker with LOGGER */
    public synchronized static void trackSet(Object LOG, String name, Object value) {
        if (isLogEnabled) {
            writeToLog(LOG, "[CTEST][SET-PARAM] %s = %s", name, value);
        }
        trackSet(name, value);
    }

    public synchronized static void trackGet(Object LOG, String name, Object value) {
        if (isLogEnabled) {
            writeToLog(LOG, "[CTEST][GET-PARAM] %s = %s", name, value);
        }
        trackGet(name, value);
    }

    public static void writeToLog(Object LOG, String mes, Object... args) {
        try {
            Method info = LOG.getClass().getMethod("info", String.class);
            info.invoke(LOG, String.format(mes, args));
        } catch (Throwable e) {
            // ignore, not outputting logs
        }
    }

    public synchronized static Boolean isParamSet(String name) {
        return setConfigs.contains(name);
    }

    /**
     * Called by projects' configuration API to record exercised
     * configuration parameter by test
     * @param name
     * @param value
     * @param isSet
     */
    public synchronized static void track(String name, Object value, boolean isSet) {
        if (isSet) {
            trackSet(name, value);
        } else {
            trackGet(name, value);
        }
    }

    public static Set<Object> getDefaultValues(String name) {
        return Collections.unmodifiableSet(defaultValues.getOrDefault(name, new HashSet<>()));
    }

    /**
     * Get configMap
     * @return A map stores the pairs of configuration parameter name and value
     */
    public synchronized static Map<String, Object> getTotalConfigMap() {
        int curConfigMapSize = getMapSize();
        maxConfigMapSize = Math.max(maxConfigMapSize, curConfigMapSize);
        if (curConfigMapSize > 0) {
            minConfigMapSize = Math.min(minConfigMapSize, curConfigMapSize);
        }
        return new LinkedHashMap<>(totalConfigMap);
    }

    public static Map<String, Object> getCurConfigMap() {
        return curConfigMap;
    }

    public static Set<String> getSetConfigs() {
        return setConfigs;
    }

//    public synchronized static Object getConfigKey(String name) {
//        return configKeyMap.get(name);
//    }
//
//    public synchronized static Map<String, Object> getConfigKeyMap() {
//        return Collections.unmodifiableMap(configKeyMap);
//    }
//
//    public synchronized static void setConfigKeyMap(Map<String, Object> configKeyMap) {
//        ConfigTracker.configKeyMap = new LinkedHashMap<>(configKeyMap);
//    }

    /**
     * Get the maximum size of the configMap in fuzzed configuration execution
     * @return The maximum size of the configMap in fuzzed configuration execution
     */
    public synchronized static int getMaxConfigMapSize() {
        return maxConfigMapSize;
    }

    /**
     * Get the minimum size of the configMap in fuzzed configuration execution
     * @return The minimum size of the configMap in fuzzed configuration execution
     */
    public synchronized static int getMinConfigMapSize() {
        return minConfigMapSize;
    }

    /**
     * Clear configMap
     */
    public synchronized static void clearConfigMap() {
        totalConfigMap.clear();
    }

    public synchronized static void clearCurConfigMap() {
        curConfigMap.clear();
    }

    /**
     * Clear setConfigMap
     */
    public synchronized static void clearSetConfigs() {
        setConfigs.clear();
    }

    /**
     * Get the size of configMap
     * @return size of configMap
     */
    public synchronized static int getMapSize() {
        return totalConfigMap == null ? 0 : totalConfigMap.size();
    }

    public synchronized static void setMap(Map<String, String> map) {
        totalConfigMap.clear();
        totalConfigMap.putAll(map);
    }
}
