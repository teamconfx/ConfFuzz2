#!/bin/bash

INPUT_DIR=$1
TARGET_DIR=$2

rm -rf $TARGET_DIR
mkdir -p $TARGET_DIR
for f in $(find ${INPUT_DIR} -name "*.csv" | grep -v "_input")
do
    FILE_PATH=$(echo $f | rev | cut -d'/' -f1,2 | rev)
    SUB_DIR=$(echo $f | rev | cut -d'/' -f2 | rev)
    mkdir -p ${TARGET_DIR}/${SUB_DIR}
    cp $f ${TARGET_DIR}/${FILE_PATH}
done


