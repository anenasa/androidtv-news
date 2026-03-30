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
    val useApi21 = providers.gradleProperty("useApi21").getOrElse("false") == "true"
    val chaquopyVersion = if (useApi21) "15.0.1" else "17.0.0"
    plugins {
        id("com.chaquo.python") version chaquopyVersion
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
