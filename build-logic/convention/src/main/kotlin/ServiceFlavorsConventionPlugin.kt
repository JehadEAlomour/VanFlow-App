import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Adds the gms/nogms flavours to the application module. Library modules get
 * them from [KmpLibraryConventionPlugin] instead — see [applyServiceFlavors].
 */
class ServiceFlavorsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        extensions.configure(ApplicationExtension::class.java) { applyServiceFlavors() }
    }
}
