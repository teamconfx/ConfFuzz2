package edu.illinois.confuzz.internal;

import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import okio.BufferedSource;
import okio.Okio;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.*;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;


public class ConfigUtils {
    private static final String AZURE_DIR_PREFIX = "/mnt/batch/";

    /** The map that contains the original configuration in the injection target configuration file
     * We need this because our injection will append to it instead of overwriting */
    private static Map<String, String> ORIGINAL_CONFIG_MAP = new LinkedHashMap<>();

    /** A copy of original configuration file to make clean() easier */
    private static File copiedConfigFile;

    /**
     * Check whether the configuration file for configuration injection exists
     * @return
     * @throws FileNotFoundException
     */
    public static Boolean checkConfigFile(File configFile) {
        return configFile.exists();
    }

    /**
     * Initialize a new configuration file for configuration injection
     * @return
     * @throws FileNotFoundException
     */
    public static void initializeInjectionConfigFile(File configFile) throws IOException {
        // cleanConfig(configFile, System.out);
        // emptyXML(configFile);
        // READ the original config file from XML
        // loadConfigurationFile(configFile);

        // Check file suffix and load the configuration file
        String suffix = getFileSuffix(configFile);
        switch (suffix) {
            case "xml":
                emptyXML(configFile);
                break;
            case "properties":
            case "cfg":
            case "yaml":
                cleanConfigPropertiesAndCFGAndYAML(configFile);
                break;
            default:
                throw new RuntimeException("Unsupported configuration file type: " + suffix);
        }
    }

    /**
     * Copy the original configuration file to a new file with the same name but with a .copy suffix
     * This makes it easier to clean up the configuration file after the injection
     * @return
     * @throws FileNotFoundException
     */
    private static boolean copyConfigFile(File configFile) throws IOException {
        copiedConfigFile = new File(configFile.getAbsolutePath() + ".copy");
        FileUtils.copyFile(configFile, copiedConfigFile);
        if (copiedConfigFile.exists()) {
            return true;
        }
        return false;
    }

