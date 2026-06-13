import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Applies the Compose Multiplatform + Compose Compiler plugins. Apply alongside
 * `flowvan.kmp.library` on UI-bearing modules (design-system + features).
 * Compose dependencies stay in each module since they vary.
 */
class ComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("org.jetbrains.compose")
            apply("org.jetbrains.kotlin.plugin.compose")
        }
    }
}
