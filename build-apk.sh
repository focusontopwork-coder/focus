#!/bin/bash
# Builds a test APK of the client app.   Run on the Mac:  bash build-apk.sh
set -e
cd "$(dirname "$0")"
# files copied in by Claude can arrive read-only: make sure Gradle can read/copy them
chmod -R u+rwX . 2>/dev/null || true
say() { echo; echo "── $1"; }

say "1/4  Java 21"
if ! /usr/libexec/java_home -v 21 >/dev/null 2>&1; then brew install --cask temurin@21; fi
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"

say "2/4  Android SDK"
if ! command -v sdkmanager >/dev/null; then brew install --cask android-commandlinetools; fi
export ANDROID_HOME="$(brew --prefix)/share/android-commandlinetools"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
yes | sdkmanager --licenses >/dev/null 2>&1 || true
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0" >/dev/null
echo "sdk.dir=$ANDROID_HOME" > local.properties

say "3/4  Build (first time: 5–15 minutes, downloads libraries)"
chmod +x gradlew
set -o pipefail
./gradlew --no-daemon :composeApp:assembleDebug 2>&1 | tee build.log

say "4/4  Done"
OUT="$HOME/Desktop/FocusOnTop-Client.apk"
cp composeApp/build/outputs/apk/debug/composeApp-debug.apk "$OUT"
echo "APK: $OUT"
echo
echo "── Google sign-in SHA-1 (already registered in Firebase):"
"$JAVA_HOME/bin/keytool" -list -v -keystore debug.keystore -alias androiddebugkey -storepass android 2>/dev/null | grep -E "SHA1:" || true
open -R "$OUT"
