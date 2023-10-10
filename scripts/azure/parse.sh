#!/bin/bash

# Main entry for parsing fuzzing campaign results
# Usage: bash parse.sh <results_dir>

RESULTS_DIR=$1

# parse fuzzing campaign summary file
python3 parse_summary.py ${RESULTS_DIR}

# parse failure file
python3 parse_failure_csv.py ${RESULTS_DIR}

# generate unique failure file
python3 remove_duplicated.py ${RESULTS_DIR}/failures-output.csv ${RESULTS_DIR}/unique_failures.csv

# generate exception summary file
python3 get_exceptions.py ${RESULTS_DIR}/unique_failures.csv ${RESULTS_DIR}/exception_summary.csv
