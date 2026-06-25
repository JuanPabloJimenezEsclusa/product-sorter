#!/usr/bin/env bash

# Usage: ./stop.sh removeImages

set -o errexit
set -o errtrace
set -o nounset
if [[ "${DEBUG:-}" == "true" ]]; then set -o xtrace; fi

cd "$(dirname "$0")"

if [[ -f ../.env ]]; then
  set -o allexport
  source ../.env
  set +o allexport
fi

SEPARATOR="\n##################################################\n"
REGION="${REGION:-eu-west-1}"
REMOVE_IMAGES="${1:-}"
HOSTED_ZONE_ID="$(aws route53 list-hosted-zones --query 'HostedZones[?Name==`jpje.net.`].Id' --output text | sed 's|/hostedzone/||')"

__delete_stack() {
  local stack="$1"
  if aws cloudformation describe-stacks --stack-name "${stack}" --region "${REGION}" >/dev/null 2>&1; then
    echo "   Deleting: ${stack}"
    aws cloudformation delete-stack --stack-name "${stack}" --region "${REGION}"
    aws cloudformation wait stack-delete-complete --stack-name "${stack}" --region "${REGION}"
  fi
}

__delete_log_groups() {
  local log_groups=(
    "/ecs/product-sorter"
  )
  for lg in "${log_groups[@]}"; do
    aws logs delete-log-group --log-group-name "${lg}" --region "${REGION}" 2>/dev/null || true
    echo "Deleted log group: ${lg}"
  done
}

__cleanup_ecr() {
  local repo="product-sorter"
  echo -e "${SEPARATOR} 🗑️ Clean up ECR${SEPARATOR}"

  if aws ecr describe-repositories --repository-names "${repo}" --region "${REGION}" >/dev/null 2>&1; then
    aws ecr batch-delete-image \
      --repository-name "${repo}" \
      --image-ids "$(aws ecr list-images --repository-name "${repo}" --region "${REGION}" --query 'imageIds[*]' --output json)" \
      --region "${REGION}" 2>/dev/null || true
    aws ecr delete-repository --repository-name "${repo}" --region "${REGION}" 2>/dev/null || true
    echo "Deleted ECR repository: ${repo}"
  fi
}

__cleanup_route53_validation() {
  local hosted_zone_id="${HOSTED_ZONE_ID:-}"
  local domain="${DOMAIN_NAME:-api.product-sorter.com}"
  echo -e "${SEPARATOR} 🗑️ Clean up Route53 validation CNAMEs${SEPARATOR}"

  aws route53 list-resource-record-sets \
    --hosted-zone-id "${hosted_zone_id}" \
    --query "ResourceRecordSets[?contains(Name, '${domain}') && Type=='CNAME']" \
    --output json | jq -c '.[]' 2>/dev/null | while read -r record; do
      [[ -z "${record}" ]] && continue
      aws route53 change-resource-record-sets \
        --hosted-zone-id "${hosted_zone_id}" \
        --change-batch "{\"Changes\":[{\"Action\":\"DELETE\",\"ResourceRecordSet\":${record}}]}" \
        >/dev/null 2>&1 || true
      echo "Deleted CNAME: $(echo "$record" | jq -r '.Name')"
    done
}

__removeImages() {
  echo -e "${SEPARATOR} 🧹 Clean up ${SEPARATOR}"
  docker volume prune --force --all

  if [[ "${REMOVE_IMAGES}" == "removeImages" ]]; then
    docker images --filter reference='*/*product-sorter*' --filter reference='*product-sorter*' \
      --format '{{.Repository}}:{{.Tag}}' | xargs -I {} docker rmi -f {}
  else
    echo -e "🚧 Skip remove images"
  fi
}

main() {
  echo "Init ${0##*/}"
  echo -e "${SEPARATOR}🗑️ Tear down${SEPARATOR}"

  __delete_stack "product-sorter-app"
  __delete_stack "product-sorter-secrets"
  __delete_stack "product-sorter-cognito"

  __delete_log_groups
  __cleanup_ecr
  __cleanup_route53_validation
  __removeImages
  echo "Done ${0##*/}"
}

time main
