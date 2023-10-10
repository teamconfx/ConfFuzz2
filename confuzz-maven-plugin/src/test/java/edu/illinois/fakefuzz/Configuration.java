package edu.illinois.fakefuzz;

import edu.illinois.confuzz.internal.ConfigTracker;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class Configuration {
    private Map<String, Object> conf;

    public Configuration() throws IOException {
        this.conf = new LinkedHashMap();
        this.conf.putAll(this.loadDefaultConfigFromFile("src/test/resources/fake-config.properties"));
        File injectFile = new File("target/classes/ctest.properties");
        if (injectFile.exists()) {
            this.conf.putAll(this.loadDefaultConfigFromFile("target/classes/ctest.properties"));
        }
        Configuration generatedConf = ConfigurationGenerator.getGeneratedConfig();
        if (generatedConf != null) {
            this.conf.putAll(generatedConf.getConf());
        }

    }

    public Configuration(Boolean loadFromFile) throws IOException {
        this.conf = new LinkedHashMap();
        if (loadFromFile) {
            this.conf.putAll(this.loadDefaultConfigFromFile("src/test/resources/fake-config.properties"));
            File injectFile = new File("target/classes/ctest.properties");
            if (injectFile.exists()) {
                this.conf.putAll(this.loadDefaultConfigFromFile("target/classes/ctest.properties"));
            }
        }

    }

    public Configuration(Configuration conf) {
        this.conf = new LinkedHashMap(conf.getConf());
    }

    public Map<String, Object> getConf() {
        return this.conf;
    }

    public Object get(String key) {
        Object value = this.conf.get(key);
        if (key.equals("failure2") || key.equals("failure3")) {
            ConfigTracker.trackGet(key, key);
        } else {
            ConfigTracker.trackGet(key, value);
        }

        return value;
    }

    public String getStr(String key) {
        Object value = this.get(key);
        return (String)value;
    }

    public int getInt(String key) {
        String value = (String)this.get(key);
        return Integer.valueOf(value);
    }

    public float getFloat(String key) {
        String value = (String)this.get(key);
        return Float.valueOf(value);
    }

    public Boolean getBoolean(String key) {
        String value = (String)this.get(key);
        return Boolean.valueOf(value);
    }

    public Object get(String key, Object defaultValue) {
        Object value = this.conf.getOrDefault(key, defaultValue);
        ConfigTracker.trackGet(key, value);
        return value;
    }

    public void set(String key, Object value) {
        ConfigTracker.trackSet(key, value);
        this.conf.put(key, value);
    }

    public boolean contains(String key) {
        return this.conf.containsKey(key);
    }

    public void generatorSet(String key, Object value) {
        this.conf.put(key, value);
    }

    public Map<String, Object> loadDefaultConfigFromFile(String filePath) throws IOException {
        BufferedReader fileReader = new BufferedReader(new FileReader(filePath));
        Map<String, Object> map = new LinkedHashMap();

        for(String line = fileReader.readLine(); line != null; line = fileReader.readLine()) {
            String[] pair = line.split("=");
            map.put(pair[0], pair[1]);
        }

        fileReader.close();
        return map;
    }
}
