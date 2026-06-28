import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.maps.compose)
            implementation(libs.play.services.maps)
            implementation(libs.play.services.location)
            // XPrinter / POS thermal printer SDK (USB / Bluetooth / Serial / Network)
            implementation(files("libs/printer-sdk.aar"))
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.navigation.compose)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)
            implementation(libs.kotlinx.datetime)
            implementation(projects.core.designSystem)
            implementation(projects.core.model)
            implementation(projects.core.common)
            implementation(projects.core.database)
            implementation(projects.core.datastore)
            implementation(projects.core.domain)
            implementation(projects.feature.ai)
            implementation(projects.feature.auth)
            implementation(projects.feature.customer)
            implementation(projects.feature.home)
            implementation(projects.feature.map)
            implementation(projects.feature.print)
            implementation(projects.feature.reports)
            implementation(projects.feature.voucher)
            implementation(projects.shared)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.jehadalomour.flowvan"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.jehadalomour.flowvan"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            // The XPrinter serial driver (libserial_port.so) is a prebuilt vendor binary whose
            // LOAD segments aren't 16 KB-aligned, which blocks Play Store uploads for Android 15+.
            // Only the SERIAL transport needs it; we print over Bluetooth/USB/Network, so drop it.
            excludes += "**/libserial_port.so"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

