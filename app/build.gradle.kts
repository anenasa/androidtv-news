import org.gradle.util.GradleVersion

plugins {
    id("com.android.application")
    id("com.chaquo.python")
    if (GradleVersion.current() < GradleVersion.version("9.0")) {
        id("org.jetbrains.kotlin.android")
    }
    id("org.jetbrains.kotlin.plugin.serialization")
}

val useApi21 = GradleVersion.current() < GradleVersion.version("9.0")
val apiVersion = if(useApi21) 21 else 24
// Latest version requires AGP 9.1.0 or later
val coreKtxVersion = if(useApi21) "1.17.0" else "1.18.0"
val media3Version = if(useApi21) "1.8.1" else "1.10.1"
val lifecycleVersion = if(useApi21) "2.9.4" else "2.11.0"
val api21Suffix = if(useApi21) "api21-" else ""

android {
    namespace = "io.github.anenasa.news"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.anenasa.news"
        // No need to update minSdkVersion to 23
        // to use usesCleartextTraffic.
        // They are ignored in older versions.
        // https://stackoverflow.com/a/27100238
        minSdk = apiVersion
        // Need to set this to 29 so file access works on Android TV 11 and above
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 29
        versionCode = 60000
        versionName = "6.0.0"

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

chaquopy {
    defaultConfig {
        version = "3.11"
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation("androidx.core:core-ktx:$coreKtxVersion")
    implementation("androidx.media3:media3-exoplayer:${media3Version}")
    implementation("androidx.media3:media3-exoplayer-hls:${media3Version}")
    implementation("androidx.media3:media3-datasource-rtmp:${media3Version}") {
        // https://github.com/ant-media/LibRtmp-Client-for-Android/issues/109
        exclude("io.antmedia", "rtmp-client")
    }
    implementation("com.github.anenasa:LibRtmp-Client-for-Android:16647b19d5")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.squareup.okhttp3:okhttp:5.4.0")
    implementation("org.jsoup:jsoup:1.22.2")
    implementation("com.github.mendhak:storage-chooser:2.0.4.4b")
    implementation("ch.acra:acra-core:5.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:${lifecycleVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
}
