package com.ggz.smsbridge

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first

class SmsWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val settingsDataStore = SettingsDataStore(applicationContext)
        val appSettings = settingsDataStore.appSettings.first()

        if (!appSettings.isListening) {
            Log.d("SmsWorker", "Not listening, skipping SMS processing.")
            return Result.success()
        }

        val sender = inputData.getString("sender") ?: return Result.failure()
        val messageBody = inputData.getString("messageBody") ?: return Result.failure()
        val timestamp = inputData.getLong("timestamp", 0L)
        if (timestamp == 0L) return Result.failure()

        val uploadSuccess = extractAndUploadCode(applicationContext, sender, messageBody, timestamp, appSettings)
        
        return if (uploadSuccess) Result.success() else Result.retry()
    }

    private suspend fun extractAndUploadCode(context: Context, sender: String, smsBody: String, timestamp: Long, appSettings: AppSettings): Boolean {
        val matchResult = appSettings.smsRegex.toRegex().find(smsBody)

        val code = matchResult?.groupValues?.get(1) ?: "(未找到)"
        Log.e("SmsWorker-Upload", "Extracted code: $code")

        val logData = VerificationCodeLog(
            sender = sender,
            smsBody = smsBody,
            code = code,
            timestamp = timestamp,
            uploadStatus = "未知",
            uploadMethod = "N/A",
            failureReason = if (code == "(未找到)") "无法从短信中解析出验证码" else null
        )

        if (code == "(未找到)") {
            MonitorRepository.updateLog(logData)
            return true // Don't retry parsing failures
        }

        val content = if (appSettings.enableEncryption) {
            UploadHelper.encrypt(code, appSettings.publicKey) ?: ""
        } else {
            code
        }

        var uploadStatus = "失败"
        var failureReason: String? = null
        var uploadSuccessful = false

        try {
            if (appSettings.uploadMethod == "email") {
                UploadHelper.sendEmail(appSettings.emailUser, appSettings.emailPass, appSettings.recipientEmail, appSettings.emailServer, appSettings.emailPort, appSettings.emailSsl, "验证码", content)
            } else { // http
                UploadHelper.sendToServer(content, appSettings.apiUrl)
            }
            uploadStatus = "成功"
            uploadSuccessful = true
        } catch (e: Exception) {
            Log.e("SmsWorker-Upload", "Upload failed", e)
            failureReason = e.message ?: e.toString()
            uploadSuccessful = false
        } finally {
            val finalLog = logData.copy(
                uploadStatus = uploadStatus,
                uploadMethod = appSettings.uploadMethod,
                failureReason = failureReason
            )
            MonitorRepository.updateLog(finalLog)

            if (appSettings.notifyOnNewCode) {
                NotificationHelper.postNewCodeNotification(context, code, uploadStatus)
            }
        }
        return uploadSuccessful
    }
}
