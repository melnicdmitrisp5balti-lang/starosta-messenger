package com.starosta.messenger.feature.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.starosta.messenger.core.util.Resource
import com.starosta.messenger.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val verificationId: String? = null,
    val isOtpSent: Boolean = false,
    val isAuthenticated: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun sendOtp(phoneNumber: String, activity: Activity) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                viewModelScope.launch {
                    verifyWithCredential(credential)
                }
            }

            override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Verification failed"
                )
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    verificationId = verificationId,
                    isOtpSent = true
                )
            }
        }

        viewModelScope.launch {
            authRepository.sendOtp(phoneNumber, activity, callbacks)
        }
    }

    fun verifyOtp(otp: String) {
        val verificationId = _uiState.value.verificationId ?: run {
            _uiState.value = _uiState.value.copy(error = "Verification ID not found. Please try again.")
            return
        }
        val credential = PhoneAuthProvider.getCredential(verificationId, otp)
        viewModelScope.launch {
            verifyWithCredential(credential)
        }
    }

    private suspend fun verifyWithCredential(credential: PhoneAuthCredential) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        when (val result = authRepository.verifyOtp(credential)) {
            is Resource.Success -> {
                _uiState.value = _uiState.value.copy(isLoading = false, isAuthenticated = true)
            }
            is Resource.Error -> {
                _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
            is Resource.Loading -> Unit
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
