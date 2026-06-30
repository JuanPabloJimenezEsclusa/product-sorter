#!/bin/bash

set -euo pipefail

FILE=${1:?Usage: import-data.sh <jsonl_file>}
MONGODB_URI=${MONGODB_URI:-mongodb://localhost:27017/productsorter}
COLLECTION=${COLLECTION:-products}
BATCH=${BATCH_SIZE:-250000}
PAUSE=${BATCH_PAUSE:-3}

if [ ! -f "${FILE}" ]; then
  echo "File not found: ${FILE}"
  exit 1
fi

echo "Dropping collection ${COLLECTION} ..."
mongosh --quiet "${MONGODB_URI}" --eval "db.${COLLECTION}.drop()" 2>/dev/null || true
sleep 1

total=$(wc -l < "${FILE}")
echo "Importing ${total} docs into MongoDB (${MONGODB_URI}, collection=${COLLECTION}) in batches of ${BATCH} ..."

chunks_dir="$(mktemp -d)"
split -l "${BATCH}" "${FILE}" "${chunks_dir}/chunk_"

for chunk in "${chunks_dir}"/chunk_*; do
  count=$(wc -l < "${chunk}")
  echo "  importing ${count} docs ..."
  mongoimport \
    --uri="${MONGODB_URI}" \
    --collection "${COLLECTION}" \
    --file "${chunk}"
  sleep "${PAUSE}"
done
rm -rf "${chunks_dir}"

echo "Import complete."

echo "Recreating indexes ..."
mongosh --quiet "$MONGODB_URI" --eval "
  db.${COLLECTION}.createIndex({salesUnits: -1});
  print('Index salesUnits:-1 created');
"
