#!/bin/bash

set -euo pipefail

FILE=${1:?Usage: import-data.sh <jsonl_file>}
MONGODB_URI=${MONGODB_URI:-mongodb://localhost:27017/productsorter}
COLLECTION=${COLLECTION:-products}
BATCH=${BATCH_SIZE:-100000}
PAUSE=${BATCH_PAUSE:-5}

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
  for attempt in 1 2 3; do
    if mongoimport \
      --uri="${MONGODB_URI}" \
      --collection "${COLLECTION}" \
      --file "${chunk}" \
      --numInsertionWorkers 1 \
      --batchSize 5000; then
      break
    fi
    if [ "$attempt" -lt 3 ]; then
      echo "    attempt ${attempt} failed, retrying ..."
      sleep 2
    else
      echo "    mongoimport failed after 3 attempts"
      exit 1
    fi
  done
  sleep "${PAUSE}"
done
rm -rf "${chunks_dir}"

echo "Import complete."

echo "Recreating indexes ..."
mongosh --quiet "$MONGODB_URI" --eval "
  db.${COLLECTION}.createIndex({salesUnits: -1, _id: -1});
  print('Index {salesUnits:-1, _id:-1} created');
"
