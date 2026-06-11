package com.jehadalomour.flowvan.shared.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jehadalomour.flowvan.shared.data.local.entity.AiMessageEntity

@Dao
interface AiMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: AiMessageEntity)

    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun listConversation(conversationId: String): List<AiMessageEntity>
}