#!/bin/bash
set -e

# 1. 建立 Docker 映像
echo "Building Docker image 'android-news-builder'..."
docker build -t android-news-builder .

# 2. 執行編譯程序
echo "Starting build process in Docker..."
docker run --rm -v "$(pwd)":/app -v gradle-cache:/root/.gradle android-news-builder

# 3. 簽名並複製到 apks 資料夾
mkdir -p apks
if [ -f "release.jks" ]; then
    echo "Signing release APK..."
    docker run --rm -v "$(pwd)":/app android-news-builder bash -c '
        /opt/android-sdk/build-tools/*/apksigner sign --ks /app/release.jks --ks-pass pass:123456 --ks-key-alias androidtv --key-pass pass:123456 /app/app/build/outputs/apk/release/*.apk 2>/dev/null || true
    '
fi

docker run --rm -v "$(pwd)":/app ubuntu bash -c '
    cp -f /app/app/build/outputs/apk/release/*.apk /app/apks/ 2>/dev/null || true
    chown -R 1000:1000 /app/apks
    chmod -R 777 /app/apks
'

echo "Build finished! APKs are saved to 'apks/' directory."
