#!/bin/bash

set -e
python3 replay_failure_inputs.py hdfs true hdfs_debug_list ../../../azure-confuzz-data/r3-hdfs ./r3-hdfs-debug