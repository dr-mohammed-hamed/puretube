# Quickstart & Verification Guide: 003-player-engine

**Feature Branch / Directory**: `specs/003-player-engine`  
**Spec Reference**: [spec.md](file:///d:/b/puretube/specs/003-player-engine/spec.md)  

---

## 1. Prerequisites & Setup

1. Android Studio Ladybug / Koala or CLI with Android SDK 36, JDK 17.
2. An active internet connection for YouTube DASH video stream extraction via `NewPipeExtractor`.
3. An Android device or emulator running Android 8.0+ (API 26+) for PiP support (Android 12+ recommended for auto-PiP).

---

## 2. Automated Testing

Run the automated unit tests covering player manager state transitions, stream model parsing, and history resume persistence:

```bash
# Run unit tests
./gradlew test

# Assemble debug APK to ensure 0 build regressions
./gradlew assembleDebug
```

---

## 3. Manual PM Verification Scenarios

### Scenario 1: Video Playback & DASH Stream Merging
1. Launch PureTube.
2. In the Feed tab or Watch Later tab, tap any video card.
3. **Verify**:
   - `PlayerScreen` opens smoothly.
   - Video and audio are perfectly synchronized.
   - Duration badge matches actual video length.
   - Time elapsed updates cleanly in mm:ss format.

### Scenario 2: Audio-Only Background Playback & Lock Screen Controls
1. While a video is playing, tap the "صوتي فقط" (Audio-Only) button on the player controls.
2. **Verify**: Video surface dims or hides, and audio continues playing smoothly.
3. Press the device Power button to lock the screen.
4. **Verify**: Audio continues playing uninterrupted without stutter.
5. Wake the screen (without unlocking).
6. **Verify**: Lock screen notification displays title, channel name, play/pause toggle, and seek buttons.

### Scenario 3: Touch Gestures (Double-Tap Seek)
1. During video playback, double-tap quickly on the right third of the video screen.
2. **Verify**: Video jumps forward by 10 seconds; a "+10" vector icon indicator animates.
3. Double-tap quickly on the left third of the video screen.
4. **Verify**: Video jumps backward by 10 seconds; a "-10" vector icon indicator animates.

### Scenario 4: Picture-in-Picture (PiP)
1. Start playing any video.
2. Perform the Android home gesture or tap the device Home button.
3. **Verify**:
   - The app transitions seamlessly into a floating PiP window.
   - All scrubber bars, title headers, and buttons are hidden; only the clean video is visible.
4. Tap the PiP window to expand back to fullscreen.
5. **Verify**: Controls reappear with playback position intact.

### Scenario 5: History Resume Sync
1. Play a video for 30 seconds, then exit the player.
2. Re-open the same video from the Feed or History.
3. **Verify**: The player seeks automatically to the 30-second mark and resumes without manual scrubbing.
