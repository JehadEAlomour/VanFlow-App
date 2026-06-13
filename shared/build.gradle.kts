plugins {
    id("flowvan.kmp.library")
    id("flowvan.compose")
    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    // Core layers exposed via :shared's public API → api
    "commonMainApi"(projects.core.model)
    "commonMainApi"(projects.core.common)
    "commonMainApi"(projects.core.database)
    "commonMainApi"(projects.core.datastore)
    "commonMainApi"(projects.core.network)
    "commonMainApi"(projects.core.data)
    "commonMainApi"(projects.core.domain)

    // Compose runtime is required because the Compose compiler is applied;
    // no Compose UI deps to keep the iOS framework slim.
    "commonMainImplementation"(libs.compose.runtime)
    "commonMainImplementation"(libs.androidx.lifecycle.viewmodel)
    "commonMainImplementation"(libs.androidx.lifecycle.viewmodelCompose)
    "commonMainImplementation"(libs.koin.core)
    "commonMainImplementation"(libs.koin.compose.viewmodel)
    "commonMainImplementation"(libs.multiplatform.settings)
    "commonMainImplementation"(libs.multiplatform.settings.noarg)
    "commonMainImplementation"(libs.multiplatform.settings.coroutines)
    "commonMainImplementation"(libs.kotlinx.coroutines.core)
    "commonMainImplementation"(libs.kotlinx.datetime)
    "commonMainImplementation"(libs.kotlinx.serialization.json)
    "commonMainImplementation"(libs.kermit)
    "commonMainImplementation"(projects.core.designSystem)

    "commonTestImplementation"(libs.kotlin.test)

    "androidMainImplementation"(libs.koin.android)
    // coroutines-android provides the Android Main dispatcher for :shared ViewModels.
    "androidMainImplementation"(libs.kotlinx.coroutines.android)
}
