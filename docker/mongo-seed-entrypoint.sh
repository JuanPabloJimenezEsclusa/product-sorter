#!/bin/bash
set -e

if [ -f /seed/products.json ]; then
  echo "Seeding with product data..."
  mongoimport --db productsorter --collection products \
    --file /seed/products.json --drop 2>&1 | tail -1
  echo "Seed complete."
fi
