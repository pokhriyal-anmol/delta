#!/usr/bin/env bash
set -euo pipefail
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

V="${1:?usage: ./release.sh v1.1}"

./gradlew assembleRelease
APK=app/build/outputs/apk/release/app-release.apk

OWNER=$(keytool -printcert -jarfile "$APK" | grep -m1 "Owner:" || true)
echo "$OWNER"
[[ "$OWNER" == *"CN=Anmol"* ]] || { echo "✗ WRONG SIGNER — aborting"; exit 1; }

git tag -a "$V" -m "$V" 2>/dev/null || echo "tag $V already exists"
git push origin "$V"

cp "$APK" "f1t-$V.apk"
gh release create "$V" "f1t-$V.apk" --title "$V" --generate-notes
rm -f "f1t-$V.apk"
echo "✓ published $V"
