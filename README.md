# Configuration Fuzzing

## Add Confuzz to your project
Follow the instruction to add Confuzz to your project: [HOW_TO_PORT](HOW_TO_PORT.md).

## Identify Configuration Tests

Identify all configuration tests in your project using the Confuzz `identify` goal:

```
mvn confuzz:identify
```

PS: You need to **compile** your module before running this goal.
The goal will output two CSV files, one called `confuzz_identify_ctest.csv` that contains all identified configuration tests,
and one called `confuzz_identify_param.csv` that contains all configuration parameters that are accessed by all ctests.

## Fuzz a configuration test

Fuzz a configuration using the `fuzz` goal:

```
mvn confuzz:fuzz
-Dconfuzz.generator=<G>
-Dmeringue.testClass=<C> 
-Dmeringue.testMethod=<M>
-DregexFile=<R>
[-Dmeringue.duration=<D>]
[-Dmeringue.outputDirectory=<O>]
[-Dmeringue.javaOptions=<P>]
[-DonlyCheckDefault]
[-Dconfuzz.debug]
```

Where:

* \<G\> is the fully-qualified name of the configuration generator class.
* \<C\> is the fully-qualified name of the test class.
* \<M\> is the name of the test method.
* \<R\> is the path of the configuration regex file.
* \<D\> is the maximum amount of time to execute the fuzzing campaign for specified in the ISO-8601 duration format (
  e.g., 2 days, 3 hours, and 4 minutes is "P2DT3H4M"). The default value is one day.
* \<O\> is the path of the directory to which the output files should be written.
  The default value is ${project.build.directory}/meringue.
* \<P\> is a list of Java command line options that should be used for test JVMs.
* The presence of -DonlyCheckDefault checks whehter the current test can pass under default configuration in a same JVM twice.
* The presence of -Dconfuzz.debug indicates that campaign JVMs should suspend and wait for a debugger to attach
  on port 5005. By default, campaign JVMs do not suspend and wait for a debugger to attach.


## Debug the root-cause parameter set of failures
Debug a configuration using the `debug` goal. 
This goal produces failures.json file under `${meringue.outputDirectory}`.

```
mvn confuzz:debug
-Dconfuzz.generator=<G>
-Dmeringue.testClass=<C> 
-Dmeringue.testMethod=<M>
-DinjectConfigFile=<I>
[-Dmeringue.outputDirectory=<O>]
```

Where:

* \<G\> is the fully-qualified name of the configuration generator class.
* \<C\> is the fully-qualified name of the test class.
* \<M\> is the name of the test method.
* \<I\> is the path of the configuration file that should be injected.
* \<O\> is the path of the directory to which the output files should be written.
  The default value is ${project.build.directory}/meringue.

## Calculate coverage of a fuzzing campaign
### Dump Jacoco Exec File and generate coverage trend csv file
Use the `coverage` goal to calculate the coverage of a fuzzing campaign. 

This goal produces a coverage directory under ${meringue.outputDirectory} that contains Jacoco exec file and 
a coverage_trend.csv file that contains the coverage trend of the campaign.

```
mvn confuzz:coverage
-Dconfuzz.generator=<G>
-Dmeringue.testClass=<C> 
-Dmeringue.testMethod=<M>
[-Dconfuzz.debug]
```
Where:

* \<G\> is the fully-qualified name of the configuration generator class.
* \<C\> is the fully-qualified name of the test class.
* \<M\> is the name of the test method.

### Merge Jacoco Exec File
Use the `dumpCov` goal to dump the merged jacoco exec file of a fuzzing campaign. You can later use
Jacoco to generate the coverage report.

```
mvn confuzz:dumpCov
-Ddata.dir=<D>
-Doutput.dir=<O>
```
Where:
* \<D\> is the path of the directory that contains the `.jed` files of the campaign.
* \<O\> is the path of the directory to which the merged jacoco exec file should be written.

## Check configuration regular expression can be used
```
mvn confuzz:regex-check
-DregexFile=<R>
```
Where:
* \<R\> is the path of the configuration regex file.

## Other goals
Other goals can be found in the [GOALS](GOALS.md).


## Bug and FP definition
You can find our bug and false positive definition [here](BUG_DEFINITION.md).
