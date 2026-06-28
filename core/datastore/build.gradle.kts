plugins {
    id("flowvan.kmp.library")
}

dependencies {
    // Settings type appears in public constructors (e.g. SessionStore) → api
    "commonMainApi"(libs.multiplatform.settings)
    "commonMainApi"(libs.kotlinx.coroutines.core)
}
