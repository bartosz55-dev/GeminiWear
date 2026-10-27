#!/bin/bash
set -e

PROJECT_DIR="/Volumes/DyskNvmeE6XPG/Antigravity/krakowanieweraose/GeminiWear"
cd "$PROJECT_DIR"

export JAVA_HOME="/opt/homebrew/opt/openjdk"
export PATH="/opt/homebrew/opt/openjdk/bin:$PATH"

JAVAC="/opt/homebrew/opt/openjdk/bin/javac"
BUILD_TOOLS="/Users/bartosz5500/Library/Android/sdk/build-tools/36.1.0"
ANDROID_JAR="/Users/bartosz5500/Library/Android/sdk/platforms/android-36.1/android.jar"
AAPT2="$BUILD_TOOLS/aapt2"
D8="$BUILD_TOOLS/d8"
ZIPALIGN="$BUILD_TOOLS/zipalign"
APKSIGNER="$BUILD_TOOLS/apksigner"

echo "==> Cleaning old build artifacts..."
rm -rf build
mkdir -p build/compiled_res build/gen build/classes build/dex build/apk keystore

echo "==> Compiling resources with aapt2..."
"$AAPT2" compile --dir src/main/res -o build/compiled_res.zip

echo "==> Linking resources and generating R.java..."
"$AAPT2" link -I "$ANDROID_JAR" \
    --manifest src/main/AndroidManifest.xml \
    --min-sdk-version 30 \
    --target-sdk-version 34 \
    --version-code 1 \
    --version-name "1.0" \
    --java build/gen \
    -o build/apk/unaligned.apk \
    build/compiled_res.zip \
    --auto-add-overlay

echo "==> Compiling Java sources with javac..."
"$JAVAC" -source 17 -target 17 \
    -cp "$ANDROID_JAR:build/gen" \
    -d build/classes \
    build/gen/com/antigravity/geminiwear/R.java \
    src/main/java/com/antigravity/geminiwear/*.java

echo "==> Converting bytecode to classes.dex with d8..."
"$D8" --release --min-api 30 --lib "$ANDROID_JAR" --output build/dex build/classes/com/antigravity/geminiwear/*.class

echo "==> Adding classes.dex to APK..."
cd build/dex
zip -u ../apk/unaligned.apk classes.dex
cd "$PROJECT_DIR"

echo "==> Aligning APK with zipalign..."
"$ZIPALIGN" -f -p 4 build/apk/unaligned.apk build/apk/aligned.apk

echo "==> Creating debug keystore if needed..."
if [ ! -f keystore/debug.keystore ]; then
    keytool -genkeypair -v \
        -keystore keystore/debug.keystore \
        -alias androiddebugkey \
        -keypass android \
        -storepass android \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -dname "CN=Android Debug,O=Android,C=US"
fi

echo "==> Signing APK with apksigner..."
"$APKSIGNER" sign --ks keystore/debug.keystore \
    --ks-key-alias androiddebugkey \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out build/apk/GeminiWear.apk \
    build/apk/aligned.apk

echo "==> Build successful! Output: $PROJECT_DIR/build/apk/GeminiWear.apk"
ls -lh build/apk/GeminiWear.apk
