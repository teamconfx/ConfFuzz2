package edu.illinois.confuzz.internal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class IdentifyData implements Serializable {
    private static final long serialVersionUID = 1L;
    private String testClassName;
    private String testMethodName;
    private Map<String, String> configMap;
    private int configCount;

    public IdentifyData() {
        this.testClassName = "";
        this.testMethodName = "";
        this.configMap = new HashMap<>();
        this.configCount = 0;
    }

    public IdentifyData(String testClassName, String testMethodName, Map<String, Object> configMap) {
        this.testClassName = testClassName;
        this.testMethodName = testMethodName;
        this.configMap = ConfigUtils.convertFromObjectToStringMap(configMap);
        this.configCount = configMap.size();
    }

    public String getTestClassName() {
        return testClassName;
    }

    public String getTestMethodName() {
        return testMethodName;
    }

    public Map<String, String> getConfigMap() {
        return configMap;
    }

    public int getConfigCount() {
        return configCount;
    }

    public Set<String> getConfigKeys() {
        return configMap.keySet();
    }

    public String toString() {
        String res = testClassName + "#" + testMethodName + "," + configCount + ",[";
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            res += entry.getKey() + "=" + entry.getValue() + ";";
        }
        res += "]";
        return res;
    }

    public boolean isEmpty() {
        return configCount == 0;
    }
}