    /**
     * Get the file suffix
     * @param file
     * @return
     */
    private static String getFileSuffix(File file) {
        // get file suffix
        String filename = file.getName();
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    /**
     * Load the configuration from the given file to ORIGINAL_CONFIG_MAP
     * @param configFile
     * @throws IOException
     */
    public static Map<String, String> loadConfigurationFile(File configFile) throws IOException {
        copyConfigFile(configFile);
        String fileSuffix = getFileSuffix(configFile).toLowerCase();
        InputStream is = new FileInputStream(configFile);
        switch (fileSuffix) {
            case "xml":
                ORIGINAL_CONFIG_MAP = loadFromXML(is);
                break;
            case "cfg":
            case "properties":
                ORIGINAL_CONFIG_MAP = loadFromPropertiesOrCFG(is);
                break;
            case "yaml":
                ORIGINAL_CONFIG_MAP = loadFromYAML(is);
                break;
            default:
                throw new IOException("Unsupported configuration file type: " + fileSuffix);
        }
        return new LinkedHashMap<>(ORIGINAL_CONFIG_MAP);
    }

    private static Map<String, String> loadFromXML(InputStream is) throws IOException {
        return parseXML(is, "property", "name", "value");
    }

    private static Map<String, String> loadFromPropertiesOrCFG(InputStream is) throws IOException {
        return parsePropertiesOrCFG(is, "=");
    }

    private static Map<String, String> loadFromYAML(InputStream is) throws IOException {
        return parsePropertiesOrCFG(is, ": ");
    }

    /**
     * Parse the properties file and return a map of configuration name and value
     * */
    public static Map<String, String> parsePropertiesOrCFG(InputStream is, String regex) throws IOException {
        // Key Value are separated by "="
        // Read the file line by line
        Map<String, String> map = new LinkedHashMap<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = br.readLine()) != null) {
            String[] kv = line.split(regex);
            if (kv.length == 2) {
                map.put(kv[0], kv[1]);
            }
        }
        return map;
    }

    /**
     * Parse the XML file and return a map of configuration name and value
     * @param is
     * @param tagName
     * @param tagConfigName
     * @param tagConfigValue
     * @return
     * @throws IOException
     */
    public static Map<String, String> parseXML(InputStream is, String tagName, String tagConfigName, String tagConfigValue) throws IOException {
        try {
            Map<String, String> map = new LinkedHashMap<>();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);
            NodeList nl = doc.getElementsByTagName(tagName);
            for (int i = 0; i < nl.getLength(); i++) {
                NodeList nl2 = nl.item(i).getChildNodes();
                String configName = "";
                String configValue = "";
                for (int j = 0; j < nl2.getLength(); j++) {
                    Node n = nl2.item(j);
                    if (n.getNodeName().equals(tagConfigName)) configName = n.getTextContent();
                    if (n.getNodeName().equals(tagConfigValue)) configValue = n.getTextContent();
                }

                // Multiple configuration files may have duplicated settings. We choose the last one as the final value (Overwrite)
                // This is the same idea as some real-world software like Hadoop.
                map.put(configName, configValue);
            }
            is.close();
            return map;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static LinkedHashMap<String, String> getConfig(File file, String separator) throws IOException {
        LinkedHashMap<String, String> res = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file.getAbsolutePath()))) {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                String[] split = sCurrentLine.split(separator);
                String key = split[0].trim();
                String value;
                if (split.length == 2) {
                    value = split[1].trim();
                } else {
                    throw new IOException("Should not have multiple separator in config file " + file.getAbsolutePath()
                            + "line " + sCurrentLine);
                }
                res.put(key, value);
            }
        }
        return res;
    }

    public static ArrayList<Map> getConfigMapsFromJSON(File jsonFile) throws IOException {
        Moshi MOSHI = new Moshi.Builder().build();
        try (BufferedSource bs = Okio.buffer(Okio.source(jsonFile))) {
            return (ArrayList<Map>) MOSHI
                    .adapter(Types.newParameterizedType(List.class, Map.class))
                    .indent("    ")
                    .fromJson(bs);
        }
    }

    /**
     * Return the diff map, that contains all the keys in child but not in parent, or the values are different
     * @param child the child map
     * @param parent the parent map
     */
    public static Map<String, Object> childDiffParent(Map<String, Object> child, Map<String, Object> parent) {
        Map<String, Object> diff = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : child.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (!parent.containsKey(key) || !parent.get(key).equals(value)) {
                diff.put(key, value);
            }
        }
        return diff;
    }

    public static <T> Map<T, T> getMapDiff(Map<T, T> map1, Map<T, T> map2) {
        // Get keys present in map1 but not in map2
        Map<T, T> difference1 = new HashMap<>(map1);
        difference1.keySet().removeAll(map2.keySet());

        // Get keys present in map2 but not in map1
        Map<T, T> difference2 = new HashMap<>(map2);
        difference2.keySet().removeAll(map1.keySet());

        // Get keys present in both maps but with different values
        Map<T, T> difference3 = new HashMap<>();
        for (Map.Entry<T, T> entry : map1.entrySet()) {
            T key = entry.getKey();
            T value1 = entry.getValue();
            T value2 = map2.get(key);
            if (value2 != null && !value2.equals(value1)) {
                difference3.put(key, value2);
            }
        }

        Map<T, T> res = new LinkedHashMap<>();
        res.putAll(difference1);
        res.putAll(difference2);
        res.putAll(difference3);
        return res;
    }

    public static void cleanConfig(File configFile, PrintStream out) throws IOException {
        String extension = FilenameUtils.getExtension(configFile.getName());
        switch (extension.toLowerCase()) {
            case "xml":
                cleanXML(configFile);
                break;
            case "properties":
            case "cfg":
            case "yaml":
                cleanConfigPropertiesAndCFGAndYAML(configFile);
                break;
            default:
                out.println("Failed to inject configuration : Do not support " + extension.toLowerCase() + " file");
                break;
        }
    }

    private static void cleanConfigPropertiesAndCFGAndYAML(File configFile) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(configFile, false))) {
            out.flush();
        }
    }

    private static void cleanXML(File configFile) throws IOException {
        // Directly copy back from the copied file
        FileUtils.copyFile(copiedConfigFile, configFile);
    }

    public static void emptyXML(File configFile) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(configFile, false))) {
            out.write("<?xml version=\"1.0\"?><?xml-stylesheet type=\"text/xsl\" href=\"configuration.xsl\"?><configuration></configuration>");
        }
    }

    public static void injectConfig(File configFile, Map<String, String> configPairs, PrintStream out, Boolean localDebug)
            throws IOException, TransformerException, ParserConfigurationException {
        if (configPairs.isEmpty()) {
            return;
        }
        String extension = FilenameUtils.getExtension(configFile.getName());

        switch (extension.toLowerCase()) {
            case "xml":
                injectConfigXML(configFile, configPairs, out, localDebug);
                break;
            case "properties":
            case "cfg":
                injectConfigPropertiesAndCFG(configFile, configPairs, out, localDebug);
                break;
            case "yaml":
                injectConfigYAML(configFile, configPairs, out, localDebug);
                break;
            default:
                throw new IOException("Failed to inject configuration : Do not support " + extension.toLowerCase() + " file");
        }
    }

    private static void inject(File configFile, Map<String, String> configPairs, PrintStream out, Boolean localDebug, String regex) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(configFile, false))) {
            for (Map.Entry<String, String> configPair : configPairs.entrySet()) {
                if (localDebug && configPair.getKey().contains(AZURE_DIR_PREFIX)) {
                    continue;
                }
                String configName = configPair.getKey();
                String configValue = configPair.getValue();
                pw.println(configName + regex + configValue);
            }
        }
    }

    /**
     * Inject real configuration in *.properties file for testing
     * @param configPairs
     */
    private static void injectConfigPropertiesAndCFG(File configFile, Map<String, String> configPairs, PrintStream out, Boolean localDebug) throws IOException {
        inject(configFile, configPairs, out, localDebug, "=");
    }

    private static void injectConfigYAML(File configFile, Map<String, String> configPairs, PrintStream out, Boolean localDebug) throws IOException {
        inject(configFile, configPairs, out, localDebug, ": ");
    }


    /**
     * Inject real configuration in *.xml file for testing
     * @param configPairs
     */
    private static void injectConfigXML(File configFile, Map<String, String> configPairs, PrintStream out, Boolean localDebug)
            throws IOException, TransformerException, ParserConfigurationException {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root element and add xml declaration
            Document doc = docBuilder.newDocument();
            ProcessingInstruction pi1 = doc.createProcessingInstruction("xml", "version=\"1.0\"");
            ProcessingInstruction pi2 = doc.createProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\"configuration.xsl\"");
            doc.insertBefore(pi1, doc.getDocumentElement());
            doc.insertBefore(pi2, doc.getDocumentElement());

            //addingStylesheet(doc);
            Element rootElement = doc.createElement("configuration");
            doc.appendChild(rootElement);

            /** For now we do not need to inject the original config because we create a new ctest injection file
             * for each debug session
             * // first we need to inject the original config
             * // injectConfigXMLFromMap(doc, rootElement, ORIGINAL_CONFIG_MAP, localDebug);
             * // then we need to inject the ctest config that causing failure
             */
            injectConfigXMLFromMap(doc, rootElement, configPairs, localDebug);
            OutputStream os = new FileOutputStream(configFile);
            writeXML(doc, os);
            os.close();
        } catch (Exception e) {
            out.println("Failed to inject configuration " + e.getMessage());
            throw e;
        }
    }

    private static void injectConfigXMLFromMap(Document doc, Element rootElement, Map<String, String> configPairs, Boolean localDebug) {
        for (Map.Entry<String, String> configPair : configPairs.entrySet()) {
            // add xml elements and add property to root
            if (localDebug && configPair.getKey().contains(AZURE_DIR_PREFIX)) {
                continue;
            }
            Element property = doc.createElement("property");
            rootElement.appendChild(property);
            // add name and value to property
            Element name = doc.createElement("name");
            Element value = doc.createElement("value");
            name.setTextContent(configPair.getKey());
            value.setTextContent(configPair.getValue());
            property.appendChild(name);
            property.appendChild(value);
        }
    }

    /**
     * Inject parameter to XML file
     * @param doc
     * @param output
     * @throws TransformerException
     * @throws IOException
     */
    private static void writeXML(Document doc, OutputStream output) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        // pretty print
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(output);

        transformer.transform(source, result);
    }



    public static String getMapDiffMsg(Map<String, String> map1, Map<String, String> map2) {
        // Get keys present in map1 but not in map2
        Map<String, String> difference1 = new HashMap<>(map1);
        difference1.keySet().removeAll(map2.keySet());

        // Get keys present in map2 but not in map1
        Map<String, String> difference2 = new HashMap<>(map2);
        difference2.keySet().removeAll(map1.keySet());

        // Get keys present in both maps but with different values
        Map<String, String> difference3 = new HashMap<>();
        for (Map.Entry<String, String> entry : map1.entrySet()) {
            String key = entry.getKey();
            String value1 = entry.getValue();
            String value2 = map2.get(key);
            if (value2 != null && !value2.equals(value1)) {
                difference3.put(key, value2);
            }
        }
        String msg = "\n\n";
        msg += diffMsgHelper(difference1, "[ONLY-FILE]");
        msg += diffMsgHelper(difference2, "[ONLY-REPLAY]");
        msg += diffMsgHelper(difference3, "[DIFF]");
        return msg;
    }

