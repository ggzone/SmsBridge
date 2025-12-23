package com.ggz.smsbridge

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val settingsDataStore = SettingsDataStore(context)

    val appSettings: StateFlow<AppSettings> = settingsDataStore.appSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings(
            smsNumber = "",
            smsRegex = "",
            uploadMethod = "email",
            recipientEmail = "",
            emailUser = "",
            emailPass = "",
            emailServer = "",
            emailPort = "",
            emailSsl = true,
            apiUrl = "",
            publicKey = "",
            enableEncryption = false,
            notifyOnNewCode = true,
            isListening = false
        )
    )

    fun saveSettings(settings: AppSettings) {
        viewModelScope.launch {
            settingsDataStore.saveSettings(settings)
        }
    }
}