# 使用 Ubuntu 22.04 作為基礎映像
FROM ubuntu:22.04

# 設定環境變數避免互動式安裝詢問
ENV DEBIAN_FRONTEND=noninteractive

# 安裝基本工具、JDK 17、Python 3 及 NDK 建構所需的依賴
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    wget \
    unzip \
    git \
    python3 \
    python3-pip \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# 設定 JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=$PATH:$JAVA_HOME/bin

# 安裝 Android SDK
ENV ANDROID_SDK_ROOT=/opt/android-sdk
RUN mkdir -p $ANDROID_SDK_ROOT/cmdline-tools && \
    wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdline-tools.zip && \
    unzip /tmp/cmdline-tools.zip -d $ANDROID_SDK_ROOT/cmdline-tools && \
    mv $ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools $ANDROID_SDK_ROOT/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

# 設定 SDK 相關環境變數
ENV PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools

# 接受授權並安裝所需的 SDK 和 NDK 組件 (根據專案需求)
# compileSdk = 36, NDK is required for Chaquopy
RUN yes | sdkmanager --licenses && \
    sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools" "ndk;25.2.9519653"

# 設定工作目錄
WORKDIR /app

# 預設執行命令
CMD ["./gradlew", "assembleRelease"]
