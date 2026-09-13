package com.jehadalomour.flowvan

import android.app.Application
import com.google.android.gms.maps.MapsInitializer

/**
 * Pre-loads the Maps renderer at startup so the first map screen does not pay
 * for it. Asking for LATEST here rather than at first use is deliberate: the
 * renderer choice is process-wide and fixed by whoever asks first.
 */
internal fun Application.initPlatformServices() {
    MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST, null)
}
