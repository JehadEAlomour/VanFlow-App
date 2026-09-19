import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    // gms / nogms — see ServiceFlavorsConventionPlugin.
    id("flowvan.service.flavors")
    // ferdous / tal3at / dev — see CustomerFlavors.kt. Carries each customer's
    // API and update URLs in as BuildConfig fields.
    id("flowvan.customer.flavors")
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
            // The self-update receivers log through kermit, like the rest of the app.
            implementation(libs.kermit)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            // XPrinter / POS thermal printer SDK (USB / Bluetooth / Serial / Network) — ESC/POS
            implementation(files("libs/printer-sdk.aar"))
            // Zebra Link-OS SDK — CPCL mobile Bluetooth printers (the ESC/POS SDK can't drive them)
            implementation(files("libs/ZSDK_ANDROID_API.jar"))
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
            implementation(projects.core.data)
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
        // Read from gradle.properties rather than written here, so bumping the
        // shipped version is one line in one file and the release script can do
        // it. The in-app updater compares versionCode and nothing else: ship two
        // different builds under the same code and the second one is invisible
        // to every device already carrying the first.
        versionCode = providers.gradleProperty("flowvan.versionCode").get().toInt()
        versionName = providers.gradleProperty("flowvan.versionName").get()
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
    sourceSets {
        // Point the flavour manifests at the same directory as the flavour's Kotlin,
        // so everything that makes a build the `gms` or `nogms` one sits together.
        //
        // Needed because the two plugins disagree about where a flavour lives and
        // each is only half-right: the Kotlin plugin remaps these source sets to
        // src/androidGms, but not until after AGP has already taken its snapshot of
        // where manifests are — so AGP goes on looking in AGP's own src/gms, finds
        // nothing, and merges no overlay at all. That failure is silent: the build
        // succeeds and the APK is simply missing the permissions and the service.
        // Setting the path here happens while the build script is read, which is
        // early enough for AGP to see it.
        getByName("gms").manifest.srcFile("src/androidGms/AndroidManifest.xml")
        getByName("nogms").manifest.srcFile("src/androidNogms/AndroidManifest.xml")
    }
    signingConfigs {
        // The release key, read from an untracked keystore.properties at the repo
        // root (see keystore.properties.example). Absent, the block is simply not
        // created and `release` falls back to the debug key below.
        val keystoreProperties = rootProject.file("keystore.properties")
        if (keystoreProperties.exists()) {
            val props = Properties().apply { keystoreProperties.inputStream().use(::load) }
            create("release") {
                storeFile = rootProject.file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            // WHY THIS MATTERS MORE THAN IT LOOKS. Android only replaces an
            // installed app with a new APK when both are signed by the SAME key.
            // The debug key is generated per developer machine and is not in the
            // repo — build a release with it, hand out the APK, and the day that
            // machine is reimaged every handset in the field becomes permanently
            // un-updatable: the only way forward is uninstall + reinstall, which
            // takes the local Room database (and any un-synced vouchers) with it.
            //
            // So a release built without keystore.properties is still allowed —
            // a developer has to be able to build one — but it is loud about it,
            // because an APK signed this way must never reach a van.
            signingConfig = signingConfigs.findByName("release") ?: run {
                logger.warn(
                    "\n" +
                        "  ****************************************************************\n" +
                        "  FlowVan: no keystore.properties — signing `release` with the\n" +
                        "  DEBUG key. This APK must NOT be distributed: devices that\n" +
                        "  install it can never be updated in place. See docs/auto-update.md\n" +
                        "  ****************************************************************",
                )
                signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // Required for minSdk 24/25 (e.g. Sunmi T2, Android 7.1): kotlinx-datetime
        // calls java.time.* which only exists on API 26+. Desugaring backports it;
        // without it the first date operation after login crashes with NoClassDefFoundError.
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Google, in the only build that has any. Nothing here is referenced from
    // commonMain, so `nogms` compiles with the Play Services / Maps artifacts
    // simply absent rather than stubbed.
    "gmsImplementation"(libs.maps.compose)
    "gmsImplementation"(libs.play.services.maps)
    "gmsImplementation"(libs.play.services.location)
}

