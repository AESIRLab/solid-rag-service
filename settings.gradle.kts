import java.net.URI

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
//        maven { url = "https://www.jitpack.io" }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = URI("https://www.jitpack.io") }
        gradlePluginPortal()
        maven {
            url = URI("https://repo1.maven.org/maven2/")
        }
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/aesirlab/annotations-repo")
            credentials {
                username = "zg009"
                password = "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"
            }
        }
    }
}

rootProject.name = "UsingCustomProcessorAndroid"
include(":app")
 