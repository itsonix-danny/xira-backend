#!/bin/bash

set -a
source .env
set +a

BRANCH_NAME=${1:-$(git rev-parse --abbrev-ref HEAD)}

mvn verify -DskipTests=true sonar:sonar -Dsonar.token="$SONAR_TOKEN" -Dsonar.branch.name="$BRANCH_NAME"
