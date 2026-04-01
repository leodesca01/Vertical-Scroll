#!/usr/bin/env bash
set -euo pipefail

# Find a Gradle binary: prefer system 'gradle', fall back to wrapper dists
if command -v gradle &>/dev/null; then
    GRADLE="gradle"
else
    GRADLE=$(find ~/.gradle/wrapper/dists -name "gradle" -path "*/bin/gradle" 2>/dev/null | sort -V | tail -1)
    if [ -z "$GRADLE" ]; then
        echo "ERROR: No Gradle binary found. Install Gradle or add it to PATH." >&2
        exit 1
    fi
fi

MOD_VERSION=$(grep '^mod_version=' gradle.properties | cut -d= -f2 | tr -d '[:space:]')
BASE_NAME=$(grep '^archives_base_name=' gradle.properties | cut -d= -f2 | tr -d '[:space:]')

mkdir -p dist

for TARGET in "1.21.x" "26.1.x"; do
    echo "=== Building $TARGET ==="
    "$GRADLE" clean build -Pmc_target="$TARGET" --no-daemon -q
    cp "build/libs/${BASE_NAME}-${MOD_VERSION}.jar" "dist/${BASE_NAME}-${MOD_VERSION}+${TARGET}.jar"
    echo "-> dist/${BASE_NAME}-${MOD_VERSION}+${TARGET}.jar"
done

echo ""
echo "Done. 2 JARs written to dist/"
