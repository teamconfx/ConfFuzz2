#!/bin/bash

FUZZ_APP_DIR=$1
#CONFUZZ_BRANCH=$2
#TAG=$3
TAG=$2
# if FUZZ_APP_DIR is not set, return error
if [ -z "$FUZZ_APP_DIR" ]; then
  echo "Please set FUZZ_APP_DIR"
  exit 1
fi

# if CONFUZZ_BRANCH is not set, send info
# if [ -z "$CONFUZZ_BRANCH" ]; then
#   echo "Please set CONFUZZ_BRANCH"
#   exit 1
# fi

if [ -z "$TAG" ]; then
  echo "Please set TAG"
  exit 1
fi

# if FUZZ_APP_DIR is not exist or not a directory, return error
if [ ! -d "$FUZZ_APP_DIR" ]; then
  echo "${FUZZ_APP_DIR} is not a directory"
  exit 1
fi
#docker build --build-arg FUZZ_APP_DIR=${FUZZ_APP_DIR} --build-arg CONFUZZ_BRANCH=${CONFUZZ_BRANCH} --no-cache -t shuaiwang516/confuzz-image:${TAG} -f Dockerfile .
docker build --build-arg FUZZ_APP_DIR=${FUZZ_APP_DIR} --no-cache -t shuaiwang516/confuzz-image:${TAG} -f Dockerfile .

