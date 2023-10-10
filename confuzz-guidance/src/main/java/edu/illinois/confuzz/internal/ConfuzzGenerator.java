package edu.illinois.confuzz.internal;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfuzzGenerator {
    /* Mapping that let generator know which configuration parameter to fuzz */
    private static Map<String, Object> curTestMapping = null;
    private static Map<String, Object> injectedConfig = new LinkedHashMap<>();

    private static Object generatedConfigObject = null;

    /** A unify generator */

    public static Object generate(SourceOfRandomness random, Object configObject, Class<?> type, String methodName,
                                  Class<?>[] parameterTypes)
            throws NoSuchMethodException, ClassCastException {
        Map<String, Object> configMap = generate(random);
        Object typedConfigObject = type.cast(configObject);
        Method setMethod = typedConfigObject.getClass().getMethod(methodName, parameterTypes);

        for (Map.Entry<String, Object> entry: configMap.entrySet()) {
            try {
                //Object key = ConfigTracker.getConfigKeyMap().getOrDefault(entry.getKey(), entry.getKey());
                Object key = entry.getKey();
                setMethod.invoke(typedConfigObject, key, entry.getValue());
            } catch (Exception e) {
                ConfParamGenerator.debugPrint(" Configuration Name: " + entry.getKey() + " value " +
                        entry.getValue() + " Exception:");
                e.printStackTrace();
            }
        }
        // clear CurConfigMap after generator set all the values, so that this map always records the parameter set
        // from the real test execution
        ConfigTracker.clearCurConfigMap();
        generatedConfigObject = typedConfigObject;
        return typedConfigObject;
    }

    public static Map<String, Object> generate(SourceOfRandomness random) throws ClassCastException {
        // In coverage goal, we only need to directly get the stored configuration object and return it.
        // TODO: Here if Fuzzer complains nothing to generate, we can randomly consume some bytes
        if (Utils.isCoverageGoal()) {
            if (CoverageGuidance.curConfigMap == null) {
                throw new RuntimeException("CoverageGuidance returns a null config object.");
            }
            //ConfigTracker.setConfigKeyMap(CoverageGuidance.configKeyMap);
            return CoverageGuidance.curConfigMap;
        }
        ConfParamGenerator.debugPrint("Map size before freshMap = " + ConfigTracker.getMapSize());
        injectedConfig.clear();
        curTestMapping = ConfigTracker.getTotalConfigMap();

        if (Boolean.getBoolean("preround")) {
            ConfParamGenerator.debugPrint("Return default configuration");
            return new LinkedHashMap<>();
        }

        // Directly return the default configuration if it's pre-round
        // Otherwise the curTestMapping size should larger than 0 (if not then there is
        // no configuration parameter need to be fuzzed)
        if (curTestMapping.size() == 0) {
            throw new IllegalArgumentException("No configuration parameter tracked from test. " +
                    "Make sure (1) Set -DconfigFuzz flag correctly; (2) the test exercises at least " +
                    "one configuration parameter");
        }


        // curTestMapping is a sorted TreeMap
        /* THERE MIGHT BE A BUG HERE, LET's SEE */
        // ConfigTracker.clearSetConfigs();

        // build the map of key-val pairs
        Map<String, Object> ret = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : curTestMapping.entrySet()) {
            try {
                Object randomValue = ConfParamGenerator.generate(entry.getKey(), entry.getValue(), random);
                // Do not inject IGNORED configs, SET configs, and non-serializable configs
                if (ConfuzzGuidance.ignoredConfigs.contains(entry.getKey())
                        || ConfigTracker.getSetConfigs().contains(entry.getKey())
                        || !Utils.checkObjectSerilizable(randomValue)) {
                    continue;
                }
                // Set the configuration parameter only if the random value is not null (discussed for now)
                if (randomValue != null) {
                    ret.put(entry.getKey(), randomValue);
                    injectedConfig.put(entry.getKey(), randomValue);
                    ConfParamGenerator.debugPrint("Setting conf " + entry.getKey() + " = " + randomValue);
                }
            } catch (Exception e) {
                ConfParamGenerator.debugPrint(" Configuration Name: " + entry.getKey() + " value " +
                        entry.getValue() + " Exception:");
                e.printStackTrace();
            }
        }
        //ConfigTracker.freshMap();  // --> Comment out if we want to incrementally collect exercised config set.
        injectedConfig = ret;
        return ret;
    }

    public static Map<String, Object> getInjectedConfigMap() {
        return injectedConfig;
    }

    public static Map<String, Object> getInjectedConfig() {
        return injectedConfig;
    }
}
