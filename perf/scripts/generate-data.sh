#!/bin/bash

set -euo pipefail

COUNT=${1:?Usage: generate-data.sh <count> [output_file]}
OUTPUT=${2:-/tmp/perf-products.jsonl}

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

CLASSPATH="$PROJECT_DIR/adapter-persistence/target/test-classes"
for jar in jackson-databind jackson-core jackson-annotations; do
  found=$(find "$HOME/.m2/repository/com/fasterxml/jackson" -name "${jar}-*.jar" ! -name "*-sources.jar" ! -name "*-javadoc.jar" 2>/dev/null | sort -V | tail -1)
  if [ -n "$found" ]; then
    CLASSPATH="$CLASSPATH:$found"
  fi
done

echo "Generating $COUNT products to $OUTPUT ..."
java -cp "$CLASSPATH" \
  dev.jpje.productsorter.adapter.persistence.mongo.datagen.ProductDataGenerator \
  "$COUNT" "$OUTPUT"

echo "Done: $(wc -l < "$OUTPUT") lines written."
