plugins {
    id("flowvan.kmp.library")
    id("flowvan.compose")
}

dependencies {
    "commonMainImplementation"(libs.compose.runtime)
    "commonMainImplementation"(libs.compose.foundation)
    "commonMainImplementation"(libs.compose.material3)
    "commonMainImplementation"(libs.compose.ui)
    "commonMainImplementation"(libs.compose.components.resources)
    "commonMainImplementation"(libs.compose.uiToolingPreview)
    "commonMainImplementation"(libs.androidx.lifecycle.viewmodel)
    "commonMainImplementation"(libs.androidx.lifecycle.viewmodelCompose)
    "commonMainImplementation"(libs.androidx.lifecycle.runtimeCompose)
    "commonMainImplementation"(libs.koin.core)
    "commonMainImplementation"(libs.koin.compose.viewmodel)
    "commonMainImplementation"(libs.kermit)

    "commonMainImplementation"(projects.core.model)
    "commonMainImplementation"(projects.core.common)
    "commonMainImplementation"(projects.core.data)
    "commonMainImplementation"(projects.core.network)
    "commonMainImplementation"(projects.core.domain)
    "commonMainImplementation"(projects.core.designSystem)
}
