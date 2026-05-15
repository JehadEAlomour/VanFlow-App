package com.jehadalomour.flowvan.shared.data.repository

import com.jehadalomour.flowvan.shared.data.local.dao.LocationPointDao
import com.jehadalomour.flowvan.shared.data.local.entity.LocationPointEntity
import com.jehadalomour.flowvan.shared.data.location.LatLng
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class LocationRepository(private val locationPointDao: LocationPointDao) {

    @OptIn(ExperimentalTime::class)
    suspend fun savePoint(shiftId: String, userId: String, latLng: LatLng, accuracy: Float?) {
        val nowMs = Clock.System.now().toEpochMilliseconds()
        locationPointDao.insert(
            LocationPointEntity(
                shiftId = shiftId,
                userId = userId,
                lat = latLng.lat,
                lng = latLng.lng,
                accuracy = accuracy,
                recordedAt = nowMs,
                synced = false,
            ),
        )
    }
}
