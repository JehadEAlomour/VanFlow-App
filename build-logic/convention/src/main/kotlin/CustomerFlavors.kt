import com.android.build.api.dsl.ApplicationExtension
import org.gradle.kotlin.dsl.create

/*
 * The per-customer split: one APK per company, built from one source tree.
 *
 * What actually differs between them is three strings — the API base URL, the
 * update manifest URL, and a name for support to say out loud. Everything else
 * is identical. They used to live as a `const val` in ApiConfig with the other
 * customers' URLs commented out above it, which made "which customer is this APK
 * for?" a question about who edited the file last.
 *
 * WHY THIS DIMENSION IS ON THE APP MODULE ONLY, unlike `services`. The rule for
 * `services` — every Android module carries it — exists because a LIBRARY
 * declares that dimension, and a consumer that does not have to be told how to
 * resolve it. Here nothing is flavoured but three BuildConfig fields in the
 * application itself; no library has a `customer` dimension to resolve, so the
 * app declaring one it does not share with them needs no strategy and costs no
 * extra library variants. Adding it everywhere would multiply every module's
 * variants by three to express a difference none of them have.
 *
 * SEPARATE applicationId PER CUSTOMER, for the same reason `nogms` has one and a
 * sharper one besides: these builds are told apart only by which server they
 * talk to. Sharing a package would mean a Ferdous APK installs cleanly over a
 * Tal3at handset and silently repoints a van full of stock at another company's
 * data — a mistake with no symptom until someone sells against the wrong
 * catalogue. Different packages make it impossible rather than unlikely.
 */
private const val CUSTOMER_DIMENSION = "customer"

/**
 * One customer's build.
 *
 * [updateManifestUrl] is the static JSON next to that customer's APK on the
 * update host — NOT on [apiBaseUrl]. The two are deliberately unrelated: the API
 * lives on the customer's own box, and an update that could only be fetched from
 * a server that is down would be useless exactly when it is needed.
 */
private data class Customer(
    val name: String,
    val applicationIdSuffix: String,
    val apiBaseUrl: String,
    val updateManifestUrl: String,
)

private const val UPDATE_HOST = "https://7softwarejo.com/flowvan/updates"

private val CUSTOMERS = listOf(
    Customer(
        name = "ferdous",
        applicationIdSuffix = ".ferdous",
        apiBaseUrl = "http://94.142.51.91:3100/api/v1",
        updateManifestUrl = "$UPDATE_HOST/ferdous/android.json",
    ),
    Customer(
        name = "tal3at",
        applicationIdSuffix = ".tal3at",
        apiBaseUrl = "http://77.245.5.113:3002/api/v1",
        updateManifestUrl = "$UPDATE_HOST/tal3at/android.json",
    ),
    Customer(
        name = "dev",
        applicationIdSuffix = ".dev",
        apiBaseUrl = "https://app-dev.7softwarejo.com/api/v1",
        updateManifestUrl = "$UPDATE_HOST/dev/android.json",
    ),
)

internal fun ApplicationExtension.applyCustomerFlavors() {
    flavorDimensions += CUSTOMER_DIMENSION
    productFlavors {
        CUSTOMERS.forEach { customer ->
            create(customer.name) {
                dimension = CUSTOMER_DIMENSION
                applicationIdSuffix = customer.applicationIdSuffix
                // In the version string so a screenshot of the settings screen is
                // enough to answer "which build is this?" on a support call.
                versionNameSuffix = "-${customer.name}"
                buildConfigField("String", "CUSTOMER", "\"${customer.name}\"")
                buildConfigField("String", "API_BASE_URL", "\"${customer.apiBaseUrl}\"")
                buildConfigField("String", "UPDATE_MANIFEST_URL", "\"${customer.updateManifestUrl}\"")
            }
        }
    }
}
