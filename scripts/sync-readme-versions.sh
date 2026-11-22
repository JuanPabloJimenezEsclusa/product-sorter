#!/usr/bin/env bash
# Sync versions documented in README.md with the values declared in pom.xml.
set -euo pipefail

root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
pom="$root/pom.xml"
readmes=("$root/README.md")

prop() { sed -n "s:.*<$1>\([^<]*\)</$1>.*:\1:p" "$pom" | head -1; }

# The Spring Boot version is the parent version, not a property.
parent_version=$(awk '/<parent>/{p=1} p && /<version>/{print; exit}' "$pom" |
  sed 's:.*<version>\([^<]*\)</version>.*:\1:')

java_version=$(prop java.version)
boot_minor=$(cut -d. -f1-2 <<<"$parent_version")

# There is no Maven wrapper; the supported Maven floor is the enforcer range's
# lower bound, e.g. <version>[3.9.4,4.0.0)</version>.
maven_range=$(awk '/<requireMavenVersion>/{p=1} p && /<version>/{print; exit}' "$pom")
maven_minor=$(sed -n 's:.*\[\([0-9]*\.[0-9]*\).*:\1:p' <<<"$maven_range")

[[ -n $parent_version && -n $java_version && -n $maven_minor ]] || {
  echo "cannot read versions from $pom" >&2
  exit 1
}

# Each pair is "literal anchor in the doc" -> "value from the pom". The anchor
# keeps the replacement away from unrelated numbers; the version right after it
# is fully replaced.
sync() {
  local file=$1 anchor=$2 value=$3
  perl -0777 -pi -e "s/\Q$anchor\E[0-9]+(?:\.[0-9]+)*/$anchor$value/g" "$file"
}

for file in "${readmes[@]}"; do
  before=$(cksum <"$file")
  sync "$file" 'Java ' "$java_version"
  sync "$file" 'JDK ' "$java_version"
  sync "$file" 'Spring%20Boot-' "$boot_minor"
  sync "$file" 'Spring Boot ' "$boot_minor"
  sync "$file" 'Maven ' "$maven_minor"
  if [[ $before == "$(cksum <"$file")" ]]; then
    echo "$(basename "$file"): already up to date"
  else
    echo "$(basename "$file"): updated"
  fi
done

echo "pom: Java $java_version, Spring Boot $parent_version (docs show $boot_minor); maven enforcer floor: $maven_minor"
