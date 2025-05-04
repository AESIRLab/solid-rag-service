import java.io.FileInputStream
import java.net.URI
import java.util.Properties

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

val props = Properties()
props.load(FileInputStream("credentials.properties"))

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
                password = props.getProperty("mavenKey")
            }
        }
    }
}

rootProject.name = "SolidRagApp"
include(":app")
 