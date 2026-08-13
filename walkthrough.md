# CloudBeats — Implementation Walkthrough

## What Was Built

A complete native Android (Kotlin) music player app that streams music from OneDrive. 

**Most importantly:** This app uses a custom OAuth flow with a public Client ID, meaning **zero Azure setup is required by you**.

---

## Project Structure

```
cloud-beatsxx/
├── settings.gradle.kts              # Root project config
├── build.gradle.kts                  # Root build file
├── gradle.properties                 # JVM & Android settings
└── app/
    ├── build.gradle.kts              # App module build file
    └── src/main/
        ├── AndroidManifest.xml       # Permissions, activities, service
        └── java/com/cloudbeats/app/
            ├── MainActivity.kt            # Single activity
            ├── auth/
            │   ├── AuthManager.kt         # Custom OAuth using Local Socket
            │   └── AuthState.kt           # Auth state sealed class
            ├── data/
            │   ├── local/               # Room DB, DAOs, Entities
            │   ├── remote/              # OneDriveService (Graph API wrapper)
            │   └── repository/          # MusicRepository, PlaylistRepository
            ├── player/                  # CloudBeatsService, PlaybackManager
            ├── download/                # DownloadManager
            ├── di/                      # Hilt Modules
            └── ui/                      # Jetpack Compose Screens & Theme
```

---

## Key Architecture Decisions

| Decision | Rationale |
|:---|:---|
| **No Azure Registration** | Bypasses Microsoft's strict requirement for credit cards. Uses the public `rclone` Client ID. |
| **Local Socket Auth** | When logging in, the app spins up a tiny temporary web server on port `53682` to catch the token from the browser, completely avoiding Android Intent verification issues. |
| **OkHttp + JSON** | Keeps APK lighter than the heavyweight Graph SDK. |
| **Room as Source of Truth** | UI always reads from local DB; sync happens in background. |
| **URL caching (50-min expiry)** | OneDrive URLs expire at ~60 min; refresh at 50 min avoids playback interruption. |
| **MediaSessionService** | Background playback survives app close; integrates with lock screen. |

---

## Next Steps for You

### Step 1: Install Android Studio
1. Download [Android Studio](https://developer.android.com/studio)
2. Open the project at `c:\Users\dhanu\Documents\AUDIOAI\cloud-beatsxx`
3. Let Gradle sync and download dependencies.

### Step 2: Configure Your Music Folder
By default, the app scans `spotify_downloads` in your OneDrive root. To change this, update the folder path in `HomeViewModel.kt` (line 50).

### Step 3: Run and Sign In
1. Connect your Android phone or use an emulator.
2. Click **Run**.
3. When the app opens, click **Sign in with Microsoft**.
4. Your browser will open. You will see a prompt saying **"rclone wants to access your files"**. 
5. **Accept the prompt.** The browser will instantly close and you'll be signed in!

---

## Features Implemented

- ✅ Stream music from OneDrive (no full download needed)
- ✅ Download individual songs for offline playback
- ✅ Background playback with lock screen controls
- ✅ Playlist creation, management, and playback
- ✅ Real-time search across title, artist, album
- ✅ Sort by title, artist, album, date added, most played
- ✅ Play count tracking and recently played
- ✅ Zero Azure setup required!
