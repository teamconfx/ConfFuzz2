package edu.illinois.fakefuzz;


import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.illinois.confuzz.internal.ConfuzzGenerator;
import java.io.IOException;

public class ConfigurationGenerator extends Generator<Configuration> {
    private static Configuration generatedConf = null;
    private static String setMethodName = "generatorSet";
    private static Class<?>[] setParameterTypes = new Class[]{String.class, Object.class};

    public ConfigurationGenerator() {
        super(Configuration.class);
    }

    public static Configuration getGeneratedConfig() {
        return generatedConf == null ? null : new Configuration(generatedConf);
    }

    public Configuration generate(SourceOfRandomness random, GenerationStatus generationStatus) {
        try {
            Configuration conf = new Configuration(true);
            generatedConf = (Configuration)ConfuzzGenerator.generate(random, conf, conf.getClass(), setMethodName, setParameterTypes);
            return generatedConf;
        } catch (IOException | NoSuchMethodException var4) {
            throw new RuntimeException(var4);
        }
    }
}
