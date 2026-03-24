package com.starosta.messenger.feature.contacts

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

data class ContactsUiState(
    val isLoading: Boolean = false,
    val contacts: List<User> = emptyList(),
    val filteredContacts: List<User> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null,
    val navigateToChatId: String? = null
)

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactsUiState(isLoading = true))
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    val currentUserId: String get() = authRepository.getCurrentUserId() ?: ""

    init {
        loadContacts()
    }

    private fun loadContacts() {
        viewModelScope.launch {
            try {
                val all = chatRepository.getAllUsers().filter { it.id != currentUserId }
                _uiState.update {
                    it.copy(isLoading = false, contacts = all, filteredContacts = all)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onSearch(query: String) {
        _uiState.update { state ->
            val filtered = if (query.isBlank()) state.contacts
            else {
                val lower = query.lowercase()
                state.contacts.filter {
                    it.name.lowercase().contains(lower) ||
                    it.username.lowercase().contains(lower) ||
                    it.phone.contains(lower)
                }
            }
            state.copy(searchQuery = query, filteredContacts = filtered)
        }
    }

    fun openChat(otherUserId: String) {
        viewModelScope.launch {
            when (val result = chatRepository.getOrCreatePrivateChat(currentUserId, otherUserId)) {
                is Resource.Success -> _uiState.update { it.copy(navigateToChatId = result.data) }
                is Resource.Error -> _uiState.update { it.copy(error = result.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    fun clearNavigation() {
        _uiState.update { it.copy(navigateToChatId = null) }
    }
}
