import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.jehadalomour.flowvan.buildlogic"

// AGP 8.x / the KMP plugin are compiled for JDK 17 bytecode.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        // The Kotlin/Room Gradle plugin APIs we reference are built with a newer Kotlin
        // than Gradle's embedded kotlin-dsl compiler; allow reading their metadata.
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}

dependencies {
    // compileOnly: only needed to compile the convention plugins against the plugin
    // DSL types we reference (LibraryExtension, KotlinMultiplatformExtension, RoomExtension).
    // Compose + KSP are applied by id only (no class references), so they are NOT here —
    // they resolve at runtime from the root build's `apply false` plugin classpath.
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "flowvan.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("compose") {
            id = "flowvan.compose"
            implementationClass = "ComposeConventionPlugin"
        }
        register("room") {
            id = "flowvan.room"
            implementationClass = "RoomConventionPlugin"
        }
    }
}
