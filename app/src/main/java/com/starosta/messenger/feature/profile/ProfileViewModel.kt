package com.starosta.messenger.feature.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starosta.messenger.core.util.Resource
import com.starosta.messenger.data.model.User
import com.starosta.messenger.data.repository.AuthRepository
import com.starosta.messenger.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val user: User? = null,
    val isEditing: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser()
            _uiState.update { it.copy(isLoading = false, user = user) }
        }
    }

    fun startEditing() {
        _uiState.update { it.copy(isEditing = true) }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(isEditing = false) }
    }

    fun saveProfile(name: String, username: String, statusText: String) {
        val current = _uiState.value.user ?: return
        val updated = current.copy(
            name = name.trim(),
            username = username.trim(),
            statusText = statusText.trim()
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = userRepository.updateUser(updated)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(isSaving = false, user = updated, isEditing = false, successMessage = "Profile updated!")
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isSaving = false, error = result.message) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun uploadPhoto(uri: Uri) {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = userRepository.uploadAndUpdatePhoto(userId, uri)) {
                is Resource.Success -> {
                    val updated = _uiState.value.user?.copy(photoUrl = result.data)
                    _uiState.update { it.copy(isSaving = false, user = updated) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isSaving = false, error = result.message) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.updateOnlineStatus(false)
            authRepository.signOut()
        }
    }
}
