package com.jehadalomour.flowvan.core.data.repository

import com.jehadalomour.flowvan.core.database.dao.LocationPointDao
import com.jehadalomour.flowvan.core.database.entity.LocationPointEntity
import com.jehadalomour.flowvan.core.data.location.LatLng
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class LocationRepository(private val locationPointDao: LocationPointDao) {

    @OptIn(ExperimentalTime::class)
    suspend fun savePoint(
        shiftId: String,
        userId: String,
        latLng: LatLng,
        accuracy: Float?,
        recordedAtMs: Long? = null,
    ) {
        locationPointDao.insert(
            LocationPointEntity(
                shiftId = shiftId,
                userId = userId,
                lat = latLng.lat,
                lng = latLng.lng,
                accuracy = accuracy,
                recordedAt = recordedAtMs ?: Clock.System.now().toEpochMilliseconds(),
                synced = false,
            ),
        )
        // F12 §4: bound the offline queue to the newest QUEUE_CAP pending pings.
        locationPointDao.trimQueueToCap(QUEUE_CAP)
    }

    private companion object {
        const val QUEUE_CAP = 5000
    }
}
