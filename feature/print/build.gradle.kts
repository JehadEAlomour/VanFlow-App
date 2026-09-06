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
    "commonMainImplementation"(libs.kotlinx.datetime)
    "commonMainImplementation"(libs.kotlinx.serialization.json)

    "commonMainImplementation"(projects.core.model)
    "commonMainImplementation"(projects.core.common)
    "commonMainImplementation"(projects.core.data)
    "commonMainImplementation"(projects.core.database)
    // The print-line-discount permission is read from the persisted session.
    "commonMainImplementation"(projects.core.datastore)
    "commonMainImplementation"(projects.core.network)
    "commonMainImplementation"(projects.core.domain)
    // Compose-Multiplatform QR renderer — draws the JoFotara QR into the receipt.
    "commonMainImplementation"("io.github.alexzhirkevich:qrose:1.0.1")
    "commonMainImplementation"(projects.core.designSystem)

    // PdfShareHelper.android uses ContextCompat (androidx.core)
    "androidMainImplementation"(libs.androidx.core.ktx)
}
