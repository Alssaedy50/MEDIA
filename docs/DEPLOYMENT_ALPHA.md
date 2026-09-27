# MEDIA Alpha — Online Backend Deployment

MEDIA Android Alpha is currently an **online application**. The Android client requires a reachable MEDIA FastAPI backend until a trained local model is packaged for offline inference.

## Render

The repository includes `render.yaml` for a Render web service.

Deployment settings:

- Runtime: Python
- Build command: `pip install -r app/requirements.txt`
- Start command: `uvicorn app.api.main:app --host 0.0.0.0 --port $PORT`
- Health check: `/health`
- No API keys or medical secrets are required by the current Alpha backend.

After deployment, verify:

1. Open the service URL.
2. Confirm `/health` returns a healthy response.
3. Open the root web app.
4. Ask a supported hematology question.
5. Confirm the answer includes evidence/source information.
6. Test an unsupported/fabricated medical term and confirm safe abstention.

## Android

Build the Android Alpha against the deployed HTTPS backend:

```
cd android
gradle -PMEDIA_WEB_URL=https://YOUR-SERVICE.onrender.com/ :android-app:assembleDebug
```

The resulting APK is:

```
android-app/build/outputs/apk/debug/android-app-debug.apk
```

Do not place tokens, passwords, API keys, or private service credentials in the Android project.

## Operational boundary

The current deployment serves the retrieval/evidence-aware Alpha. The frozen ~100M model foundation is a contract for later large-corpus pretraining; it is not yet a downloadable trained model and must not be represented as active offline inference.

## Free-host behavior

A free hosted service may sleep or cold-start depending on provider policy. The Android app therefore needs explicit loading and backend-unavailable states rather than assuming the API is always warm.

## Next validation

Once the backend is deployed and the Android APK builds, install it on a real Android device and run the smoke test in `docs/ANDROID_ALPHA.md`. Collect student feedback before expanding the corpus or beginning the final 100M training run.
