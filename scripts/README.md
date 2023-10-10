# Scripts for Fuzzing Configuration Tests

## Fuzzing a single configuration test

```python
$ python3 fuzz_single_test.py <mode> <build> <test_name> <app_dir> <output_dir>  <fuzzing_duration> <total_timeout> <JAVA9+_HOME>
```

Where:

* \<mode\> : 'fuzz', 'check'; must run 'fuzz' before 'check'
* \<build\> : 'true', 'false'; build application or not
* \<test_name\> : test_class#test_method
* \<app_dir\> : directory that contains pom.xml of target application
* \<output_dir\> : directory that stores fuzzing outputs
* \<fuzzing_duration\> : timeout of fuzzing campaign
* \<total_timeout\> : timeout of running mvn, should be slightly larger than <fuzzing_duration>
* \<JAVA9+_HOME\> : requires java 9+

## Fuzzing multiple configuration tests

```python
$ python3 fuzz_test_from_test_list.py <mode> <test_list_file> <app_dir> <output_dir>  <fuzzing_duration> <total_timeout> <JAVA9+_HOME>
```

Where:

* \<mode\> : 'fuzz', 'check'; must run 'fuzz' before 'check'
* \<test_list_file\> : file path that contains the list of fuzzing tests
* \<app_dir\> : directory that contains pom.xml of target application
* \<output_dir\> : directory that stores fuzzing outputs
* \<fuzzing_duration\> : timeout of fuzzing campaign
* \<total_timeout\> : timeout of running mvn, should be slightly larger than <fuzzing_duration>
* \<JAVA9+_HOME\> : requires java 9+

## Fuzzing tests on Azure

```python
$ python3 fuzz_on_azure.py <project_name> <test_list> <output_dir> <fuzzing_duration>
```

Where:

* \<project_name\> : project name; supported project can be found in ./config.py
* \<test_list\> : list of tests to fuzz, separated by ";"
* \<output_dir\> : directory that stores fuzzing outputs
* \<fuzzing_duration\> : timeout of fuzzing campaign

PS: This script should be called by a Azure VM Node.