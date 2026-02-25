#!/bin/bash
# Build and optionally publish Wave Defense for multiple Minecraft versions
# Usage: ./build-all.sh          (build only)
#        ./build-all.sh publish  (build + upload to Modrinth)

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
VERSIONS_DIR="$SCRIPT_DIR/versions"
PROPS_FILE="$SCRIPT_DIR/gradle.properties"
OUTPUT_DIR="$SCRIPT_DIR/multiversion-jars"

# Require JDK 21
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/Cellar/openjdk@21/21.0.10/libexec/openjdk.jdk/Contents/Home}"
if ! "$JAVA_HOME/bin/java" -version 2>&1 | grep -q '"21\.'; then
    echo "ERROR: JDK 21 required. Set JAVA_HOME to a JDK 21 installation."
    exit 1
fi

# Save original gradle.properties
cp "$PROPS_FILE" "$PROPS_FILE.bak"
trap 'mv "$PROPS_FILE.bak" "$PROPS_FILE"; echo "Restored original gradle.properties"' EXIT

# Read base properties (mod_version, maven_group, etc.)
MOD_VERSION=$(grep "mod_version" "$PROPS_FILE" | cut -d= -f2)
LOADER_VERSION=$(grep "loader_version" "$PROPS_FILE" | cut -d= -f2)
MAVEN_GROUP=$(grep "maven_group" "$PROPS_FILE" | cut -d= -f2)
ARCHIVES_NAME=$(grep "archives_base_name" "$PROPS_FILE" | cut -d= -f2)

mkdir -p "$OUTPUT_DIR"

PUBLISH_MODE="${1:-build}"
FAILED_VERSIONS=""
SUCCESS_COUNT=0

echo "========================================="
echo "  Wave Defense Multi-Version Builder"
echo "  Mod Version: $MOD_VERSION"
echo "  Mode: $PUBLISH_MODE"
echo "========================================="
echo ""

for VERSION_FILE in "$VERSIONS_DIR"/*.properties; do
    [ -f "$VERSION_FILE" ] || continue

    MC_VERSION=$(grep "minecraft_version" "$VERSION_FILE" | cut -d= -f2)
    YARN=$(grep "yarn_mappings" "$VERSION_FILE" | cut -d= -f2)
    FABRIC_API=$(grep "fabric_version" "$VERSION_FILE" | cut -d= -f2)

    echo "--- Building for Minecraft $MC_VERSION ---"

    # Write temporary gradle.properties
    cat > "$PROPS_FILE" << EOF
# Auto-generated for MC $MC_VERSION
minecraft_version=$MC_VERSION
yarn_mappings=$YARN
loader_version=$LOADER_VERSION
mod_version=$MOD_VERSION
maven_group=$MAVEN_GROUP
archives_base_name=$ARCHIVES_NAME
fabric_version=$FABRIC_API
EOF

    # Clean and build
    if "$SCRIPT_DIR/gradlew" clean build -x test 2>&1 | tail -5; then
        JAR_FILE="$SCRIPT_DIR/build/libs/${ARCHIVES_NAME}-${MOD_VERSION}.jar"
        mkdir -p "$OUTPUT_DIR"
        if [ -f "$JAR_FILE" ]; then
            cp "$JAR_FILE" "$OUTPUT_DIR/${ARCHIVES_NAME}-${MOD_VERSION}-mc${MC_VERSION}.jar"
            echo "  -> Built: ${ARCHIVES_NAME}-${MOD_VERSION}-mc${MC_VERSION}.jar"
            SUCCESS_COUNT=$((SUCCESS_COUNT + 1))

            # Publish to Modrinth if requested
            if [ "$PUBLISH_MODE" = "publish" ]; then
                if [ -z "$MODRINTH_TOKEN" ]; then
                    echo "  -> SKIP publish: MODRINTH_TOKEN not set"
                else
                    echo "  -> Publishing to Modrinth..."
                    if "$SCRIPT_DIR/gradlew" modrinth 2>&1 | tail -3; then
                        echo "  -> Published!"
                    else
                        echo "  -> PUBLISH FAILED for $MC_VERSION"
                    fi
                fi
            fi
        else
            echo "  -> ERROR: JAR not found"
            FAILED_VERSIONS="$FAILED_VERSIONS $MC_VERSION"
        fi
    else
        echo "  -> BUILD FAILED for $MC_VERSION"
        FAILED_VERSIONS="$FAILED_VERSIONS $MC_VERSION"
    fi
    echo ""
done

echo "========================================="
echo "  Results: $SUCCESS_COUNT versions built"
if [ -n "$FAILED_VERSIONS" ]; then
    echo "  Failed:$FAILED_VERSIONS"
fi
echo "  Output: $OUTPUT_DIR/"
echo "========================================="
