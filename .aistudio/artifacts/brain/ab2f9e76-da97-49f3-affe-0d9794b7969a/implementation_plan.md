# AnimeLib Player — Offline Video Player & File Explorer

Transforming the application into **AnimeLib Player**, a dedicated offline video player featuring a built-in video file browser with folder grouping, search, watch progress tracking via Room database, and the full suite of advanced player controls (gestures, speed, aspect ratio resize, 3D surround sound, audio tracks, and subtitles).

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following product decisions were confirmed during the interactive clarification phase:

- **Confirmed Decision 1 (File Manager)**: Device storage is scanned via `MediaStore.Video` with automatic folder grouping, search filter, and sorting by date, title, or size.
- **Confirmed Decision 2 (Player Feature Set)**: The rich player controls from the original app are retained: volume/brightness/seek gestures, double-tap skip, hold-to-2x speed, resize modes (Fit/Crop/Stretch), audio track switching, subtitle track switching, and 3D surround sound.
- **Confirmed Decision 3 (Watch Progress)**: Playback position, duration, and last watched timestamps are stored locally in Room database to provide seamless "Resume" capabilities.
- **App Identity & Version**: Name updated to `AnimeLib Player`, version reset to pre-release `1.0.0-beta` (`versionCode = 1`, `versionName = "1.0.0-beta"`).

---

### 1. Overview & Core Concept

- **What It Does**: Scans and displays local video files on the user's device organized by folders with search, thumbnail generation, video duration, and playback progress indicators. Tapping a video launches the player with full gesture support and audio/video controls.
- **Target Audience**: Users looking for an anime-styled, responsive offline video player with advanced playback customization and zero network dependencies.
- **Key Value**: A fast, privacy-focused offline player maintaining AnimeLib's signature visual styling and player experience without extraneous web views, login prompts, or API overhead.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Launch & Permissions**:
   - On first launch, the app gracefully requests storage permission (`READ_MEDIA_VIDEO` on Android 13+, `READ_EXTERNAL_STORAGE` on older versions).
   - Once granted, videos are indexed from MediaStore and grouped by directories (e.g., *Downloads*, *Movies*, *Anime*, *Telegram*).
2. **File Explorer (MainActivity)**:
   - **Header Bar**: Displays `AnimeLib Player` title, search button/bar, sort options (Date, Name, Size), and folder filter chips.
   - **Video Grid/List**: Displays video thumbnail preview, title, folder source, file size, formatted duration, and a progress bar showing watched percentage if previously opened.
   - **Empty / Zero State**: Friendly illustration and message when no video files are found or permissions are pending.
3. **Playback (VideoPlayerActivity)**:
   - Tapping any video immediately opens `VideoPlayerActivity`.
   - Automatically resumes from the last saved position if watched previously (with an optional "Resume from XX:XX" toast or prompt).
   - Full player features accessible: vertical swipe for volume and brightness, horizontal swipe to seek, double-tap skip (configurable duration), hold-to-speed (2x), speed sheet (0.25x – 3.0x), resize mode (Fit, Fill/Crop, Stretch), 3D surround sound settings, and embedded audio/subtitle track selector.
   - Top bar displays the video filename and close button; previous/next controls allow navigating between videos in the same folder.

#### Visual Identity & Theme
- **Color Tokens**: Preserves AnimeLib's modern dark theme palette (`#121212` background, `#8A2BE2` / `#6200EE` purple primary accents, dark elevated cards, white primary typography, muted secondary text).
- **Typography**: Retains the bundled `open_sans` font family for crisp legibility.
- **Animations & Feedback**: Card ripple effects, smooth progress bars, and Material 3 bottom sheets.

---

### 3. Key Product Decisions & Trade-Offs

#### 1. Stripping Network APIs & WebView
- **Scope**: Completely remove network API layers (`ApiService`, `KodikLinksExtractor`, `TokenResponse`, etc.), WebView scripts/controllers (`MainWebViewController`, `MainAuthController`, JS injection assets), web comments (`PlayerCommentsController`, `CommentsManager`), voiceovers catalog from online servers, and update checks.
- **Rationale**: Eliminates unnecessary network traffic, removes API breakage risk, drastically reduces APK size, and ensures 100% offline reliability.

#### 2. Local File & Folder Organization
- **Chosen Approach**: MediaStore content resolver query for `MediaStore.Video.Media.EXTERNAL_CONTENT_URI` with fallback file system checks. Folders are extracted from `MediaStore.Video.Media.BUCKET_DISPLAY_NAME` or directory paths.
- **Why**: Standard Android practice that operates with low memory overhead, provides fast thumbnail generation via `ContentResolver.loadThumbnail`, and respects Android scoped storage policies.

