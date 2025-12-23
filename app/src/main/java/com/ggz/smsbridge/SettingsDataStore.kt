package com.ggz.smsbridge

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val smsNumber: String,
    val smsRegex: String,
    val uploadMethod: String,
    val recipientEmail: String,
    val emailUser: String,
    val emailPass: String,
    val emailServer: String,
    val emailPort: String,
    val emailSsl: Boolean,
    val apiUrl: String,
    val publicKey: String,
    val enableEncryption: Boolean,
    val notifyOnNewCode: Boolean,
    val isListening: Boolean
)

class SettingsDataStore(context: Context) {

    private val dataStore = context.dataStore

    val appSettings: Flow<AppSettings> = dataStore.data.map {
        AppSettings(
            smsNumber = it[PreferencesKeys.SMS_NUMBER] ?: "",
            smsRegex = it[PreferencesKeys.SMS_REGEX] ?: "",
            uploadMethod = it[PreferencesKeys.UPLOAD_METHOD] ?: "email",
            recipientEmail = it[PreferencesKeys.RECIPIENT_EMAIL] ?: "",
            emailUser = it[PreferencesKeys.EMAIL_USER] ?: "",
            emailPass = it[PreferencesKeys.EMAIL_PASS] ?: "",
            emailServer = it[PreferencesKeys.EMAIL_SERVER] ?: "",
            emailPort = it[PreferencesKeys.EMAIL_PORT] ?: "",
            emailSsl = it[PreferencesKeys.EMAIL_SSL] ?: true,
            apiUrl = it[PreferencesKeys.API_URL] ?: "",
            publicKey = it[PreferencesKeys.PUBLIC_KEY] ?: "",
            enableEncryption = it[PreferencesKeys.ENABLE_ENCRYPTION] ?: false,
            notifyOnNewCode = it[PreferencesKeys.NOTIFY_ON_NEW_CODE] ?: true,
            isListening = it[PreferencesKeys.IS_LISTENING] ?: false
        )
    }

    suspend fun saveSettings(settings: AppSettings) {
        dataStore.edit {
            it[PreferencesKeys.SMS_NUMBER] = settings.smsNumber
            it[PreferencesKeys.SMS_REGEX] = settings.smsRegex
            it[PreferencesKeys.UPLOAD_METHOD] = settings.uploadMethod
            it[PreferencesKeys.RECIPIENT_EMAIL] = settings.recipientEmail
            it[PreferencesKeys.EMAIL_USER] = settings.emailUser
            it[PreferencesKeys.EMAIL_PASS] = settings.emailPass
            it[PreferencesKeys.EMAIL_SERVER] = settings.emailServer
            it[PreferencesKeys.EMAIL_PORT] = settings.emailPort
            it[PreferencesKeys.EMAIL_SSL] = settings.emailSsl
            it[PreferencesKeys.API_URL] = settings.apiUrl
            it[PreferencesKeys.PUBLIC_KEY] = settings.publicKey
            it[PreferencesKeys.ENABLE_ENCRYPTION] = settings.enableEncryption
            it[PreferencesKeys.NOTIFY_ON_NEW_CODE] = settings.notifyOnNewCode
            it[PreferencesKeys.IS_LISTENING] = settings.isListening
        }
    }

    private object PreferencesKeys {
        val SMS_NUMBER = stringPreferencesKey("sms_number")
        val SMS_REGEX = stringPreferencesKey("sms_regex")
        val UPLOAD_METHOD = stringPreferencesKey("upload_method")
        val RECIPIENT_EMAIL = stringPreferencesKey("recipient_email")
        val EMAIL_USER = stringPreferencesKey("email_user")
        val EMAIL_PASS = stringPreferencesKey("email_pass")
        val EMAIL_SERVER = stringPreferencesKey("email_server")
        val EMAIL_PORT = stringPreferencesKey("email_port")
        val EMAIL_SSL = booleanPreferencesKey("email_ssl")
        val API_URL = stringPreferencesKey("api_url")
        val PUBLIC_KEY = stringPreferencesKey("public_key")
        val ENABLE_ENCRYPTION = booleanPreferencesKey("enable_encryption")
        val NOTIFY_ON_NEW_CODE = booleanPreferencesKey("notify_on_new_code")
        val IS_LISTENING = booleanPreferencesKey("is_listening")
    }
}