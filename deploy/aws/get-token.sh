#!/usr/bin/env bash

set -euo pipefail

REGION="${REGION:-eu-west-1}"
USERNAME="${PRODUCT_SORTER_USERNAME:-juan.pablo.jimenez.esclusa@gmail.com}"
PASSWORD="${PRODUCT_SORTER_PASSWORD:-password}"

POOL_ID=$(aws cloudformation describe-stacks \
  --stack-name product-sorter-cognito --region "${REGION}" \
  --query "Stacks[0].Outputs[?OutputKey=='UserPoolId'].OutputValue" --output text)

CLIENT_ID=$(aws cloudformation describe-stacks \
  --stack-name product-sorter-cognito --region "${REGION}" \
  --query "Stacks[0].Outputs[?OutputKey=='UserPoolClientId'].OutputValue" --output text)

CLIENT_SECRET=$(aws cognito-idp describe-user-pool-client \
  --user-pool-id "${POOL_ID}" \
  --client-id "${CLIENT_ID}" \
  --region "${REGION}" \
  --query 'UserPoolClient.ClientSecret' --output text)

SECRET_HASH="$(echo -n "${USERNAME}${CLIENT_ID}" | openssl dgst -sha256 -hmac "${CLIENT_SECRET}" -binary | base64)"

echo "======================================"
echo -e "- POOL_ID=${POOL_ID} \n- CLIENT_ID=${CLIENT_ID} \n- CLIENT_SECRET=${CLIENT_SECRET} \n- SECRET_HASH=${SECRET_HASH}"
echo "======================================"

TOKEN="$(aws cognito-idp admin-initiate-auth \
  --user-pool-id "${POOL_ID}" \
  --client-id "${CLIENT_ID}" \
  --auth-flow ADMIN_USER_PASSWORD_AUTH \
  --auth-parameters "USERNAME=${USERNAME},PASSWORD=${PASSWORD},SECRET_HASH=${SECRET_HASH}" \
  --region "${REGION}" \
  --query 'AuthenticationResult.AccessToken' --output text)"

echo "======================================"
echo "TOKEN=${TOKEN}"
echo "======================================"