#### 3. Room Watch Progress & Preferences
- **Chosen Approach**: Retain and streamline the Room `AppDatabase` to store `VideoPlaybackHistoryEntity` (filePath, title, duration, lastPositionMs, lastWatchedTimestamp) and `PlayerPreferences` (resize mode, default speed, skip duration, 3D sound settings).
- **Why**: Instant save on pause/exit; automatically populates progress indicators in the file explorer.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                   AnimeLib Player                      │
└────────────────────────────────────────────────────────┘
                           │
        ┌──────────────────┴──────────────────┐
        ▼                                     ▼
┌──────────────────────────────┐    ┌──────────────────────────────────┐
│         MainActivity         │    │       VideoPlayerActivity        │
│  (Custom File Explorer)      │    │    (ExoPlayer Offline Engine)    │
│  - MediaStore Scanner        │    │  - Gestures (Brightness/Vol/Seek)│
│  - Folder Grouping Chips     │    │  - Speed (0.25x - 3.0x)          │
│  - Instant Search & Sort     │    │  - Aspect Ratio (Fit/Crop/Scale) │
│  - Thumbnail & Progress Card │    │  - 3D Surround Audio Engine      │
└──────────────┬───────────────┘    │  - Subtitle & Audio Track Picker │
               │                    └────────────────┬─────────────────┘
               │ Launches Intent with file URI       │ Saves progress
               ▼                                     ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    Room Database (AppDatabase)                       │
│  - VideoPlaybackHistoryDao: lastPositionMs, durationMs, timestamp    │
│  - PlayerPreferencesDao: audio, gestures, resize, skip duration      │
└──────────────────────────────────────────────────────────────────────┘
```

#### Class Refactoring & Cleanup Map
- **Retained & Refactored**:
  - `MainActivity`: Rebuilt from scratch as the Video File Browser.
  - `VideoPlayerActivity`: Stripped of online API calls, web panels, and comments; focused on file URI playback, playlist navigation across directory videos, and offline bottom sheets.
  - Controllers/Managers kept: `PlayerPlaybackController`, `PlayerAudioController`, `PlayerOrientationController`, `PlayerLockController`, `SurroundSoundManager`, `GesturesManager`, `VerticalGesturesManager`, `VideoFiltersManager`.
  - Offline Bottom Sheets: `SpeedBottomSheet`, `ResizeModeBottomSheet`, `SubtitlesBottomSheet`, `SurroundSoundBottomSheet`, `SkipDurationBottomSheet`.
- **Removed (Obsolete)**:
  - WebView controllers & JS assets: `MainWebViewController`, `MainAuthController`, `JSInjectionsHandler`, `assets/js/*`.
  - API & Web Models: `ApiService`, `KodikLinksExtractor`, `ApiData`, `ApiResponse`, `CommentsResponse`, `EpisodesListResponse`, `KodikResponse`, `RelatedTitlesResponse`, `SearchResponse`, `UpdateInfo`, `UrlInputActivity`, `UpdateActivity`.
  - Web Panels & Dialogs: `PlayerCommentsController`, `CommentsManager`, `CommentSettingsBottomSheet`, `VoiceoverBottomSheet`, `SiteSelectionBottomSheet`, `RulesBottomSheet`, `BugReportBottomSheet`, `TitleWebViewBottomSheet`.
  - Cloud / Remote Sync: `PendingSyncTaskDao`, `OfflineSyncManager`, `OfflineSyncWorker`.

---

### 5. Step-by-Step Implementation Strategy

1. **Phase 1: Project & Version Configuration**
   - Update `app/build.gradle`: set `versionCode 1`, `versionName "1.0.0-beta"`.
   - Update `res/values/strings.xml` and `metadata.json`: set `app_name` and `name` to `AnimeLib Player`.
   - Declare permissions: `READ_MEDIA_VIDEO` and `READ_EXTERNAL_STORAGE` in `AndroidManifest.xml`. Add video viewing intent filters for `VideoPlayerActivity`.
2. **Phase 2: Database & Data Models**
   - Update `AppDatabase` entities: keep `PlayerPreferences` and introduce `VideoPlaybackHistoryEntity`.
   - Create `VideoItem` and `VideoFolder` domain models for the file explorer.
3. **Phase 3: Custom File Explorer UI & Logic (MainActivity)**
   - Create clean M3 layout for `activity_main.xml` (Search bar, folder chips carousel, video RecyclerView, empty state).
   - Implement `VideoAdapter` with thumbnail loading, title, folder tag, formatted duration, and progress bar.
   - Implement storage scanning logic with folder grouping, search query filtering, and sorting.
4. **Phase 4: Player Streamlining (VideoPlayerActivity)**
   - Clean up `VideoPlayerActivity` and `activity_video_player.xml`: remove comments panel, voiceover panel, online info views, and API calls.
   - Wire local playback from file Uri, support Next/Prev video in directory, and persist playback progress to Room on pause/stop.
5. **Phase 5: Cleanup & Deletion of Obsolete Code**
   - Delete obsolete API classes, webview JS assets, and unused layouts.
6. **Phase 6: Verification & Compilation**
   - Run `compile_applet` and verify zero compilation errors and clean execution.
