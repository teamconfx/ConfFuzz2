package edu.illinois.confuzz.internal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Random;
@Deprecated
public class RegexGenTest {
    @Test
    public void testStringGenerationWithRegex() {
        RegexGen rg = new RegexGen();
        String paramName = "file_path";
        String filePathRegex = "/|(/[a-zA-Z0-9_-]+)+";
        String gen1 = rg.generate(0, 10, paramName, filePathRegex);
        String gen3 = rg.generate(-1, Integer.MAX_VALUE, paramName, filePathRegex);
        String gen2 = rg.generate(0, 10, paramName, filePathRegex);
        System.out.println(gen1 + " ; " + gen2 + " ; " + gen3);

        /** Check the generated strings are deterministic under same seed **/
        Assert.assertEquals(gen1, gen2);
        /** Check the generated strings are not same under different seed **/
        Assert.assertNotEquals(gen1, gen3);
        /** Check the generated strings match given regex **/
        Assert.assertTrue(gen1.matches(filePathRegex));
        Assert.assertTrue(gen2.matches(filePathRegex));
        Assert.assertTrue(gen3.matches(filePathRegex));
    }

    @Test
    public void testDifferentLength() {
        RegexGen rg = new RegexGen();
        String paramName = "file_path";
        String testRegex = "(abcde)+";
        String gen1 = rg.generate(0, 4, paramName, testRegex);
        String gen2 = rg.generate(0, 5, paramName, testRegex);
        String gen3 = rg.generate(0, 6, paramName, testRegex);
        String gen4 = rg.generate(0, new Random().nextInt(), paramName, testRegex);
        System.out.println(gen1 + " ; " + gen2 + " ; " + gen3 + " ; " + gen4);
        Assert.assertEquals(gen1, "abcde");
        Assert.assertEquals(gen2, "abcde");
        Assert.assertEquals(gen3, "abcdeabcde");
        Assert.assertTrue(gen4.matches(testRegex));
    }
}
