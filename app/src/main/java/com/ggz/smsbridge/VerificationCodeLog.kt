package com.ggz.smsbridge

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "verification_code_logs")
data class VerificationCodeLog(
    @PrimaryKey
    val timestamp: Long,
    val sender: String,
    val smsBody: String,
    val code: String,
    val uploadMethod: String,
    val uploadStatus: String,
    val failureReason: String? = null
)
