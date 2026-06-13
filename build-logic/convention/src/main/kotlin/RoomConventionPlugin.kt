import androidx.room.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/**
 * Applies KSP + Room and wires the schema directory and the per-target KSP
 * compiler dependencies. Apply alongside `flowvan.kmp.library` on :core:database.
 */
class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.google.devtools.ksp")
            apply("androidx.room")
        }

        extensions.configure<RoomExtension> {
            schemaDirectory("$projectDir/schemas")
        }

        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
        val roomCompiler = libs.findLibrary("androidx-room-compiler").get()
        dependencies {
            add("kspAndroid", roomCompiler)
            add("kspIosArm64", roomCompiler)
            add("kspIosSimulatorArm64", roomCompiler)
        }
    }
}
