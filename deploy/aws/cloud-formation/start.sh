#!/usr/bin/env bash

# Usage: ./start.sh buildImage

set -o errexit
set -o errtrace
set -o nounset
if [[ "${DEBUG:-}" == "true" ]]; then set -o xtrace; fi

cd "$(dirname "$0")"
workspace="$(pwd)"

if [[ -f ../.env ]]; then
  set -o allexport
  source ../.env
  set +o allexport
fi

SEPARATOR="\n##################################################\n"
BUILD_IMAGE="${1:-}"
REGION="${REGION:-eu-west-1}"
AWS_ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
ECR_REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"
ECR_REPOSITORY="${ECR_REPOSITORY:-product-sorter}"
ECR_IMAGE_TAG="${ECR_IMAGE_TAG:-1.0.0}"
IMAGE_URI="${IMAGE_URI:-"${ECR_REGISTRY}/${ECR_REPOSITORY}:${ECR_IMAGE_TAG}"}"

__require_aws_cli() {
  if ! command -v aws &>/dev/null; then
    curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
    unzip awscliv2.zip
    sudo ./aws/install --update
    rm -rf awscliv2.zip aws
    aws --version
  fi
}

__require_mongo_uri() {
  if [[ -z "${MONGODB_URI:-}" ]]; then
    echo "MONGODB_URI is required. Set it in deploy/aws/.env or as env var."
    exit 1
  fi
}

__require_hosted_zone() {
  if [[ -z "${HOSTED_ZONE_ID:-}" ]]; then
    # shellcheck disable=SC2016
    HOSTED_ZONE_ID="$(aws route53 list-hosted-zones --query 'HostedZones[?Name==`jpje.net.`].Id' --output text | sed 's|/hostedzone/||')"
    if [[ -z "${HOSTED_ZONE_ID:-}" ]]; then
      echo "Hosted zone jpje.net not found in Route53."
      echo "Set HOSTED_ZONE_ID manually or create the zone first."
      exit 1
    fi
    export HOSTED_ZONE_ID
  fi
}

__validate() {
  echo -e "${SEPARATOR}🔍 Validate environment${SEPARATOR}"
  __require_aws_cli
  __require_mongo_uri
  __require_hosted_zone
}

__create_ecr_repository() {
  echo -e "${SEPARATOR}📦 Create ECR repository${SEPARATOR}"
  aws ecr describe-repositories --repository-names "${ECR_REPOSITORY}" --region "${REGION}" >/dev/null 2>&1 || \
    aws ecr create-repository --repository-name "${ECR_REPOSITORY}" --region "${REGION}"
}

__build_image() {
  echo -e "${SEPARATOR}🐳 Build Docker image${SEPARATOR}"
  docker build -t "${ECR_REPOSITORY}:${ECR_IMAGE_TAG}" "${workspace}/../../.."
}

__login_ecr() {
  echo -e "${SEPARATOR}🔑 Login to ECR${SEPARATOR}"
  aws ecr get-login-password --region "${REGION}" | \
    docker login --username AWS --password-stdin "${ECR_REGISTRY}"
}

__push_image() {
  echo -e "${SEPARATOR}⬆️ Push image to ECR${SEPARATOR}"
  docker tag "${ECR_REPOSITORY}:${ECR_IMAGE_TAG}" "${IMAGE_URI}"
  docker push "${IMAGE_URI}"
}

__manage_image() {
  echo -e "${SEPARATOR}🚀 Manage image${SEPARATOR}"
  if [[ "${BUILD_IMAGE}" == "buildImage" ]]; then
    __create_ecr_repository
    __build_image
    __login_ecr
    __push_image
  else
    echo "Using pre-built image: ${IMAGE_URI}"
  fi
}

__deploy_stack_cognito() {
  local stack="product-sorter-cognito"
  echo -e "${SEPARATOR}☁️ — Cognito${SEPARATOR}"

  aws cloudformation deploy \
    --stack-name "${stack}" \
    --template-file 01-cognito.yml \
    --capabilities CAPABILITY_NAMED_IAM \
    --region "${REGION}" \
    --no-fail-on-empty-changeset

  aws cloudformation wait stack-create-complete \
    --stack-name "${stack}" --region "${REGION}"

  JWKS_URI="$(aws cloudformation describe-stacks --stack-name "${stack}" --region "${REGION}" \
    --query "Stacks[0].Outputs[?OutputKey=='JwksUri'].OutputValue" --output text)"
  USER_POOL_ID="$(aws cloudformation describe-stacks --stack-name "${stack}" --region "${REGION}" \
    --query "Stacks[0].Outputs[?OutputKey=='UserPoolId'].OutputValue" --output text)"

  echo "   UserPoolId: ${USER_POOL_ID}"
  echo "   JwksUri:    ${JWKS_URI}"
}

