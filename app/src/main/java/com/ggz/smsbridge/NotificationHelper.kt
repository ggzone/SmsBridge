package com.ggz.smsbridge

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    private const val CHANNEL_ID_NEW_CODE = "new_verification_code"
    private const val CHANNEL_ID_DEBUG = "debug_notifications"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val newCodeChannel = NotificationChannel(
                CHANNEL_ID_NEW_CODE,
                "新验证码提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "当监控到新的验证码时发出提醒"
            }
            
            val debugChannel = NotificationChannel(
                CHANNEL_ID_DEBUG,
                "调试日志",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "用于显示原始短信内容以供调试"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(newCodeChannel)
            notificationManager.createNotificationChannel(debugChannel)
        }
    }

    fun postNewCodeNotification(context: Context, code: String, status: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_NEW_CODE)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Use a standard system icon
            .setContentTitle("获取到新的验证码")
            .setContentText("验证码: $code, 上传状态: $status")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun postRawSmsNotification(context: Context, sender: String?, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_DEBUG)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Use a standard system icon
            .setContentTitle("收到来自 '$sender' 的短信")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message)) // Expandable notification
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}