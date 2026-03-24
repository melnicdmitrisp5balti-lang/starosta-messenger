package com.starosta.messenger.core.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.starosta.messenger.feature.auth.OtpScreen
import com.starosta.messenger.feature.auth.PhoneInputScreen
import com.starosta.messenger.feature.chat.ChatScreen
import com.starosta.messenger.feature.chats.ChatListScreen
import com.starosta.messenger.feature.chats.NewChatScreen
import com.starosta.messenger.feature.contacts.ContactsScreen
import com.starosta.messenger.feature.profile.ProfileScreen

@Composable
fun NavGraph() {
    val rootNavController = rememberNavController()
    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
        Routes.MAIN
    } else {
        Routes.PHONE_INPUT
    }

    NavHost(navController = rootNavController, startDestination = startDestination) {
        composable(Routes.PHONE_INPUT) {
            PhoneInputScreen(
                onNavigateToOtp = { phone ->
                    rootNavController.navigate(Routes.otpRoute(phone))
                }
            )
        }
        composable(
            route = Routes.OTP,
            arguments = listOf(navArgument("phoneNumber") { type = NavType.StringType })
        ) { backStackEntry ->
            OtpScreen(
                phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: "",
                onAuthSuccess = {
                    rootNavController.navigate(Routes.MAIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = { rootNavController.popBackStack() }
            )
        }
        composable(Routes.MAIN) {
            MainScreen(
                onNavigateToChat = { chatId ->
                    rootNavController.navigate(Routes.chatRoute(chatId))
                },
                onNavigateToNewChat = {
                    rootNavController.navigate(Routes.NEW_CHAT)
                },
                onLogout = {
                    rootNavController.navigate(Routes.PHONE_INPUT) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Routes.CHAT,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            ChatScreen(
                chatId = backStackEntry.arguments?.getString("chatId") ?: "",
                onNavigateBack = { rootNavController.popBackStack() }
            )
        }
        composable(Routes.NEW_CHAT) {
            NewChatScreen(
                onChatCreated = { chatId ->
                    rootNavController.navigate(Routes.chatRoute(chatId)) {
                        popUpTo(Routes.NEW_CHAT) { inclusive = true }
                    }
                },
                onNavigateBack = { rootNavController.popBackStack() }
            )
        }
    }
}

sealed class BottomNavItem(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Chats : BottomNavItem("chats_tab", "Chats", Icons.Filled.Chat)
    object Contacts : BottomNavItem("contacts_tab", "Contacts", Icons.Filled.Contacts)
    object Profile : BottomNavItem("profile_tab", "Profile", Icons.Filled.Person)
}

@Composable
fun MainScreen(
    onNavigateToChat: (String) -> Unit,
    onNavigateToNewChat: () -> Unit,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val items = listOf(BottomNavItem.Chats, BottomNavItem.Contacts, BottomNavItem.Profile)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Chats.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Chats.route) {
                ChatListScreen(
                    onChatClick = onNavigateToChat,
                    onNewChat = onNavigateToNewChat
                )
            }
            composable(BottomNavItem.Contacts.route) {
                ContactsScreen(
                    onContactClick = { userId ->
                        onNavigateToChat(userId)
                    }
                )
            }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen(onLogout = onLogout)
            }
        }
    }
}