__set_user_password() {
  echo "Init ${FUNCNAME:-} ..."

  PRODUCT_SORTER_USERNAME="${PRODUCT_SORTER_USERNAME:-juan.pablo.jimenez.esclusa@gmail.com}"
  PRODUCT_SORTER_PASSWORD="${PRODUCT_SORTER_PASSWORD:-password}"
  USER_POOL_ID="$(aws cloudformation describe-stacks \
    --stack-name "product-sorter-cognito" \
    --query "Stacks[0].Outputs[?OutputKey=='UserPoolId'].OutputValue" --output text)"

  aws cognito-idp admin-set-user-password \
    --username "${PRODUCT_SORTER_USERNAME}" \
    --password "${PRODUCT_SORTER_PASSWORD}" \
    --user-pool-id "${USER_POOL_ID}" \
    --permanent

  aws cognito-idp admin-add-user-to-group \
    --user-pool-id "${USER_POOL_ID}" \
    --username "${PRODUCT_SORTER_USERNAME}" \
    --group-name operator \
    --region "${REGION}"

  aws cognito-idp admin-add-user-to-group \
    --user-pool-id "${USER_POOL_ID}" \
    --username "${PRODUCT_SORTER_USERNAME}" \
    --group-name admin \
    --region "${REGION}"

  echo "End ${FUNCNAME:-} successfully!"
}

__deploy_stack_secrets() {
  local stack="product-sorter-secrets"
  echo -e "${SEPARATOR}🔐 — Secrets${SEPARATOR}"

  aws cloudformation deploy \
    --stack-name "${stack}" \
    --template-file 02-secrets.yml \
    --parameter-overrides \
      MongoUri="${MONGODB_URI}" \
    --capabilities CAPABILITY_NAMED_IAM \
    --region "${REGION}" \
    --no-fail-on-empty-changeset

  aws cloudformation wait stack-create-complete \
    --stack-name "${stack}" --region "${REGION}"

  SECRET_ARN="$(aws cloudformation describe-stacks --stack-name "${stack}" --region "${REGION}" \
    --query "Stacks[0].Outputs[?OutputKey=='MongoUriSecretArn'].OutputValue" --output text)"

  echo "   MongoUriSecretArn: ${SECRET_ARN}"
}

__deploy_stack_app() {
  local stack="product-sorter-app"
  echo -e "${SEPARATOR}📦 — App ECS${SEPARATOR}"

  aws cloudformation deploy \
    --stack-name "${stack}" \
    --template-file 03-app.yml \
    --parameter-overrides \
      ImageUri="${IMAGE_URI}" \
      MongoUriSecretArn="${SECRET_ARN}" \
      JwksUri="${JWKS_URI}" \
      HostedZoneId="${HOSTED_ZONE_ID}" \
      DomainName="${DOMAIN_NAME:-api.product-sorter.com}" \
    --capabilities CAPABILITY_NAMED_IAM \
    --region "${REGION}" \
    --no-fail-on-empty-changeset

  aws cloudformation wait stack-create-complete \
    --stack-name "${stack}" --region "${REGION}"

  AlbDnsName="$(aws cloudformation describe-stacks --stack-name "${stack}" --region "${REGION}" \
    --query "Stacks[0].Outputs[?OutputKey=='AlbDnsName'].OutputValue" --output text)"
  ServiceUrl="$(aws cloudformation describe-stacks --stack-name "${stack}" --region "${REGION}" \
    --query "Stacks[0].Outputs[?OutputKey=='ServiceUrl'].OutputValue" --output text)"
}

__smoke_test() {
  echo -e "${SEPARATOR}🧪 Smoke test${SEPARATOR}"
  echo "ALB DNS: ${AlbDnsName}"

  if [[ -n "${AlbDnsName:-}" ]]; then
    echo "Waiting for ECS task to become healthy (up to 120s)..."
    for i in $(seq 1 24); do
      if curl -skf "https://${AlbDnsName}/actuator/health" >/dev/null 2>&1; then
        echo "✅ Health check passed"
        curl -sk "https://${AlbDnsName}/actuator/health" | python3 -m json.tool 2>/dev/null || true
        return 0
      fi
      echo -n "."
      sleep 5
    done
    echo " ⚠️ Health check timed out (DNS may still need propagation)"
  fi
}

main() {
  echo "Init ${0##*/}"

  __validate
  __manage_image
  __deploy_stack_cognito
  __set_user_password
  __deploy_stack_secrets
  __deploy_stack_app
  __smoke_test

  echo ""
  echo "Done ${0##*/}"
  echo "Service URL: ${ServiceUrl}"
  echo "Test: curl -sk https://${AlbDnsName}/actuator/health"
}

time main
