import org.gradle.util.GradleVersion

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    val useApi21 = GradleVersion.current() < GradleVersion.version("9.0")
    val chaquopyVersion = if (useApi21) "15.0.1" else "17.0.0"
    val agpVersion = if (useApi21) "8.13.2" else "9.3.0"
    plugins {
        id("com.chaquo.python") version chaquopyVersion
        id("com.android.application") version agpVersion apply false
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"

}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "新聞直播"
include(":app")
