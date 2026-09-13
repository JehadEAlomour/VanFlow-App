import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.kotlin.dsl.create

/*
 * The `gms` / `nogms` split.
 *
 * `nogms` is a build of the app with no Google Play Services and no location of
 * any kind: no GPS permissions, no fused location client, no Maps SDK, no
 * tracking service. It exists for fleets whose handsets have no Play Services at
 * all (AOSP POS terminals) and for customers who will not ship a rep-tracking
 * app — neither of which a runtime flag can serve, because the point is that the
 * Google libraries are not IN the APK.
 *
 * EVERY ANDROID MODULE CARRIES THE DIMENSION, including the many that compile to
 * byte-identical output either way. The alternative — flavouring only the three
 * modules whose code actually differs and giving everyone else
 * `missingDimensionStrategy` — reads leaner and is a trap: the strategy pins the
 * dimension for that module's own resolution, so a flavoured module that also
 * inherited one resolved :core:data's `gms` variant while compiling the `nogms`
 * build, and the no-op location classes it was supposed to be binding were
 * simply not on its classpath. Declaring the dimension everywhere means the
 * question is never asked and never answered wrongly.
 */
private const val SERVICES_DIMENSION = "services"
private const val GMS_FLAVOR = "gms"
private const val NO_GMS_FLAVOR = "nogms"

internal fun LibraryExtension.applyServiceFlavors() {
    flavorDimensions += SERVICES_DIMENSION
    productFlavors {
        create(GMS_FLAVOR) { dimension = SERVICES_DIMENSION }
        create(NO_GMS_FLAVOR) { dimension = SERVICES_DIMENSION }
    }
}

internal fun ApplicationExtension.applyServiceFlavors() {
    flavorDimensions += SERVICES_DIMENSION
    productFlavors {
        create(GMS_FLAVOR) { dimension = SERVICES_DIMENSION }
        create(NO_GMS_FLAVOR) {
            dimension = SERVICES_DIMENSION
            // A separate package so both builds can sit on one handset — the way
            // anybody actually compares them — and so a fleet moved to the
            // no-location build is not silently upgraded back into tracking.
            applicationIdSuffix = ".nogms"
            versionNameSuffix = "-nogms"
        }
    }
}
