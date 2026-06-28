plugins {
    id("flowvan.kmp.library")
    id("flowvan.room")
}

dependencies {
    // Entities/mappers expose domain models in the public API → api
    "commonMainApi"(projects.core.model)
    "commonMainImplementation"(projects.core.common)
    // FlowVanDatabase (a RoomDatabase) is exposed in the public API → api so consumers
    // (e.g. core:domain) can see the RoomDatabase supertype on their compile classpath
    "commonMainApi"(libs.androidx.room.runtime)
    "commonMainImplementation"(libs.androidx.sqlite.bundled)
    "commonMainImplementation"(libs.kotlinx.coroutines.core)
}
