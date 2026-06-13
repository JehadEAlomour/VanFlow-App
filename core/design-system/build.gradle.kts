plugins {
    id("flowvan.kmp.library")
    // Compose applied directly (not via flowvan.compose) so the `compose.resources {}`
    // DSL accessor is generated for this script.
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    "commonMainImplementation"(libs.compose.runtime)
    "commonMainImplementation"(libs.compose.foundation)
    "commonMainImplementation"(libs.compose.material3)
    "commonMainImplementation"(libs.compose.ui)
    "commonMainImplementation"(libs.kotlinx.datetime)
    // Shared resources (one generated `Res` for all features) + shared composables
    // that take domain models → api so consumers see the types.
    "commonMainApi"(libs.compose.components.resources)
    "commonMainApi"(projects.core.model)
    "commonMainApi"(projects.core.common)
    "androidMainImplementation"(libs.compose.uiToolingPreview)
}

// Generate a public `Res` class at a stable package shared by all feature modules.
compose.resources {
    publicResClass = true
    packageOfResClass = "com.jehadalomour.flowvan.core.designsystem.resources"
}
