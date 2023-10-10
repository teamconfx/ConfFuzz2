package edu.illinois.confuzz.internal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

public class RegexGeneratorTest {
    @Test
    public void testStringGenerationWithRegex() {
        String filePathRegex = "/|(/[a-zA-Z0-9_-]+)+";
        String gen1 = RegexGenerator.generate(0, filePathRegex);
        String gen2 = RegexGenerator.generate(0, filePathRegex);
        String gen3 = RegexGenerator.generate(-1, filePathRegex);
        System.out.println(gen1 + "\n" + gen2 + "\n" + gen3);
        System.out.println(gen1.length() + "\n" + gen2.length() + "\n" + gen3.length());

        /** Check the generated strings are deterministic under same seed **/
        Assert.assertEquals(gen1, gen2);
        /** Check the generated strings are not same under different seed **/
        Assert.assertNotEquals(gen1, gen3);
        /** Check the generated strings match given regex **/
        Assert.assertTrue(gen1.matches(filePathRegex));
        Assert.assertTrue(gen2.matches(filePathRegex));
        Assert.assertTrue(gen3.matches(filePathRegex));
    }
}
