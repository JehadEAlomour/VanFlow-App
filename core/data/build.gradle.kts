plugins {
    id("flowvan.kmp.library")
}

dependencies {
    // Repositories return domain models and Room entities in their public API → api
    "commonMainApi"(projects.core.model)
    "commonMainApi"(projects.core.database)
    "commonMainImplementation"(projects.core.common)
    "commonMainImplementation"(projects.core.datastore)
    "commonMainImplementation"(projects.core.network)
    "commonMainImplementation"(libs.kotlinx.coroutines.core)
    "commonMainImplementation"(libs.kotlinx.serialization.json)
    "commonMainImplementation"(libs.kermit)
    "androidMainImplementation"(libs.androidx.core.ktx)
    "androidMainImplementation"(libs.kotlinx.coroutines.android)
    "androidMainImplementation"(libs.kotlinx.coroutines.play.services)
    "androidMainImplementation"(libs.play.services.location)
}