//    private String getMapDiffMsg(Map<String, String> confFromFile, Map<String, String> confFromReplay) {
//        String msg = "\n\n";
//        MapDifference<String, String> diff = Maps.difference(confFromFile, confFromReplay);
//        //msg += getMapStr(confFromFile);
//        //msg += getMapStr(confFromReplay);
//        msg += diffMsgHelper(diff.entriesOnlyOnLeft(), "[ONLY-FILE]");
//        msg += diffMsgHelper(diff.entriesOnlyOnRight(), "[ONLY-REPLAY]");
//        msg += diffMsgHelper(diff.entriesDiffering());
//        return msg;
//    }

    private static String diffMsgHelper(Map<String, String> map, String prefix) {
        String msg = "\n=====ONLY-ONE=====\n";
        for (Map.Entry<String, String> entry : map.entrySet()) {
            msg += String.format("%s : %s = %s\n", prefix, entry.getKey(), entry.getValue());
        }
        return msg;
    }

//    private String diffMsgHelper(Map<String, MapDifference.ValueDifference<String>> map) {
//        String msg = "\n=====DIFF=====\n";
//        String prefix = "[VALUE-DIFF]";
//        for (Map.Entry<String, MapDifference.ValueDifference<String>> entry : map.entrySet()) {
//            String key = entry.getKey();
//            MapDifference.ValueDifference<String> valueDiff = entry.getValue();
//            msg += String.format("%s : %s -> FILE = %s; REPLAY = %s\n", prefix, entry.getKey(),
//                    valueDiff.leftValue(), valueDiff.rightValue());
//        }
//        return msg;
//    }

    private String getMapStr(Map<String, String> map) {
        String s = "";
        for (Map.Entry<String, String> entry : map.entrySet()) {
            s += String.format("%s = %s\n", entry.getKey(), entry.getValue());
        }
        return s;
    }

    /**
     * Convert from Map<String, Object> to Map<String, String>.
     * Remove all the null values in the map.
     * @param map the map to convert
     * @return the converted map
     */
    public static Map<String, String> convertFromObjectToStringMap(Map<String, Object> map) {
        return map.entrySet().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));
        /*
        Map<String, String> util = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry: map.entrySet()) {
            util.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return util;
        */
        // TODO: why does this code triggers a test failure?
        //return new LinkedHashMap<>(map.entrySet().stream()
        //        .collect(Collectors.toConcurrentMap(Map.Entry::getKey, e -> String.valueOf(e.getValue()))));
    }

}
