#!/usr/bin/env bash

##
# Convenience script for building and deploying Docker images for BioModels.
##

# fail the script in case of errors
set -eu -o pipefail

cd "$(dirname "$0")"

function errorOut {
    MSG="$1"
    CODE="$2"
    echo "$MSG"
    [[ ( -n "$CODE" ) && ( $CODE -ne 0 ) ]] && return $CODE
}

function usage {
    errorOut "Usage: $0 <user_id> <username> <group_id> <group> <jpda_port>" 1
}

function requiredParam {
    PARAM="${1?`usage`}" # if missing, show usage
    NAME="$2"
    if [ -z "$PARAM" ]; then
        errorOut "$NAME argument missing"
        usage
    fi
    echo $1
}

[[ $# -lt 5 ]] && usage

# build args
USER_ID=${1?`usage`}
USERNAME=${2?`usage`}
GID=${3?`usage`}
GROUP=${4?`usage`}
JPDA_PORT=${5?`usage`}

# image metadata
REGISTRY_URL=dockerhub.ebi.ac.uk/
REGISTRY_USERNAME="biomodels"
IMAGE_NAME=webapp
VERSION=5.2
BASE_STAGE=base
PROD_STAGE=prod
DEBUG_STAGE=debug

# BuildKit is smarter than the legacy Docker build
# See https://docs.docker.com/develop/develop-images/build_enhancements/#to-enable-buildkit-builds
# for more information about BuildKit, including how to make it the default build strategy for
# your Docker installation.
export DOCKER_BUILDKIT=1

function getStageTag {
    STAGE=$1
    [[ -e "$STAGE" ]] && errorOut "usage: getStageTag <stage>" 1
    echo "${REGISTRY_URL}${REGISTRY_USERNAME}/${IMAGE_NAME}:${VERSION}-${STAGE}"
}

function doBuild {
    STAGE="${1?$"Build stage required - either prod or debug"}"
    TAG="$2"

    time DOCKER_BUILDKIT=1 docker build \
        --build-arg UID=$USER_ID \
        --build-arg USERNAME=$USERNAME \
        --build-arg GID=$GID \
        --build-arg GROUP=$GROUP \
        -t "$TAG" \
        --target=$STAGE \
        .
}

function build {
    STAGE="$1"
    TAG="$(getStageTag "$STAGE")"
    doBuild "$STAGE" "$TAG"
}

function buildAndPush {
    STAGE="$1"
    TAG="$(getStageTag "$STAGE")"
    doBuild "$STAGE" "$TAG"
    docker push "$TAG"
}

# run this on the host directly, as the container would first
# need to pull the dependencies, which is sloooow
time ./grailsw --offline prod war

echo 'Building prod and debug images...'
time buildAndPush $PROD_STAGE
time buildAndPush $DEBUG_STAGE
echo '...done'
