rootProject.name = "FlowVan"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
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
            }
        }
        mavenCentral()
    }
}

include(":composeApp")
include(":core:common")
include(":core:data")
include(":core:database")
include(":core:datastore")
include(":core:design-system")
include(":core:domain")
include(":core:model")
include(":core:network")
include(":feature:ai")
include(":feature:auth")
include(":feature:customer")
include(":feature:home")
include(":feature:map")
include(":feature:print")
include(":feature:reports")
include(":feature:voucher")
include(":shared")