package com.jehadalomour.flowvan.core.data.repository

import com.jehadalomour.flowvan.core.database.dao.AppSettingsDao
import com.jehadalomour.flowvan.core.database.mapper.toDomain
import com.jehadalomour.flowvan.core.database.mapper.toEntity
import com.jehadalomour.flowvan.core.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppSettingsRepository(private val dao: AppSettingsDao) {

    fun observe(): Flow<AppSettings> =
        dao.observe().map { it?.toDomain() ?: AppSettings() }

    suspend fun get(): AppSettings = dao.get()?.toDomain() ?: AppSettings()

    suspend fun save(settings: AppSettings) = dao.upsert(settings.toEntity())
}
