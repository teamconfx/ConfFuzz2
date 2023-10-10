## How to use Confuzz for a new project

### 1. Add Confuzz to your project pom.xml
Adding the following to your pom.xml will add Confuzz to your project.

```xml
<dependencies>
    ...
    <dependency>
        <groupId>edu.illinois.confuzz</groupId>
        <artifactId>confuzz-guidance</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <exclusions>
            <exclusion>
                <groupId>com.squareup.okio</groupId>
                <artifactId>okio</artifactId>
            </exclusion>
            <exclusion>
                <groupId>org.ow2.asm</groupId>
                <artifactId>asm-commons</artifactId>
            </exclusion>
            <exclusion>
                <groupId>com.google.collections</groupId>
                <artifactId>google-collections</artifactId>
            </exclusion>
        </exclusions>
    </dependency>
    <dependency>
        <groupId>com.pholser</groupId>
        <artifactId>junit-quickcheck-generators</artifactId>
        <version>1.0</version>
        <scope>test</scope>
    </dependency>
    ...
</dependencies>

<build>
    <plugins>
        ...
        <plugin>
            <groupId>edu.illinois.confuzz</groupId>
            <artifactId>confuzz-maven-plugin</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </plugin>
        ...
    </plugins>
</build>
```

### 2. Instrument your project configuration API with Confuzz
Confuzz needs to track your configuration parameter get/set API for dynamic input generation.
To do this, you need to add two tracking methods in your configuration API.

In the following example, `ConfigTracker.track(String paramName, String paramValue, boolean isSet)` is added to 
track the configuration parameter get/set API.
```java
public void trackConfig(String ctestParam, String result, boolean isSet) {
    ConfigTracker.track(ctestParam, result, isSet);
}

public String get(String name) {
    ...
    trackConfig(ctestParam, result, false);
    ...
}

public void set(String name, String value, boolean notGenerator) {
    ...
    trackConfig(name, value, notGenerator);
    ...
}
```

You also need a wrapped setter that can be used by ConfuzzGenerator but not track any configuration parameter.
```java
public void generatorSet(String name, String value) {
    ConfigTracker.trackGenerated(name, value);
    set(name, value, null, false);   // false means this is set by the generator
}
```

### 3. Write a configuration generator
Confuzz needs to generate configuration inputs for your project. To do this, you need to write a configuration generator.
Generator is implemented with junit-quickcheck.


### 4. Write a configuration regex json file
Confuzz needs user to define the basic format and potential valid values for configuration parameters.
The regex file contains regular expression that describes the pattern or supported value options of configuration parameters.

### Issues
If you meet the NoSuchMethodError issue:
```java
java.lang.NoSuchMethodError: 'void org.junit.runners.BlockJUnit4ClassRunner.<init>(org.junit.runners.model.TestClass)'
	at com.pholser.junit.quickcheck.runner.JUnitQuickcheck.<init>(JUnitQuickcheck.java:71)
```
Please upgrade your Junit to 4.13.2 or higher. 
We have tested confuzz with Junit 4.13.2.
