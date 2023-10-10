#/bin/bash

AZURE_DIR=$1
OUTPUT_DIR=$2

# parse all projects in the azure directory
for f in $(find ${AZURE_DIR} -maxdepth 1 -type d | sed '1d');
do
    PROJ_DIR=$f/$(ls $f);
    (python3 parse.py $PROJ_DIR)
done

# move all csv files to a single directory
bash move_csv_to_dir.sh $AZURE_DIR $OUTPUT_DIR

# change all delimiter from @;@ to `
bash change_delimiter.sh $OUTPUT_DIR
