package edu.illinois.confuzz;

import edu.illinois.confuzz.internal.ConfigUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

@Mojo(name = "inject")
public class ConfigurationInjectorMojo extends AbstractMojo {
    @Parameter(property = "targetFile", required = true,
            defaultValue = "${project.build.directory}/classes/ctest.xml")
    private File targetConfigFile;

    @Parameter(property = "sourceFile", required = true)
    private File sourceConfigFile;

    //@Parameter(property = "inject.diff", defaultValue = false)
    private Boolean injectDiff = Boolean.getBoolean("inject.diff");

    public void execute() {
        System.out.println("targetFile: " + targetConfigFile);
        System.out.println("sourceFile: " + sourceConfigFile);
        try {
            Map<String, String> config;
            ArrayList<Map> list = ConfigUtils.getConfigMapsFromJSON(sourceConfigFile);
            if (list.size() == 0) {
                System.out.println("No configuration to inject");
            } else {
                //get the last element in the list
                if (injectDiff) {
                    config = ConfigUtils.getMapDiff(list.get(list.size() - 2), list.get(list.size() - 1));
                } else {
                    config = list.get(list.size() - 1);
                }
                ConfigUtils.emptyXML(targetConfigFile);
                ConfigUtils.injectConfig(targetConfigFile, config, System.out, true);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
