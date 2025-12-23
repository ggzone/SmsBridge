package com.ggz.smsbridge

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Calendar

object MonitorRepository {
    private lateinit var logDao: VerificationCodeLogDao
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun init(context: Context) {
        logDao = AppDatabase.getDatabase(context).verificationCodeLogDao()
    }

    fun getLogs(): Flow<List<VerificationCodeLog>> {
        return logDao.getAll()
    }

    fun getTodayLogs(): Flow<List<VerificationCodeLog>> {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return logDao.getTodayLogs(todayStart)
    }

    fun addLog(log: VerificationCodeLog) {
        coroutineScope.launch {
            logDao.insert(log)
        }
    }

    fun updateLog(log: VerificationCodeLog) {
        coroutineScope.launch {
            logDao.insert(log)
        }
    }

    fun clearAllLogs() {
        coroutineScope.launch {
            logDao.clearAll()
        }
    }

    fun clearOldLogs(days: Int) {
        coroutineScope.launch {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -days)
            val cutoffDate = calendar.timeInMillis
            logDao.clearOld(cutoffDate)
        }
    }
}
