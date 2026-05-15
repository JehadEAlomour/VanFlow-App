package com.jehadalomour.flowvan.screens.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKAnnotationView
import platform.MapKit.MKCoordinateRegionMakeWithDistance
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol
import platform.MapKit.MKPinAnnotationView
import platform.MapKit.MKPointAnnotation
import platform.MapKit.MKPolyline
import platform.MapKit.MKPolylineRenderer
import platform.MapKit.MKUserTrackingModeNone
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformMapContent(
    userLat: Double?,
    userLng: Double?,
    customerLat: Double,
    customerLng: Double,
    customerName: String,
    modifier: Modifier,
) {
    UIKitView(
        modifier = modifier,
        factory = {
            val mapView = MKMapView()
            mapView.showsUserLocation = true
            mapView.userTrackingMode = MKUserTrackingModeNone

            val customerAnnotation = MKPointAnnotation()
            customerAnnotation.setCoordinate(CLLocationCoordinate2DMake(customerLat, customerLng))
            customerAnnotation.setTitle(customerName)
            customerAnnotation.setSubtitle("الوجهة")
            mapView.addAnnotation(customerAnnotation)

            val centerLat = if (userLat != null) (customerLat + userLat) / 2.0 else customerLat
            val centerLng = if (userLng != null) (customerLng + userLng) / 2.0 else customerLng
            val region = MKCoordinateRegionMakeWithDistance(
                CLLocationCoordinate2DMake(centerLat, centerLng),
                if (userLat != null) 8000.0 else 2000.0,
                if (userLat != null) 8000.0 else 2000.0,
            )
            mapView.setRegion(region, animated = false)

            if (userLat != null && userLng != null) {
                val coords = arrayOf(
                    CLLocationCoordinate2DMake(userLat, userLng),
                    CLLocationCoordinate2DMake(customerLat, customerLng),
                )
                // Polyline drawing via delegate is complex; just show markers for iOS
            }

            mapView
        },
        update = { mapView ->
            mapView.showsUserLocation = true
        },
    )
}
