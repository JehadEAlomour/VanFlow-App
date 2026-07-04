plugins {
    id("flowvan.kmp.library")
    // Required: OfferDefinitionParser has @Serializable mirror DTOs. Without the plugin their
    // serializers aren't generated and decoding throws at runtime (offers silently dropped).
    alias(libs.plugins.kotlinSerialization)
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
    "commonMainImplementation"(libs.kotlinx.datetime)
    "commonMainImplementation"(libs.kermit)
    "commonTestImplementation"(libs.kotlin.test)
    "androidMainImplementation"(libs.androidx.core.ktx)
    "androidMainImplementation"(libs.kotlinx.coroutines.android)
    "androidMainImplementation"(libs.kotlinx.coroutines.play.services)
    "androidMainImplementation"(libs.play.services.location)
}
