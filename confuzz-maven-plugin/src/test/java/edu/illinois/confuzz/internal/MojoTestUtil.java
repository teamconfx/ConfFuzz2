package edu.illinois.confuzz.internal;

import java.io.File;
import java.util.Objects;

public class MojoTestUtil {
    public static File getConfigFile(String fileName) {
        ClassLoader classLoader = BinarySearchTest.class.getClassLoader();
        return new File(Objects.requireNonNull(classLoader.getResource(fileName)).getFile());
    }
}
