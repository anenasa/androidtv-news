import com.android.build.gradle.internal.api.BaseVariantOutputImpl

plugins {
    id("com.android.application")
    id("com.chaquo.python")
    id("org.jetbrains.kotlin.android")
}

val useApi21 = providers.gradleProperty("useApi21").getOrElse("false") == "true"
val apiVersion = if(useApi21) 21 else 24
val coreKtxVersion = if(useApi21) "1.17.0" else "1.18.0"
val media3Version = if(useApi21) "1.8.0" else "1.10.0"
val lifecycleVersion = if(useApi21) "2.9.4" else "2.10.0"
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
        versionCode = 50000
        versionName = "5.0.0"

        buildConfigField("boolean", "USE_API_21", "$useApi21")

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
        }

        applicationVariants.all {
            outputs.all {
                val output = this as BaseVariantOutputImpl
                output.outputFileName = "androidtv-news-$api21Suffix$versionName.apk"
            }
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

    sourceSets {
        getByName("main") {
            jniLibs.setSrcDirs(listOf(if(useApi21) "src/main/jniLibs-api21" else "src/main/jniLibs-api24"))
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
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
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    // https://github.com/jhy/jsoup/issues/2459
    implementation("org.jsoup:jsoup:1.21.2")
    implementation("com.github.mendhak:storage-chooser:2.0.4.4b")
    implementation("ch.acra:acra-core:5.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:${lifecycleVersion}")
}
