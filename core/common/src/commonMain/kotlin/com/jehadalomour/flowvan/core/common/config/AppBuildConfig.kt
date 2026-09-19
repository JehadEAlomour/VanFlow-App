package com.jehadalomour.flowvan.core.common.config

/**
 * Everything that differs between one customer's build and another's.
 *
 * WHY A VALUE PASSED IN, rather than a constant in shared code. These three
 * strings are the entire difference between the Ferdous build and the Tal3at
 * one, and each is a URL that points a van full of stock at a particular
 * company's server. A `const val` — which is what this replaced — meant
 * switching customer was an edit to a file in `core/network` that compiled
 * cleanly whichever way round it was, and the only way to tell which customer an
 * APK belonged to was to remember which line had been uncommented when it was
 * built.
 *
 * Now the build variant decides, Gradle writes it, and the wrong pairing cannot
 * be produced by accident.
 *
 * On Android these come from the `customer` product flavour, via the generated
 * BuildConfig — see composeApp/build.gradle.kts and CustomerFlavors.kt. iOS has
 * no flavours and supplies them directly.
 */
data class AppBuildConfig(
    /** "ferdous" | "tal3at" | "dev". Shown in support screens; never used to branch behaviour. */
    val customer: String,

    /** Where the app's own API lives, when the salesman has not overridden it in Settings. */
    val apiBaseUrl: String,

    /**
     * Absolute URL of this customer's update manifest — a static JSON file sitting
     * next to the APK it describes.
     *
     * DELIBERATELY NOT DERIVED FROM [apiBaseUrl]. The APK is served from
     * 7softwarejo.com; the API is on each customer's own box. Deriving one from
     * the other would mean a customer whose server is down cannot be repaired by
     * an update, which is the one moment an update matters most.
     */
    val updateManifestUrl: String,
)
