# 📱 Starosta Messenger

A modern, production-ready Android messenger app built with **Kotlin**, **Jetpack Compose**, and **Firebase**.

---

## ✨ Features

- 📞 **Phone Auth** — SMS OTP via Firebase Authentication  
- 💬 **Real-time Chats** — Firestore snapshot listeners  
- 📷 **Media Sharing** — Send images from gallery  
- 🔔 **Push Notifications** — FCM foreground/background  
- ✏️ **Message Actions** — Reply, edit, delete  
- 📌 **Pin Chats** — Long-press to pin/unpin  
- 🔍 **Search** — Filter chats and contacts  
- 👤 **Profile** — Edit name, username, status, photo  
- 🌙 **Dark/Light Theme** — Material 3 dynamic color  
- ⌨️ **Typing Indicator** — Real-time typing state  
- 📚 **Pagination** — Load older messages on scroll  

---

## 🏗️ Architecture

```
MVVM + Repository Pattern + Hilt DI
├── feature/          # Screen-level: ViewModel + Compose UI
│   ├── auth/         # Phone input, OTP
│   ├── chats/        # Chat list, new chat
│   ├── chat/         # Message screen
│   ├── contacts/     # Contacts list
│   └── profile/      # User profile & edit
├── data/
│   ├── model/        # Kotlin data classes (User, Chat, Message, TypingState)
│   ├── remote/       # Firebase data sources
│   └── repository/   # Business logic layer
├── core/
│   ├── navigation/   # NavGraph, Routes
│   ├── ui/theme/     # Material 3 colors, typography
│   └── util/         # Resource wrapper, TimeUtils
├── di/               # Hilt modules (Firebase, Repository)
└── service/          # FCM service
```

**Tech Stack:**
- Kotlin + Coroutines + Flow
- Jetpack Compose + Material 3
- Navigation Compose
- Hilt DI
- Firebase Auth + Firestore + Storage + FCM
- Coil (image loading)

---

## 🚀 Quick Start

### 1. Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17+
- Android SDK 26+
- Firebase account

### 2. Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project: `starosta-messenger`
3. Add an Android app:
   - **Package name:** `com.starosta.messenger`
   - **App nickname:** Starosta Messenger
4. Download `google-services.json`

### 3. Configure Firebase Services

Enable all of these in your Firebase project:

| Service | Path |
|---------|------|
| Authentication | Auth → Sign-in methods → Phone |
| Firestore | Firestore Database → Create database |
| Storage | Storage → Get started |
| Cloud Messaging | Messaging → (auto-enabled) |

### 4. Add google-services.json

Place the downloaded file here:
```
app/
└── google-services.json   ← here
```

> ⚠️ **Never commit this file.** It's already in `.gitignore`.

### 5. Build & Run

```bash
# Sync Gradle
./gradlew build

# Run on device/emulator
./gradlew installDebug
```

Or open the project in Android Studio → Sync → Run.

> 📱 **Phone Auth requires a real device** for SMS delivery. Use emulator only with test phone numbers configured in Firebase Console.

---

## 🔐 Firestore Security Rules

Apply these rules in **Firebase Console → Firestore → Rules**:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function isSignedIn() { return request.auth != null; }
    function isOwner(uid) { return request.auth.uid == uid; }

    // Users: anyone signed in can read, only owner can write
    match /users/{userId} {
      allow read: if isSignedIn();
      allow write: if isSignedIn() && isOwner(userId);
    }

    // Chats: participant check
    match /chats/{chatId} {
      allow read, write: if isSignedIn();
      match /messages/{messageId} {
        allow read, write: if isSignedIn();
      }
    }

    // Typing indicators
    match /typing/{docId} {
      allow read, write: if isSignedIn();
    }

    // Per-user chat index
    match /userChats/{userId}/items/{chatId} {
      allow read, write: if isSignedIn() && isOwner(userId);
    }
  }
}
```

---

## 🗄️ Firestore Data Structure

```
users/{userId}
  - id, phone, name, username, photoUrl
  - statusText, online, lastSeen, fcmToken

chats/{chatId}
  - id, title, type (private|group)
  - participantIds[], pinnedBy[]
  - lastMessageText, lastMessageAt, lastMessageSenderId

chats/{chatId}/messages/{messageId}
  - id, chatId, senderId, text, type
  - fileUrl, replyToMessageId, replyToText
  - status (sent|delivered|read), edited
  - deletedAt (soft delete), createdAt

typing/{chatId}_{userId}
  - chatId, userId, isTyping, updatedAt
```

---

## 📦 Firebase Storage Rules

Apply in **Firebase Console → Storage → Rules**:

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /profiles/{userId}/{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    match /chats/{chatId}/{allPaths=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

---

## 🔔 Push Notifications (FCM)

FCM token is stored in `users/{userId}.fcmToken` and updated automatically when the app starts.

To send push notifications from your backend / Cloud Functions:

```javascript
// Cloud Function example (Node.js)
admin.messaging().send({
  token: recipientFcmToken,
  notification: {
    title: senderName,
    body: messageText
  },
  data: { chatId: chatId }
});
```

---

## 🛣️ Roadmap

- [ ] Group chat with admin controls
- [ ] Voice messages
- [ ] Video/file attachments
- [ ] End-to-end encryption (Signal protocol)
- [ ] WebRTC voice/video calls
- [ ] Message reactions
- [ ] Read receipts per-user
- [ ] Contact sync from device phonebook
- [ ] Channels & broadcasts
- [ ] Message forwarding

---

## 📄 License

MIT License — see [LICENSE](LICENSE) for details.