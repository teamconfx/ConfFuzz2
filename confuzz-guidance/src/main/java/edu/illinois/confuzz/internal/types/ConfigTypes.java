package edu.illinois.confuzz.internal.types;

import edu.illinois.confuzz.internal.ConfuzzGuidance;

import java.lang.reflect.Method;

public class ConfigTypes {

    public static final Class<?>[] types = { EnumConf.class, RegexConf.class, IntegerConf.class, BooleanConf.class,
            FloatConf.class, DurationConf.class, DataSizeConf.class, ListConf.class, DefaultConf.class };

    /**
     * Get the ConfigType containing all the stuff needed for generation and tracking
     * @param name the name of the config
     * @param value the value observed
     * @return the ConfigType object for the config. Create a new one if first seen.
     */
    public static ConfigType getType(String name, Object value) {
        for (Class<?> type: types) {
            try {
                Method typeCheck = type.getMethod("check", String.class, Object.class);
                if ((boolean) typeCheck.invoke(null, name, value)) {
                    return (ConfigType) type.getDeclaredConstructors()[0].newInstance(name, value);
                }
            } catch (Exception e) {
                //TODO: consider throwing the exception here
                e.printStackTrace();
            }
        }
        return new DefaultConf(name, value);
    }
}
