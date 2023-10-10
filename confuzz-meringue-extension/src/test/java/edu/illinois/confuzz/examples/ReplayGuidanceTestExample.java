package edu.illinois.confuzz.examples;

import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import edu.illinois.confuzz.internal.ConfigTracker;
import org.junit.runner.RunWith;

import java.util.Map;


@RunWith(JQF.class)
@SuppressWarnings("NewClassNamingConvention")
public class ReplayGuidanceTestExample {
    @Fuzz
    public void test(@From(TestConfigurationGenerator.class) Map<String, String> configuration) {
        System.out.println(configuration);
        String value1 = configuration.getOrDefault("red", "none");
        ConfigTracker.trackGet("red", value1);
        if (!value1.equals("none")) {
            String value2 = configuration.getOrDefault("blue", "none");
            ConfigTracker.trackGet("blue", value2);
            if (!value2.equals("none")) {
                String value3 = configuration.getOrDefault("green", "none");
                ConfigTracker.trackGet("green", value3);
                if (!value3.equals("none")) {
                    throw new IllegalArgumentException();
                }
            }
        }
        String value4 = configuration.getOrDefault("pink", "none");
        ConfigTracker.trackSet("pink", value4);
        if (ConfigTracker.getTotalConfigMap().containsKey("pink")) {
            throw new IllegalArgumentException();
        }
    }
}
