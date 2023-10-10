package edu.illinois.confuzz.internal;

import com.github.curiousoddman.rgxgen.RgxGen;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class RegexGenerator {

    /** Keep string generators in map to speed up fuzzing **/
    private static final Map<String, RgxGen> generators = new LinkedHashMap<>();

    /**
     * Generate a random string based on the regex
     * @param seed
     * @param regex
     * @param save if the generator should be saved. default is true.
     * @return a random string generated according to the regex
     */
    public static String generate(int seed, String regex, boolean save) {
        RgxGen generator = generators.get(regex);
        if (generator == null) {
            generator = new RgxGen(regex);
            if (save) {
                generators.put(regex, generator);
            }
        }
        return generator.generate(new Random(seed));
    }

    public static String generate(int seed, String regex) {
        return generate(seed, regex, true);
    }

}
