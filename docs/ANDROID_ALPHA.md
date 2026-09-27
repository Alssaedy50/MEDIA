# MEDIA Android Alpha

MEDIA Android Alpha is a thin Android WebView shell around the existing MEDIA Web/API Alpha. It reuses the existing student-facing web experience rather than duplicating UI or medical-answering logic.

## Architecture

Android app → WebView → MEDIA Web/API → retrieval/evidence-aware answer engine.

The Android layer contains no medical model, corpus, secrets, or duplicated answer logic. The backend/API contract remains the source of truth.

## Build

The Android project lives in `android/` and uses the module in `android-app/`.

A Gradle 8.9+ installation and Android SDK are required.

From the repository root:

    cd android
    gradle :android-app:assembleDebug

The debug APK is produced at:

    android-app/build/outputs/apk/debug/android-app-debug.apk

### Backend URL

The default URL is `http://10.0.2.2:8000/`, convenient for the Android Emulator when FastAPI runs on the development machine.

For a physical device, build with a reachable backend address:

    gradle -PMEDIA_WEB_URL=http://192.168.1.20:8000/ :android-app:assembleDebug

For a deployed backend, use its HTTPS URL:

    gradle -PMEDIA_WEB_URL=https://your-media-host.example/ :android-app:assembleDebug

Do not commit API keys, tokens, passwords, or private credentials. The URL is configuration only.

## Run the backend

From the repository root:

    python -m pip install -r app/requirements.txt
    uvicorn app.api.main:app --host 0.0.0.0 --port 8000

The Android shell exercises the existing endpoints: `/health`, `/api/scope`, `/api/runtime`, `/api/answer`.

## Install APK

With a connected Android device and ADB available:

    adb devices
    adb install -r android-app/build/outputs/apk/debug/android-app-debug.apk

The current development environment does not include `adb` or a local Gradle installation, so local APK build/install cannot be claimed from this workspace. CI is configured to perform the reproducible Android build.

## What works

- MEDIA branding and Android launcher icon.
- Existing mobile-first MEDIA web UI.
- Ask MEDIA flow.
- Quick / Explain / Compare / Exam / Sources modes exposed by the current API.
- Evidence/source rendering from the real API response.
- Safe abstention rendering from the real API response.
- Runtime and knowledge-scope status.
- Loading and graceful request-error states.
- Arabic/English content support through the existing web UI.
- PWA shell caching remains available to the web layer.

## Offline behavior

This is **Online Alpha**. The Android shell can be installed, but answering requires a reachable MEDIA backend. No claim of offline AI inference is made.

## Smoke test

1. Start FastAPI on the configured host/port.
2. Build and install the debug APK.
3. Open MEDIA.
4. Confirm the home/question screen appears.
5. Ask: `What is the function of red blood cells?`
6. Confirm an answer and registered sources are rendered.
7. Ask: `Explain HemoLysis-X99`
8. Confirm safe abstention is rendered.
9. Stop the backend and retry.
10. Confirm the app shows a backend-unavailable state rather than freezing.