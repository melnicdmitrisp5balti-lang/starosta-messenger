package com.starosta.messenger.feature.contacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.starosta.messenger.core.ui.theme.ColorOffline
import com.starosta.messenger.core.ui.theme.ColorOnline
import com.starosta.messenger.core.util.TimeUtils
import com.starosta.messenger.data.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    onContactClick: (String) -> Unit,
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.navigateToChatId) {
        uiState.navigateToChatId?.let { chatId ->
            onContactClick(chatId)
            viewModel.clearNavigation()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Contacts") })
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearch,
                    placeholder = { Text("Search contacts...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.filteredContacts.isEmpty()) {
                Text(
                    text = if (uiState.searchQuery.isBlank()) "No contacts found" else "No results",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn {
                    items(uiState.filteredContacts, key = { it.id }) { user ->
                        ContactItem(
                            user = user,
                            onClick = { viewModel.openChat(user.id) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 76.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactItem(user: User, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            if (user.photoUrl.isNotEmpty()) {
                AsyncImage(
                    model = user.photoUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(50.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = (user.name.firstOrNull() ?: '?').toString().uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            // Online indicator
            Surface(
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.BottomEnd),
                shape = CircleShape,
                color = if (user.online) ColorOnline else ColorOffline
            ) {}
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name.ifEmpty { user.phone },
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = if (user.online) "online"
                       else if (user.lastSeen > 0) TimeUtils.formatLastSeen(user.lastSeen)
                       else user.statusText,
                style = MaterialTheme.typography.bodySmall,
                color = if (user.online) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
