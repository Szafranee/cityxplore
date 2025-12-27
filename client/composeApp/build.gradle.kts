import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties


plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    kotlin("plugin.serialization") version libs.versions.kotlin
    id("com.github.gmazzo.buildconfig") version "6.0.7"
}

buildConfig {
    packageName("app.cityxplore")

    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localProperties.load(FileInputStream(localPropertiesFile))
    }

    fun getRequiredProperty(key: String): String {
        // Try local.properties first, then environment variables (for CI/CD)
        return localProperties.getProperty(key)
            ?: System.getenv(key)
            ?: providers.gradleProperty(key).orNull
            ?: throw GradleException("Missing required property '$key' in local.properties or environment variables")
    }

    buildConfigField("String", "SUPABASE_URL", "\"${getRequiredProperty("SUPABASE_URL")}\"")
    buildConfigField("String", "SUPABASE_KEY", "\"${getRequiredProperty("SUPABASE_KEY")}\"")
    buildConfigField("String", "MAPBOX_PUBLIC_TOKEN", "\"${getRequiredProperty("MAPBOX_PUBLIC_TOKEN")}\"")
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.material.icons.extended)
            implementation(libs.koin.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.play.services.location)
            implementation(libs.kotlinx.coroutines.play.services)
            implementation(libs.mapbox.maps.android)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.kotlinx.datetime)
            implementation(libs.navigation.compose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.supabase.gotrue)
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.compose.auth)
            implementation(libs.supabase.compose.auth.ui)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "compose.project.demo"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "compose.project.demo"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(FileInputStream(localPropertiesFile))
        }

        // Try local.properties first, then environment variables (for CI/CD)
        val mapboxToken = localProperties.getProperty("MAPBOX_PUBLIC_TOKEN")
            ?: System.getenv("MAPBOX_PUBLIC_TOKEN")
            ?: providers.gradleProperty("MAPBOX_PUBLIC_TOKEN").orNull
            ?: throw GradleException("Missing required property 'MAPBOX_PUBLIC_TOKEN' in local.properties or environment variables")

        manifestPlaceholders["MAPBOX_ACCESS_TOKEN"] = mapboxToken
        resValue("string", "mapbox_access_token", mapboxToken)
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("debug") {
            // Android automatically generates BuildConfig.DEBUG = true for debug builds
        }
        getByName("release") {
            isMinifyEnabled = false
            // Android automatically generates BuildConfig.DEBUG = false for release builds
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}
