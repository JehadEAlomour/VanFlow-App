plugins {
    id("flowvan.kmp.library")
    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    // AppSettings exposes AppLanguage in its public API → api so consumers see the type
    "commonMainApi"(projects.core.common)
    "commonMainImplementation"(libs.kotlinx.serialization.json)
    "commonTestImplementation"(libs.kotlin.test)
}
