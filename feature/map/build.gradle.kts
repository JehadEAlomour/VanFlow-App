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
    "commonMainImplementation"(libs.kotlinx.coroutines.core)

    "commonMainImplementation"(projects.core.model)
    "commonMainImplementation"(projects.core.data)
    "commonMainImplementation"(projects.core.designSystem)

    // The `gms` PlatformMapContent draws a Google map; the `nogms` one says there
    // isn't one, and needs none of this.
    "gmsImplementation"(libs.maps.compose)
    "gmsImplementation"(libs.play.services.maps)
    "gmsImplementation"(libs.play.services.location)
    "androidMainImplementation"(libs.androidx.core.ktx)
    "androidMainImplementation"(libs.kotlinx.coroutines.android)
}
