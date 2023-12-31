package edu.illinois.confuzz.internal;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeoutException;

/**
 * dictionary for constraints
 */
public class ConfigConstraints {
    /** File path that stores parameter regexes */
    private static final String regexFile = System.getProperty("regex.file", "regex.json");

    /** Mapping that keeps all parameter valid regex and/or range supported (from the regex file) */
    private static final Map<String, String> paramRegexMapping = new TreeMap<>();
    private static final Map<String, Integer> paramLowerBound = new TreeMap<>();
    private static final Map<String, Integer> paramUpperBound = new TreeMap<>();

    public static void init() throws IOException, TimeoutException {
        parseParamRegex();
        checkRegex();
    }

    public static Integer getLowerBound(String name, Integer defaultValue) {
        if (name == null) {
            return defaultValue;
        }
        return paramLowerBound.getOrDefault(name, defaultValue);
    }

    public static Integer getUpperBound(String name, Integer defaultValue) {
        if (name == null) {
            return defaultValue;
        }
        return paramUpperBound.getOrDefault(name, defaultValue);
    }

    public static String getRegex(String name) {
        return paramRegexMapping.get(name);
    }

    public static Boolean hasRegex(String name) {
        if (name == null) {
            return false;
        }
        return paramRegexMapping.containsKey(name);
    }

    private static void parseParamRegex() throws IOException {
        File file = Paths.get(regexFile).toFile();
        if (!file.exists() || !file.isFile()){
            throw new IOException("Unable to read file: " + file.getPath() + "; Please make sure to set " +
                    "-Dregex.file with the correct file path");
        }
        try (JsonReader reader = new JsonReader(new FileReader(file))) {
            ConfigConstraint[] tmp = new Gson().fromJson(reader, ConfigConstraint[].class);
            if (tmp == null) {
                return;
            }
            for(ConfigConstraint c : tmp) {
                String name = c.name;
                if (c.regex != null) {
                    paramRegexMapping.put(name, c.regex);
                }
                if (c.upper != null) {
                    paramUpperBound.put(name, c.upper);
                }
                if (c.lower != null) {
                    paramLowerBound.put(name, c.lower);
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to parse JSON file: " + file.getAbsolutePath() + " "
                    + e.getMessage());
        }
    }

    // Check all parameter in regex file are able to be generated by RegexGenerator
    private static void checkRegex() throws TimeoutException {
        for (Map.Entry<String, String> entry : paramRegexMapping.entrySet()) {
            RegexGenerator.generate(0, entry.getValue(), false);
        }
    }

    private static final class ConfigConstraint {
        private final String name;
        private final String regex;
        private final Integer upper;
        private final Integer lower;

        private ConfigConstraint(String name, String regex, Integer upper, Integer lower) {
            this.name = name;
            this.regex = regex;
            this.upper = upper;
            this.lower = lower;
        }
    }
}
