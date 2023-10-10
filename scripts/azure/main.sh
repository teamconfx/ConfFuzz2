#!/bin/bash

# The main entry to download and parse results from azure

ROUND=$1
OUTPUT_DIR=$2

if [[ -z $ROUND ]]; then
    echo 'Plase set fuzzing round to download'
    exit 1
fi

if [[ -z $OUTPUT_DIR ]]; then
    echo 'Please set output dir to save results'
    exit 1
fi

if [[ ! -d $OUTPUT_DIR ]]; then
    echo 'Please set correct output dir, current: ${OUTPUT_DIR}'
    exit 1
fi


# 1. Download from azure

echo '===== Download From Azure Storage ====='
python3 download_containers.py $ROUND $OUTPUT_DIR
echo '===== Finish Download ====='

# 2. Unzip the downloaded files

echo '===== Unzip Azure Results ====='
bash unzip_all.sh $OUTPUT_DIR/r$ROUND
echo '===== Finish Unzip ====='

# 3. Parse the results

echo '===== Parse Azure Results ====='
echo ${OUTPUT_DIR}/r${ROUND}
echo ${OUTPUT_DIR}/r${ROUND}-output

bash parse_all.sh ${OUTPUT_DIR}/r${ROUND} ${OUTPUT_DIR}/r${ROUND}-output 
echo '===== Finish Parsing ====='

