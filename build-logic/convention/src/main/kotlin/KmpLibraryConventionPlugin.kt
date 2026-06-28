import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Base for every KMP library module: applies Kotlin Multiplatform + Android Library,
 * wires the Android + iOS targets, and configures the android{} block. The module's
 * namespace and the iOS framework baseName are derived from the Gradle project path,
 * so individual modules only declare their dependencies.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("org.jetbrains.kotlin.multiplatform")
            apply("com.android.library")
        }

        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
        val segments = path.removePrefix(":").split(":")
        val frameworkName = segments.joinToString("") { seg ->
            seg.replace("-", "").replaceFirstChar { it.uppercase() }
        }
        val derivedNamespace =
            "com.jehadalomour.flowvan." + segments.joinToString(".") { it.replace("-", "") }

        extensions.configure<KotlinMultiplatformExtension> {
            androidTarget {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }
            listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
                iosTarget.binaries.framework {
                    baseName = frameworkName
                    isStatic = true
                }
            }
        }

        extensions.configure<LibraryExtension> {
            namespace = derivedNamespace
            compileSdk = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()
            defaultConfig {
                minSdk = libs.findVersion("android-minSdk").get().requiredVersion.toInt()
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_11
                targetCompatibility = JavaVersion.VERSION_11
            }
        }
    }
}
