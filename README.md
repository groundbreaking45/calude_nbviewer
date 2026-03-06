# NbViewer

Offline Jupyter Notebook (`.ipynb`) viewer for Android.

- **No internet required** — works fully offline after install
- **No code execution** — reads notebooks as static content
- **No bundled runtimes** — pure native Android (Kotlin)
- Opens `.ipynb` files from any file manager via "Open with"

## Getting the APK

### Option A — GitHub Actions (recommended)

1. Push this repository to GitHub
2. Go to **Actions** tab → **Build APK** workflow
3. Click **Run workflow** (or push any commit)
4. Download `NbViewer-debug` from the workflow artifacts

The debug APK installs directly on any Android device with "Unknown sources" enabled.

### Option B — Build locally

Requirements: Android Studio Hedgehog or newer, JDK 17

```bash
# One-time: generate the gradle wrapper jar
gradle wrapper --gradle-version 8.4

# Build
./gradlew assembleDebug

# APK location
app/build/outputs/apk/debug/app-debug.apk
```

## ⚠️ One Required Step Before First Push

The Gradle wrapper jar (`gradle/wrapper/gradle-wrapper.jar`) is a binary file
that is not committed to this repository (Android best practice).

**Run this once on your machine before pushing:**

```bash
# Install Gradle if not present: https://gradle.org/install/
gradle wrapper --gradle-version 8.4
```

This generates `gradle/wrapper/gradle-wrapper.jar`. Commit it:

```bash
git add gradle/wrapper/gradle-wrapper.jar
git commit -m "Add gradle wrapper jar"
git push
```

After that, GitHub Actions handles all builds automatically.

## Project Architecture

```
Clean Architecture — 3 layers, zero cross-layer leakage

Data     com.nbviewer.data        JSON parsing, file I/O, DTO mapping
Domain   com.nbviewer.domain      Business models, use cases (pure Kotlin)
UI       com.nbviewer.presentation ViewModels, Fragments, RecyclerView
```

See `project_state.json` for the full architecture decision record.

## Supported Notebook Format

- nbformat 4.x (all standard Jupyter notebooks)
- Cell types: markdown, code, raw
- Code outputs: stream text, execute_result text, error tracebacks
- Image outputs: placeholder text (image rendering is a future milestone)

## Minimum Requirements

- Android 8.0 (API 26)
- ~5MB storage
- Works on low-end devices (tested profile: 512MB RAM)
