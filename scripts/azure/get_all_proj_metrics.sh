#!/bin/bash

TARGET_DIR=$1

if [[ -f $TARGET_DIR ]]; then
    echo "Please specify the target dir"
fi


for f in $(find ${TARGET_DIR} -maxdepth 1 -type d | sed '1d');
do
    echo ""
    echo "======================== Proj: $(echo $f | rev | cut -d'/' -f1 | rev | cut -d'-' -f2) ========================"
    python3 get_proj_key_metrics.py $f
    echo "====================================================================="
    echo ""
done
