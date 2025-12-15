rootProject.name = "cityxplore"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Central plugin and repository management.
// NOTE: Mapbox repo token is fetched from ENV (MAPBOX_DOWNLOADS_TOKEN) or Gradle properties.
// Never commit real tokens to VCS. If missing, an empty string is used and dependencies may fail.
pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
                includeGroup("org.chromium.net")
            }
        }
        mavenCentral()
        // Mapbox repository (requires authentication for certain artifacts).
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                username = "mapbox"
                // Fetch token: first from ENV, then from Gradle properties (set in ~/.gradle/gradle.properties or local.properties), never store real token in repo.
                password = System.getenv("MAPBOX_DOWNLOADS_TOKEN")
                    ?: (providers.gradleProperty("MAPBOX_DOWNLOADS_TOKEN").orNull)
                            ?: run {
                        val localProps = java.util.Properties()
                        val localFile = rootDir.resolve("local.properties")
                        if (localFile.exists()) {
                            localProps.load(java.io.FileInputStream(localFile))
                            localProps.getProperty("MAPBOX_DOWNLOADS_TOKEN")
                        } else {
                            null
                        }
                    } ?: ""
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
include(":backend")
include(":client:composeApp")
