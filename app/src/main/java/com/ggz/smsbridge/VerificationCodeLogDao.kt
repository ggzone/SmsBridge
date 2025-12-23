package com.ggz.smsbridge

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VerificationCodeLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: VerificationCodeLog)

    @Query("SELECT * FROM verification_code_logs ORDER BY timestamp DESC")
    fun getAll(): Flow<List<VerificationCodeLog>>

    @Query("SELECT * FROM verification_code_logs WHERE timestamp >= :todayStart ORDER BY timestamp DESC")
    fun getTodayLogs(todayStart: Long): Flow<List<VerificationCodeLog>>

    @Query("DELETE FROM verification_code_logs")
    suspend fun clearAll()

    @Query("DELETE FROM verification_code_logs WHERE timestamp < :timestamp")
    suspend fun clearOld(timestamp: Long)
}
