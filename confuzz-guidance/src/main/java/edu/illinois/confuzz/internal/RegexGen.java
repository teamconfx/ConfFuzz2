package edu.illinois.confuzz.internal;

import com.mifmif.common.regex.Generex;
import java.util.LinkedHashMap;
import java.util.Map;

@Deprecated
// This class is deprecated because it can't handle some regexes and often causes OOM/StackOverflowError
public class RegexGen {

    /** Keep string generators in map to speed up fuzzing **/
    private Map<String, Generex> generators;

    /** Hard limitation on the string length **/
    private int bound = 100;

    public RegexGen() {
        this.generators = new LinkedHashMap<>();
    }

    public RegexGen(int bound) {
        this.generators = new LinkedHashMap<>();
        this.bound = bound;
    }

    // Add a new regex generator to generators
    public void addGenerator(String paramName, Generex generator) {
        generators.put(paramName, generator);
    }

    // Check whether paramName is in the generators, if not, return null
    public Generex getGenerator(String paramName) {
        if (generators.containsKey(paramName)) {
            return generators.get(paramName);
        }
        return null;
    }

    // Generate a random string based on the regex
    public String generate(long seed, int length, String paramName, String regex) {
        Generex gen = getGenerator(paramName);
        length = length % bound;
        if (gen == null) {
            gen = new Generex(regex);
            gen.setSeed(seed);
            addGenerator(paramName, gen);
        } else {
            gen.setSeed(seed);
        }
        return gen.random(length, length);
    }
}
