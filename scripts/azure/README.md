# README

## Main Entry for Parsing Results from Azure
Use `main.sh` to download and parse results from Azure.
```bash
$ bash main.sh ${AZURE_ROUND_NUM} ${OUTPUT_DIR}
```

## Only Parse Results without Download
Use `parse_all.sh` to parse all results from Azure without redownload raw data.
```bash
$ bash parse_all.sh ${AZURE_RAW_DATA_DIR} ${OUTPUT_DIR}
```

## Get Key Metrics Printed from Parsed Azure Results
To do this step, you need to first run above `main.sh` or `parse_all.sh` to parse all raw data into csv files.

Then use `get_all_proj_metrics.sh` or ` get_proj_key_metrics.py` to get the fuzzing metrics printed.
```bash
# print all metrics
$ bash get_all_proj_metrics.sh ${PARSED_CSV_DIR}

# print one project metrics
$ python3 get_proj_key_metrics.py ${PARSED_CSV_DIR}
```

## Compare Coverage Information Between Modes
There are two types of mode comparison, one is comparing Confuzz with JQF and Random, the other is comparing two rounds of Confuzz.

To compare Confuzz coverage information with JQF and Random, use `compare_cov.py`.
```bash
# Compare Confuzz Cov with JQF and Random
$ python3 compare_cov.py ${CONFUZZ_CSV_DIR} ${JQF_CSV_DIR} ${RANDOM_CSV_DIR}
```

To compare two different rounds of Confuzz coverage, use `compare_confuzz_cov.py`.
```bash
# Compare two rounds of Confuzz cov
$ python3 compare_confuzz_cov.py ${NEW_ROUND_CSV_DIR} ${OLD_ROUND_CSV_DIR}
```

## Draw Coverage Figures
To draw the figures of coverage comparison results, use `plot_cov_figure.py`. This script will generate one json file and one pdf file for each projects.
```bash
$ python3 plog_cov_figure.py ${AZURE_DATA_DIR} ${CONFUZZ_ROUND_NUM} ${JQF_ROUND_NUM} ${RANDOM_ROUND_NUM}
```

# Post Process Scripts
please use python 3.11
## auxiliary and infrastructures
All infrastructure classes are in `infra.py`, including throwables, failures and stuff. 
You can find simple auxiliary functions in `util.py` and complicated ones in `infra.py`.
Please change `infra.py` if you want to extend/modify anything such that 

## check beginning cov stability
use `calculate_coverage_diff.py`
TLDR:
```python
$ python calculate_coverage_diff.py 7253,7271,7272
```

## check crashed tests 
use `calculate_crashed.py`, only accepts runs that starts from default
TLDR:
```python
$ python calculate_crashed.py 7253,7271,7272
```
Output to `metadata/` with three types: crashedVM, crashedInTheMiddle and defaultFailures. The three types mean that the VM crashed, the test crashed halfway or the default fails. 
The script dump both numbers and the tests.

PS: kinda deprecated, need updating

## calculate effectiveness for FP filter
use `calculate_fp_filter.py` for tables and `plot_fp_filter.py` for figures.
TLDR:
```python
$ python calculate_fp_filter.py 8221
$ python plot_fp_filter.py 8221
```
Output to `meta_fp_filter` for the tables or `fp_filter_figures` for the figures. 
Outputting table supports different filtering methods, while figure does not (needs update).

## calculate eventually bug-finding ability
use `calculate_unique_failure.py` and `plot_unique_failure.py` for table and figure respectively
TLDR:
```
$ python calculate_unique_failure.py 7253,7290,7291,7271,7272
$ python plot_unique_failure.py confuzz=7253,7290 jqf=7292 random=7293
```
The tables are output to `meta_failures` and have more detailed info about #Bugs, FP rate and so on.
The figure is output to `failures.pdf` and only have info about the number of unique failure (averaged) for different tools on each project.

That should be all you need for basic evaluation. 
Don't really worry about some of the errors in diagnostics (since some of the typings are shitty), and add more typing hints if you want to extend anything. 
