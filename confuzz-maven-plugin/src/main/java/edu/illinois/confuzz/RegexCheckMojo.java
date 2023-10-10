package edu.illinois.confuzz;


import edu.illinois.confuzz.internal.RegexGenerator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;


@Mojo(name = "regex-check", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
public class RegexCheckMojo extends AbstractMojo {
    private static Map<String, String> paramRegexMapping = new HashMap<>();
    private static String regexFile = null;
    private static String PARAM_EQUAL_MARK = "=";

    public void execute() {
        Log logger = getLog();
        try {
            if (checkRegex(logger)) {
                logger.info("Regexes are valid");
            } else {
                logger.info("Regexes are invalid");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean checkRegex(Log logger) throws IOException {
        logger.info("Start checking regexes");
        regexFile = System.getProperty("regexFile", "regex");
        if (regexFile == null) {
            throw new IOException("Please specify regex file with -DregexFile=${regex-file-path}");
        }
        paramRegexMapping = parseParamRegex();
        for (Map.Entry<String, String> entry : paramRegexMapping.entrySet()) {
            logger.info("Checking regex: " + entry.getKey() + " " + entry.getValue());
            String value = RegexGenerator.generate(new Random().nextInt(), entry.getValue());
            logger.info("Generated value: " + value);
        }
        return true;
    }

    private static Map<String, String> parseParamRegex() throws IOException {
        Map<String, String> result = new TreeMap<>();
        File file = Paths.get(regexFile).toFile();
        if (!file.exists() || !file.isFile()){
            throw new IOException("Unable to read file: " + file.getPath() + "; Please make sure to set " +
                    "-Dregex.file with the correct file path");
        }
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        int index;
        while ((line = br.readLine()) != null) {
            index = line.indexOf(PARAM_EQUAL_MARK);
            if (index != -1) {
                String name = line.substring(0, index).trim();
                String regex = line.substring(index + 1);
                result.put(name, regex);
            }
        }
        return result;
    }
}
