package com.jehadalomour.flowvan.shared.data.repository

import com.jehadalomour.flowvan.shared.data.local.dao.AppSettingsDao
import com.jehadalomour.flowvan.shared.data.local.mapper.toDomain
import com.jehadalomour.flowvan.shared.data.local.mapper.toEntity
import com.jehadalomour.flowvan.shared.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppSettingsRepository(private val dao: AppSettingsDao) {

    fun observe(): Flow<AppSettings> =
        dao.observe().map { it?.toDomain() ?: AppSettings() }

    suspend fun get(): AppSettings = dao.get()?.toDomain() ?: AppSettings()

    suspend fun save(settings: AppSettings) = dao.upsert(settings.toEntity())
}
