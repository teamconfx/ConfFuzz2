package edu.illinois.confuzz;

import edu.illinois.confuzz.internal.IdentifyData;
import edu.illinois.confuzz.internal.IdentifyForkMain;
import edu.neu.ccs.prl.meringue.FileUtil;
import edu.neu.ccs.prl.meringue.JvmLauncher;
import edu.neu.ccs.prl.meringue.SystemPropertyUtil;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.junit.internal.TextListener;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.io.*;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static edu.illinois.confuzz.ConfigurationMojoHelper.getSystemPropertiesFromSurefire;

@Mojo(name = "identify", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
public class IdentifyCTestMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(property = "testName")
    private String testName;

    private static final String IDENTIFY_DATA_OBJECT_FILE = ".confuzz_IdentifyData";
    private static final String CTEST_CSV_FILE = "confuzz_identify_ctest.csv";
    private static final String PARAM_CSV_FILE = "confuzz_identify_param.csv";
    private static String CLASSPATH = System.getProperty("java.class.path");
    @Override
    public void execute() throws MojoExecutionException {
        // 1.check and get the test method list from `target/maven-status/maven-compiler-plugin/testCompile/default-testCompile/createdFiles.lst`
        ClassLoader loader;
        try {
            loader = getClassLoader();
        } catch (DependencyResolutionRequiredException e) {
            throw new RuntimeException(e);
        }
        List<String> testMethods = new LinkedList<>();
        if (testName != null) {
            testMethods.add(testName);
        } else {
            testMethods = getTestMethods(getTestClasses(), loader);
        }
        // 3. run the test method with ConfigTracker and get all test that (1) have config exercised; (2) number of config exercised by only get();
        writeToCSV(runAllTestMethods(testMethods));
    }

    /**
     * Get the class loader
     * @return the class loader
     * @throws MojoExecutionException
     * @throws DependencyResolutionRequiredException
     */
    private ClassLoader getClassLoader() throws MojoExecutionException, DependencyResolutionRequiredException {
        List<String> classpathElements = project.getTestClasspathElements();
        List<URL> urls = new ArrayList<>();
        for (String element : classpathElements) {
            try {
                urls.add(new File(element).toURI().toURL());
            } catch (MalformedURLException e) {
                throw new MojoExecutionException("Error creating URL from classpath element: " + element, e);
            }
        }
        ClassLoader pluginClassLoader = new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());
        // Set the ClassLoader for the current thread
        Thread.currentThread().setContextClassLoader(pluginClassLoader);
        return pluginClassLoader;
    }

    /**
     * Get the list of test classes from the file created by maven-compiler-plugin
     * @return the list of test classes
     * @throws MojoExecutionException
     */
    private List<String> getTestClasses() throws MojoExecutionException {
        List<String> testClasses = new LinkedList<>();
        File dir = new File("target/test-classes");
        try {
            List<File> files = Files.find(dir.toPath(), Integer.MAX_VALUE, 
                (p, bfa) -> bfa.isRegularFile() && p.getFileName().toString().matches(".*\\.class"))
                .map(Path::toFile).collect(Collectors.toList());
            for (File f: files) {
                String relPath = dir.toURI().relativize(f.toURI()).getPath();
                testClasses.add(relPath.replace("/", ".").replace(".class", ""));
            }
        } catch (IOException e) {
            throw new MojoExecutionException(e);
        }
        return testClasses;
    }

    /**
     * Get the list of test methods from the test classes by using JUnit Description
     * @param testClasses
     * @param loader
     * @return  the list of test methods
     * @throws MojoExecutionException
     */
    private List<String> getTestMethods(List<String> testClasses, ClassLoader loader) throws MojoExecutionException {
        List<String> res = new LinkedList<>();
        String currentTestClassName = "";
        for (String testClassName : testClasses) {
            try {
                currentTestClassName = testClassName;
                // Set the ClassLoader for the current thread
                Class<?> testClass = Class.forName(testClassName, false, loader);
                res.addAll(getTestClassAndMethod(testClass));
            } catch (ClassNotFoundException e) {
                System.out.println("Class " + currentTestClassName + " not found");
            }
        }
        return res;

    }

    /**
     * Get the test method name from the given test class with the help of JUnit Description
     * @param testClass
     * @return the list of test method name
     */
    private List<String> getTestClassAndMethod(Class<?> testClass) throws MojoExecutionException {
        List<String> res = new LinkedList<>();
        try {
            JUnitCore junit = new JUnitCore();
            junit.addListener(new TextListener(System.out));
            // Create a JUnit Request
            Request request = Request.aClass(testClass);
            ArrayList<Description> des =  request.getRunner().getDescription().getChildren();
            for (Description d : des) {
                if (!d.getDisplayName().contains("initializationError") && d.isTest()) {
                    String test = d.getClassName() + "#" + d.getMethodName();
                    res.add(test);
                }
            }
            return res;
        } catch (Exception e) {
            throw new MojoExecutionException(e);
        }
    }

    /**
     * Set up the JVM for running the test method
     * @param testClass
     * @param testMethod
     * @return the JVM launcher
     * @throws MojoExecutionException
     */
    public JvmLauncher setupJVM(String testClass, String testMethod) throws MojoExecutionException {
        try {
            String javaHome = System.getProperty("java.home") + "/bin/java";
            File javaExecutable = new File(javaHome);
            List<String> javaOptions = new ArrayList();
            File systemPropertiesFile = new File("confuzz.properties");
            SystemPropertyUtil.store(systemPropertiesFile, "", getSystemPropertiesFromSurefire(project));
            javaOptions.add(String.format("-D%s=%s", IdentifyForkMain.PROPERTIES_KEY, systemPropertiesFile.getAbsolutePath()));
            javaOptions.add("-Dclass=" + testClass);
            javaOptions.add("-Dmethod=" + testMethod);
            javaOptions.add("-cp");
            String classPath = System.getProperty("java.class.path") + ":" + FileUtil.getClassPathElement(IdentifyForkMain.class).getAbsolutePath();
            for (String s : project.getTestClasspathElements()) {
                classPath += ":" + s;
            }
            javaOptions.add(classPath);
            CLASSPATH = classPath;
            String DEBUG = System.getProperty("confuzz.identify.debug");
            if (DEBUG != null && DEBUG.toLowerCase().equals("true")) {
                javaOptions.add(JvmLauncher.DEBUG_OPT + "5005");
            }
            String [] arguments = {testClass, testMethod, System.getProperty("user.dir").toString()};
            return JvmLauncher.fromMain(javaExecutable, IdentifyForkMain.class.getName(),
                    javaOptions.toArray(new String[0]), true, arguments, null, new HashMap<>());
        } catch (Exception e) {
            throw new MojoExecutionException(e);
        }
    }

    /**
     * Run the test method with a new JVM
     * @param launcher
     * @return the set of configuration keys
     * @throws MojoExecutionException
     */
    private IdentifyData runWithNewJVM(JvmLauncher launcher) throws MojoExecutionException {
        try {
            Process p = launcher.launch();
            boolean notTimeout = p.waitFor(60, TimeUnit.SECONDS);
            if (!notTimeout) {
                getLog().info("Exit with Timeout, Test Fail!");
                return new IdentifyData();
            }
            return getConfigInfoFromFile();
        } catch (InterruptedException | IOException e) {
            throw new MojoExecutionException(e);
        }
    }

    /**
     * Get the configuration key set from the stored object file generated by the forked JVM that running the test method
     * @return the configuration key set
     * @throws MojoExecutionException
     */
    private IdentifyData getConfigInfoFromFile() throws MojoExecutionException {
        try {
            // Open the file
            FileInputStream fileIn = new FileInputStream(IDENTIFY_DATA_OBJECT_FILE);
            // Create the ObjectInputStream
            ObjectInputStream in = new ObjectInputStream(fileIn);
            // Read the configuration key set object from the file
            IdentifyData identifyData = (IdentifyData) in.readObject();
            // Close the ObjectInputStream and the file input stream
            in.close();
            fileIn.close();
            return identifyData;
        } catch (IOException | ClassNotFoundException e) {
            throw new MojoExecutionException(e);
        }
    }

    /**
     * Run the test method and track the configuration keys
     * @param testCase
     * @return the set of configuration keys
     * @throws MojoExecutionException
     */
    private IdentifyData runAndTrackTestMethod(String testCase) throws  MojoExecutionException {
        getLog().info("Identifying test:" + testCase);
        // Initialize the JVM launcher
        JvmLauncher launcher = setupJVM(testCase.split("#")[0], testCase.split("#")[1]);
        // Run the test method in a new JVM and return the collected configuration key set
        IdentifyData identifyData = runWithNewJVM(launcher);
        return identifyData;
    }

    /**
     * Run all test methods and track the configuration keys
     * @param testMethods
     * @return the map of test method and configuration key set
     * @throws MojoExecutionException
     */
    private Map<String, IdentifyData> runAllTestMethods(List<String> testMethods) throws MojoExecutionException {
        Map<String, IdentifyData> TestConfigMap = new HashMap<>();
        for (String testMethod : testMethods) {
            IdentifyData identifyData = runAndTrackTestMethod(testMethod);
            if (!identifyData.isEmpty()) {
                TestConfigMap.put(testMethod, identifyData);
            }
        }
        return TestConfigMap;
    }

    /**
     * Write the test method and configuration key set to a CSV file
     * @param targetMap
     * @throws MojoExecutionException
     */
    private void writeToCSV(Map<String, IdentifyData> targetMap) throws MojoExecutionException {
        try {
            Map<String, Set<String>> configMap = new HashMap<>();
            // Write the ctest information to a CSV file
            FileWriter csvWriter = new FileWriter(CTEST_CSV_FILE);
            String header = "Test Method,# configuration parameter,Configuration Key\n";
            csvWriter.write(header);
            for (IdentifyData id : targetMap.values()) {
                csvWriter.write(id.toString() + "\n");
                addValuesToMap(configMap, id.getConfigMap());
            }
            csvWriter.close();

            // Write the total configuration key set to a CSV file
            FileWriter csvWriter2 = new FileWriter(PARAM_CSV_FILE);
            header = "Configuration Parameter\n";
            csvWriter2.write(header);
            for (Map.Entry<String, Set<String>> entry : configMap.entrySet()) {
                String str = entry.getKey() + "=";
                for (String value : entry.getValue()) {
                    if (value != null) {
                        str += value.trim().replace("\n", "") + "|";
                    }
                }
                // remove the last "|"
                if (str.endsWith("|")) {
                    str = str.substring(0, str.length() - 1);
                }
                csvWriter2.write(str + "\n");
            }
            csvWriter2.close();
        } catch (IOException e) {
            throw new MojoExecutionException(e);
        }
    }

    /**
     * Collect all the values of the configuration keys that are used in the all test methods
     * @param map
     * @param values
     */
    private void addValuesToMap(Map<String, Set<String>> map, Map<String, String> values) {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!map.containsKey(entry.getKey())) {
                Set<String> valueSet = new HashSet<>();
                valueSet.add(entry.getValue());
                map.put(entry.getKey(), valueSet);
            }
            map.get(entry.getKey()).add(entry.getValue());
        }
    }
}
