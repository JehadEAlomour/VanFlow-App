import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Adds the per-customer flavours to the application module, and turns on
 * BuildConfig — which is what carries each customer's URLs into the app.
 *
 * Application only. See [applyCustomerFlavors] for why no library module gets
 * this dimension.
 */
class CustomerFlavorsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        extensions.configure(ApplicationExtension::class.java) {
            buildFeatures.buildConfig = true
            applyCustomerFlavors()
        }
    }
}
