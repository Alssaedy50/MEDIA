# MEDIA Android Alpha — Offline-first

MEDIA Android Alpha now treats local medical knowledge as the primary source. The core question-answering path does not require Internet access.

## Current offline architecture

Android app → local Hematology assets → deterministic local retrieval → evidence-aware answer rendering → safe abstention.

The APK bundles the reviewed Hematology records currently stored under `knowledge/hematology`. Android's `assets/` directory is packaged with the application and can be read through `AssetManager`, so the records are available without a network connection. cite_placeholder

## What the Alpha can do offline

- Load the bundled Hematology knowledge at application startup.
- Search by topic, concept, subtopic, subject, domain, terminology and content.
- Rank matching local records deterministically.
- Render definition, explanation, mechanism and clinical relevance when available.
- Show high-yield points and registered source titles.
- Abstain when local evidence is insufficient.
- Display the number of locally loaded medical records.
- Work with network disabled because the core path has no network dependency.

## Important limitation

This Alpha is **not yet the trained 100M neural model**. It is the offline knowledge + retrieval + evidence-aware answer layer that validates the student experience first.

The frozen model foundation remains a future local inference slot:

`Question → Local Retrieval → Local 100M Model → Verification → Answer`

The current app deliberately does not claim that the untrained foundation is active.

## Build

From the repository root:

    cd android
    gradle :android-app:assembleDebug

APK:

    android-app/build/outputs/apk/debug/android-app-debug.apk

## Test

    cd android
    gradle :android-app:testDebugUnitTest

CI builds the debug APK and runs Android unit tests.

## Offline smoke test

1. Install the APK.
2. Disable Wi-Fi and mobile data.
3. Launch MEDIA.
4. Confirm the green **OFFLINE** status and local record count.
5. Ask a supported question such as: `What is aplastic anemia?`
6. Confirm a local evidence-based answer and source titles.
7. Ask an unsupported/non-medical question.
8. Confirm safe abstention instead of an invented medical answer.

## Future layers

1. Expand blocks and subjects.
2. Add local structured retrieval/indexing as the corpus grows.
3. Train the MEDIA foundation on a much larger medical corpus.
4. Convert/package the trained model for Android local inference.
5. Add optional online synchronization/update/fallback without making Internet mandatory.

No API keys, tokens or private credentials belong in the Android project.