#!/usr/bin/env bash
set -euo pipefail

if [[ -f ${HOME}/.docker/config.json && -e /usr/bin/jq ]]; then
    CREDS=($(/usr/bin/jq -r .auths.\"docker.digital.homeoffice.gov.uk\".auth ${HOME}/.docker/config.json | base64 -d | tr ':' '\n'))
    export ARTIFACTORY_USERNAME=${CREDS[0]:-}
    export ARTIFACTORY_PASSWORD=${CREDS[1]:-}
fi

function cleanup() {
    [[ -f .drone.yml.tmp ]] && rm -f .drone.yml.tmp
}

trap cleanup EXIT
grep -v DOCKER_HOST= .drone.yml > .drone.yml.tmp

CONTEXT=$(kubectl config current-context)
USER=$(kubectl config view -o json | jq -r ".contexts[]|select(.name==\"${CONTEXT}\")|.context.user")
CLUSTER=$(kubectl config view -o json | jq -r ".contexts[]|select(.name==\"${CONTEXT}\")|.context.cluster")
export KUBE_TOKEN_NOT_PROD=$(kubectl config view -o json | jq -r ".users[]|select(.name==\"${USER}\")|.user.token")
export KUBE_SERVER_NOT_PROD=$(kubectl config view -o json | jq -r ".clusters[]|select(.name==\"${CLUSTER}\")|.cluster.server")
export GIT_PRIVATE_KEY=$(cat ~/.ssh/id_rsa)

drone exec \
    --local \
    --build-event=push \
    --volumes /var/run/docker.sock:/var/run/docker.sock \
    .drone.yml.tmp
