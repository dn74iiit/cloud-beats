package com.cloudbeats.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.cloudbeats.app.auth.AuthManager
import com.cloudbeats.app.auth.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {

    val authState: StateFlow<AuthState> = authManager.authState

    fun signOut() {
        authManager.signOut()
    }
}
