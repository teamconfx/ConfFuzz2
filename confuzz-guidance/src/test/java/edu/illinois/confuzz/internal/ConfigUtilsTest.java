package edu.illinois.confuzz.internal;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigUtilsTest {
    private File tempXML;
    private File tempProperties;
    private File tempCFG;
    private Map<String, String> testMap;

    @Before
    public void setUp() {
        tempXML = new File("target/test.xml");
        tempProperties = new File("target/test.properties");
        tempCFG = new File("target/test.cfg");
        testMap = new LinkedHashMap<>();
        testMap.put("key1", "value1");
        testMap.put("key2", "value2");
        testMap.put("key3", "value3");
    }

    @After
    public void tearDown() {
        tempXML.delete();
        tempProperties.delete();
        tempCFG.delete();
    }

    @Test
    public void testXML() throws IOException, ParserConfigurationException, TransformerException {
        ConfigUtils.initializeInjectionConfigFile(tempXML);
        ConfigUtils.injectConfig(tempXML, testMap, System.out, false);
        Map<String, String> map = ConfigUtils.loadConfigurationFile(tempXML);
        Map<String, String> diffMap = ConfigUtils.getMapDiff(map, testMap);
        Assert.assertTrue(diffMap.isEmpty());
        ConfigUtils.cleanConfig(tempXML, System.out);
    }

    @Test
    public void testProperties() throws IOException, ParserConfigurationException, TransformerException {
        ConfigUtils.initializeInjectionConfigFile(tempProperties);
        ConfigUtils.injectConfig(tempProperties, testMap, System.out, false);
        Map<String, String> map = ConfigUtils.loadConfigurationFile(tempProperties);
        Map<String, String> diffMap = ConfigUtils.getMapDiff(map, testMap);
        Assert.assertTrue(diffMap.isEmpty());
        ConfigUtils.cleanConfig(tempProperties, System.out);
        // check if the file is empty
        Assert.assertTrue(tempProperties.length() == 0);
    }

    @Test
    public void testCFG() throws IOException, ParserConfigurationException, TransformerException {
        ConfigUtils.initializeInjectionConfigFile(tempCFG);
        ConfigUtils.injectConfig(tempCFG, testMap, System.out, false);
        Map<String, String> map = ConfigUtils.loadConfigurationFile(tempCFG);
        Map<String, String> diffMap = ConfigUtils.getMapDiff(map, testMap);
        Assert.assertTrue(diffMap.isEmpty());
        ConfigUtils.cleanConfig(tempCFG, System.out);
        // check if the file is empty
        Assert.assertTrue(tempCFG.length() == 0);
    }
}
