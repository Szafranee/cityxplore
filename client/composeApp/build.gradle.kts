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
    alias(libs.plugins.kotlinSerialization)
    id("com.github.gmazzo.buildconfig") version "6.0.7"
}

// Shared property resolution logic
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(FileInputStream(localPropertiesFile))
    }
}

fun getRequiredProperty(key: String): String {
    // Try local.properties first, then environment variables (for CI/CD), then Gradle properties
    return localProperties.getProperty(key)
        ?: System.getenv(key)
        ?: providers.gradleProperty(key).orNull
        ?: throw GradleException("Missing required property '$key' in local.properties or environment variables")
}

// Resolve properties once at configuration time
val supabaseUrl: String by lazy { getRequiredProperty("SUPABASE_URL") }
val supabaseKey: String by lazy { getRequiredProperty("SUPABASE_KEY") }
val mapboxPublicToken: String by lazy { getRequiredProperty("MAPBOX_PUBLIC_TOKEN") }

buildConfig {
    packageName("app.cityxplore")

    buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
    buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
    buildConfigField("String", "MAPBOX_PUBLIC_TOKEN", "\"$mapboxPublicToken\"")

    // Configure DEBUG conditionally - defaults to false for release safety
    // Can be explicitly enabled via -Pdebug.build=true
    // Automatically detects Android build type when available
    val explicitDebugProperty = providers.gradleProperty("debug.build").orNull?.toBoolean()
    val isDebugBuild = explicitDebugProperty ?: gradle.startParameter.taskNames.any {
        it.contains("debug", ignoreCase = true) && !it.contains("release", ignoreCase = true)
    }
    buildConfigField("Boolean", "DEBUG", isDebugBuild.toString())
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
            implementation(libs.h3)
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
    namespace = "app.cityxplore"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "app.cityxplore"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        // Use the shared mapboxPublicToken resolved at top level
        manifestPlaceholders["MAPBOX_ACCESS_TOKEN"] = mapboxPublicToken
        resValue("string", "mapbox_access_token", mapboxPublicToken)
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("debug") {
            // Debug build type
        }
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = false  // Disabled - using com.github.gmazzo.buildconfig plugin instead
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}
