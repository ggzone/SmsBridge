package com.ggz.smsbridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

class SmsReceiver : BroadcastReceiver() {

    private val DEBUG_TAG = "SmsReceiver-Debug"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(DEBUG_TAG, "onReceive triggered. Action: ${intent.action}")

        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) {
            return
        }

        val settingsDataStore = SettingsDataStore(context)
        val appSettings = runBlocking { settingsDataStore.appSettings.first() }

        if (!appSettings.isListening) {
            Log.d(DEBUG_TAG, "Listener is disabled. Ignoring message.")
            return
        }

        for (smsMessage in messages) {
            val sender = smsMessage.originatingAddress ?: "Unknown Sender"
            val messageBody = smsMessage.messageBody

            Log.d(DEBUG_TAG, "--- New SMS Received ---")
            Log.d(DEBUG_TAG, "From: $sender")
            Log.d(DEBUG_TAG, "Body: $messageBody")

            if (appSettings.smsNumber.isNotBlank() && sender.contains(appSettings.smsNumber)) {
                Log.d(DEBUG_TAG, "Sender matched! Scheduling work...")

                val timestamp = System.currentTimeMillis()
                val initialLog = VerificationCodeLog(
                    sender = sender,
                    smsBody = messageBody,
                    code = "(解析中...)",
                    timestamp = timestamp,
                    uploadStatus = "处理中",
                    uploadMethod = "N/A",
                    failureReason = null
                )
                MonitorRepository.addLog(initialLog)

                val workData = Data.Builder()
                    .putString("sender", sender)
                    .putString("messageBody", messageBody)
                    .putLong("timestamp", timestamp)
                    .build()

                val workRequest = OneTimeWorkRequestBuilder<SmsWorker>()
                    .setInputData(workData)
                    .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        10, // min backoff value
                        TimeUnit.SECONDS
                    )
                    .build()

                WorkManager.getInstance(context).enqueue(workRequest)
                break
            } else {
                Log.d(DEBUG_TAG, "Sender did NOT match. Skipping. Sender: '$sender', Target: '${appSettings.smsNumber}'")
            }
        }
    }
}