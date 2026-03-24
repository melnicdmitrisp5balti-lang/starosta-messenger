package com.starosta.messenger.feature.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starosta.messenger.core.util.Resource
import com.starosta.messenger.data.model.User
import com.starosta.messenger.data.repository.AuthRepository
import com.starosta.messenger.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewChatUiState(
    val isLoading: Boolean = false,
    val users: List<User> = emptyList(),
    val filteredUsers: List<User> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null,
    val createdChatId: String? = null
)

@HiltViewModel
class NewChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewChatUiState(isLoading = true))
    val uiState: StateFlow<NewChatUiState> = _uiState.asStateFlow()

    val currentUserId: String get() = authRepository.getCurrentUserId() ?: ""

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            try {
                val allUsers = chatRepository.getAllUsers()
                val filtered = allUsers.filter { it.id != currentUserId }
                _uiState.update { it.copy(isLoading = false, users = filtered, filteredUsers = filtered) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onSearch(query: String) {
        _uiState.update { state ->
            val filtered = if (query.isBlank()) {
                state.users
            } else {
                val lower = query.lowercase()
                state.users.filter {
                    it.name.lowercase().contains(lower) ||
                    it.username.lowercase().contains(lower) ||
                    it.phone.contains(lower)
                }
            }
            state.copy(searchQuery = query, filteredUsers = filtered)
        }
    }

    fun startChatWith(otherUserId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = chatRepository.getOrCreatePrivateChat(currentUserId, otherUserId)) {
                is Resource.Success -> _uiState.update { it.copy(isLoading = false, createdChatId = result.data) }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Resource.Loading -> Unit
            }
        }
    }
}
