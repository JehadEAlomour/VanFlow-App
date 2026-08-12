plugins {
    id("flowvan.kmp.library")
}

dependencies {
    // Use cases return/expose domain models → api
    "commonMainApi"(projects.core.model)
    // Pragmatic Clean Arch: domain depends on the concrete data layer (and, per the
    // existing code, also network/database directly — a smell to tighten later).
    "commonMainImplementation"(projects.core.common)
    "commonMainImplementation"(projects.core.data)
    "commonMainImplementation"(projects.core.database)
    "commonMainImplementation"(projects.core.datastore)
    "commonMainImplementation"(projects.core.network)
    "commonMainImplementation"(libs.kotlinx.coroutines.core)
    "commonMainImplementation"(libs.kotlinx.datetime)
    "commonMainImplementation"(libs.kotlinx.serialization.json)
    "commonMainImplementation"(libs.kermit)
    "commonMainImplementation"(libs.multiplatform.settings)
    "commonTestImplementation"(libs.kotlin.test)
    // Evaluator tests assert on the network EvaluationResultDto shape it returns.
    "commonTestImplementation"(projects.core.network)
    "commonTestImplementation"(projects.core.database)
    "commonTestImplementation"(libs.kotlinx.datetime)
}
