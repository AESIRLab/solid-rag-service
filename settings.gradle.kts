pluginManagement {
    plugins {
        kotlin("jvm") version "2.0.0"
        id("com.google.devtools.ksp") version "2.0.0-1.0.21"
    }
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
}

rootProject.name = "UsingCustomProcessorAndroid"
include(":app")
 