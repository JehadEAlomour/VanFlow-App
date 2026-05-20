package com.jehadalomour.flowvan.screens.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKCoordinateRegionMakeWithDistance
import platform.MapKit.MKDirections
import platform.MapKit.MKDirectionsRequest
import platform.MapKit.MKDirectionsTransportTypeAutomobile
import platform.MapKit.MKMapItem
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol
import platform.MapKit.MKOverlayLevel
import platform.MapKit.MKOverlayRenderer
import platform.MapKit.MKPlacemark
import platform.MapKit.MKPointAnnotation
import platform.MapKit.MKPolyline
import platform.MapKit.MKPolylineRenderer
import platform.MapKit.MKRoute
import platform.MapKit.MKRouteStep
import platform.MapKit.MKUserTrackingModeFollowWithHeading
import platform.MapKit.MKUserTrackingModeNone
import platform.UIKit.UIColor
import platform.darwin.NSObject
import kotlin.math.roundToInt

// Holds map + delegate together; MKMapView.delegate is a weak reference
// so we keep both alive here and let `remember` retain the holder.
private class MapViewHolder {
    val delegate = RouteDelegate()
    val mapView = MKMapView().also {
        it.showsUserLocation = true
        it.userTrackingMode = MKUserTrackingModeNone
        it.delegate = delegate
    }
    var routeRequested = false
}

private class RouteDelegate : NSObject(), MKMapViewDelegateProtocol {
    var onRouteInfo: (String, String) -> Unit = { _, _ -> }
    var onStepsLoaded: (List<NavStep>) -> Unit = {}

    override fun mapView(
        mapView: MKMapView,
        rendererForOverlay: platform.MapKit.MKOverlayProtocol,
    ): MKOverlayRenderer {
        if (rendererForOverlay is MKPolyline) {
            return MKPolylineRenderer(polyline = rendererForOverlay).also { r ->
                r.strokeColor = UIColor(red = 0.294, green = 0.561, blue = 0.965, alpha = 1.0)
                r.lineWidth = 5.0
            }
        }
        return MKOverlayRenderer(overlay = rendererForOverlay)
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformMapContent(
    userLat: Double?,
    userLng: Double?,
    customerLat: Double,
    customerLng: Double,
    customerName: String,
    modifier: Modifier,
    isNavigating: Boolean,
    onRouteInfo: (duration: String, distance: String) -> Unit,
    onStepsLoaded: (List<NavStep>) -> Unit,
    onLocationUpdate: (lat: Double, lng: Double) -> Unit,
) {
    val holder = remember { MapViewHolder() }
    holder.delegate.onRouteInfo = onRouteInfo
    holder.delegate.onStepsLoaded = onStepsLoaded

    UIKitView(
        modifier = modifier,
        factory = {
            val mapView = holder.mapView

            val annotation = MKPointAnnotation()
            annotation.setCoordinate(CLLocationCoordinate2DMake(customerLat, customerLng))
            annotation.setTitle(customerName)
            annotation.setSubtitle("الوجهة")
            mapView.addAnnotation(annotation)

            val centerLat = if (userLat != null) (customerLat + userLat) / 2.0 else customerLat
            val centerLng = if (userLng != null) (customerLng + userLng) / 2.0 else customerLng
            mapView.setRegion(
                MKCoordinateRegionMakeWithDistance(
                    CLLocationCoordinate2DMake(centerLat, centerLng),
                    if (userLat != null) 8000.0 else 2000.0,
                    if (userLat != null) 8000.0 else 2000.0,
                ),
                animated = false,
            )

            if (userLat != null && userLng != null && !holder.routeRequested) {
                holder.routeRequested = true
                val request = MKDirectionsRequest()
                request.source = MKMapItem(
                    placemark = MKPlacemark(coordinate = CLLocationCoordinate2DMake(userLat, userLng)),
                )
                request.destination = MKMapItem(
                    placemark = MKPlacemark(coordinate = CLLocationCoordinate2DMake(customerLat, customerLng)),
                )
                request.transportType = MKDirectionsTransportTypeAutomobile
                MKDirections(request = request).calculateDirectionsWithCompletionHandler { response, _ ->
                    val route = response?.routes?.firstOrNull() as? MKRoute
                        ?: return@calculateDirectionsWithCompletionHandler
                    mapView.addOverlay(route.polyline, MKOverlayLevel.MKOverlayLevelAboveRoads)
                    val minutes = (route.expectedTravelTime / 60).roundToInt()
                    val km = "%.1f km".format(route.distance / 1000.0)
                    val dur = if (minutes < 60) "$minutes دقيقة" else "${minutes / 60} ساعة ${minutes % 60} د"
                    holder.delegate.onRouteInfo(dur, km)

                    val steps = route.steps.mapNotNull { rawStep ->
                        val step = rawStep as? MKRouteStep ?: return@mapNotNull null
                        val distM = step.distance
                        val distText = if (distM < 1000) "${distM.roundToInt()} م" else "${"%.1f".format(distM / 1000)} كم"
                        NavStep(
                            instruction = step.instructions,
                            distanceText = distText,
                            endLat = customerLat,
                            endLng = customerLng,
                        )
                    }
                    holder.delegate.onStepsLoaded(steps)
                }
            }

            mapView
        },
        update = { mapView ->
            mapView.showsUserLocation = true
            if (isNavigating) {
                mapView.setUserTrackingMode(MKUserTrackingModeFollowWithHeading, animated = true)
            } else {
                mapView.setUserTrackingMode(MKUserTrackingModeNone, animated = true)
            }
        },
    )
}
