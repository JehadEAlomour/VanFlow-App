package com.jehadalomour.flowvan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { /* result ignored — login degrades gracefully */ }

    // Arabic is the app's main language: force the Arabic locale (and RTL) regardless of the
    // device language, so string resources always resolve to the default (Arabic) set.
    override fun attachBaseContext(newBase: Context) {
        val locale = Locale("ar")
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val permissions = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            // Android 12+ needs runtime Bluetooth permission to pair-list and connect a printer.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
            }
            // Android 13+ needs runtime consent to show the tracking foreground notification.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
            // Whatever this build does not DECLARE it must not ASK for: the `nogms`
            // flavour ships no location permissions, and asking for one that is
            // absent from the manifest is an instant silent denial — which on some
            // OEM builds still flashes a dialog at the rep before denying itself.
            // Reading the manifest rather than branching on the flavour keeps this
            // right for free the next time a permission moves between the two.
            .filter { it in declaredPermissions() }
        if (permissions.isNotEmpty()) {
            locationPermissionLauncher.launch(permissions.toTypedArray())
        }

        setContent {
            App()
        }
    }

    @Suppress("DEPRECATION")
    private fun declaredPermissions(): Set<String> = runCatching {
        packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            ?.toSet()
    }.getOrNull().orEmpty()
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
