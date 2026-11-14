#!/usr/bin/env bash
set -euo pipefail
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

V="${1:?usage: ./release.sh v1.1}"

./gradlew assembleDirectRelease
APK=app/build/outputs/apk/direct/release/app-direct-release.apk

# APKs use v2/v3 signing — keytool CANNOT read these. apksigner only.
SDK=$(grep '^sdk.dir=' local.properties | cut -d= -f2-)
APKSIGNER=$(ls "$SDK"/build-tools/*/apksigner | sort -V | tail -1)
DN=$("$APKSIGNER" verify --print-certs "$APK" | grep -m1 "certificate DN" || true)
echo "$DN"
[[ "$DN" == *"CN=Anmol"* ]] || { echo "✗ WRONG SIGNER — aborting"; exit 1; }

git tag -a "$V" -m "$V" 2>/dev/null || echo "tag $V already exists"
git push origin "$V"

cp "$APK" "f1t-$V.apk"
gh release create "$V" "f1t-$V.apk" --title "$V" --generate-notes
rm -f "f1t-$V.apk"
echo "✓ published $V"
