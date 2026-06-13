plugins {
    id("flowvan.kmp.library")
    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    // Domain models appear in mapper signatures (public API) → api
    "commonMainApi"(projects.core.model)
    "commonMainImplementation"(projects.core.common)
    "commonMainImplementation"(projects.core.database)
    "commonMainImplementation"(projects.core.datastore)
    "commonMainImplementation"(libs.kotlinx.coroutines.core)
    "commonMainImplementation"(libs.kotlinx.serialization.json)
    "commonMainImplementation"(libs.kermit)
    // createHttpClient() returns an HttpClient in the public API → api so consumers
    // (e.g. :shared DI) can see the type
    "commonMainApi"(libs.ktor.client.core)
    "commonMainImplementation"(libs.ktor.client.content.negotiation)
    "commonMainImplementation"(libs.ktor.serialization.json)
    "commonMainImplementation"(libs.ktor.client.logging)
    "commonTestImplementation"(libs.kotlin.test)
    "androidMainImplementation"(libs.ktor.client.okhttp)
    // `iosMain` is a lazily-created intermediate source set, so its configuration
    // isn't available here; add Darwin to both concrete iOS targets instead (their
    // shared iosMain code still sees it via the intersection).
    "iosArm64MainImplementation"(libs.ktor.client.darwin)
    "iosSimulatorArm64MainImplementation"(libs.ktor.client.darwin)
}
