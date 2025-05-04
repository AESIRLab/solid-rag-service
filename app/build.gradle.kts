

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("com.google.devtools.ksp")
}

android {
    namespace = "org.aesirlab.solidragapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.aesirlab.solidragapp"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["appAuthRedirectScheme"]= "com.redirectScheme.com"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}


val version = "0.0.15"
dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material3.android)
    implementation(files("libs\\androjena_0.5.jar"))
    implementation(files("libs\\androjena_0.5_sources.jar"))
    implementation(files("libs\\arqoid_0.5.jar"))
    implementation(files("libs\\icu4j-3.4.5.jar"))
    implementation(files("libs\\iri-0.8.jar"))
    implementation(files("libs\\lucenoid_3.0.2.jar"))
    implementation(files("libs\\slf4j-android-1.6.1-RC1.jar"))
    implementation(files("libs\\tdboid_0.4.jar"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    ksp("com.squareup:kotlinpoet:1.14.0")
    ksp("com.squareup:kotlinpoet-ksp:1.12.0")

    implementation("org.aesirlab:sksolidannotations:$version")
    ksp("org.aesirlab:skannotationscompiler:$version")
    implementation("org.aesirlab:authlib:$version")

    // do not turn these to lib toml
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // TODO: 9.4.0 is broken, i think they moved repos on majro semver 10 to 10.0.1
    // https://mvnrepository.com/artifact/com.nimbusds/nimbus-jose-jwt
    implementation("com.nimbusds:nimbus-jose-jwt:10.0.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("net.openid:appauth:0.11.1")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.navigation:navigation-runtime-ktx:2.7.7")

    implementation("androidx.compose.ui:ui:1.4.0") // Ensure you're using the latest version
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.5.1") // For ViewModel integration with Compose
    implementation("androidx.activity:activity-compose:1.6.0") // For Compose activity support

    // for google aiedge rag
    implementation("com.google.ai.edge.localagents:localagents-rag:0.1.0")
    implementation("com.google.mediapipe:tasks-genai:0.10.22")
    // for await on listenablefuture
    implementation("com.google.guava:guava:33.3.1-android")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.9.0")

    // unified push
    implementation("com.github.UnifiedPush:android-connector:2.4.0")
}