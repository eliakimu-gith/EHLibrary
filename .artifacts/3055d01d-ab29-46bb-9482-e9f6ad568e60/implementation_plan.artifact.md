# Cloud Authentication and Real-Time Sync Implementation Plan

Implement secure user authentication (Login/Sign-up) and real-time cloud data synchronization (Books, Progress, Notes) between the Android Mobile App and the PC Web App, along with public cloud deployment for the web app.

## User Review Required

> [!IMPORTANT]
> **Cloud Backend Setup**: We will configure Firebase Authentication and Firestore Cloud Database so that your user account, books, reading progress (page numbers), and notes sync seamlessly between your Android phone and PC web browser in real-time.

## Open Questions
- None. We will use Firebase Auth & Firestore, which provides free-tier quotas sufficient for personal and multi-device reading sync.

## Proposed Changes

### Web Application (`app.py`)
- Add a secure **Login & Registration Screen** (Email / Password).
- Connect state management to Cloud Firestore so books, reading progress, and notes persist and sync in real-time under the logged-in user's account.

### Android Mobile App (`.kt`)
- Add a Login / Sign-up Activity or Screen in Jetpack Compose (`LoginScreen.kt`).
- Integrate Firebase Auth and Firestore repository to sync local Room database records with the cloud database.

### Cloud Deployment
- Configure `app.py` for deployment on Streamlit Community Cloud with a permanent public URL (e.g. `https://ehlibrary.streamlit.app`).

## Verification Plan

### Automated Tests
- Build and compile Android app via Gradle.
- Run Python Streamlit web app locally and verify login & cloud sync.

### Manual Verification
- Log in on the Android app, add a note / read a page, then log into the Web App on PC to verify instant real-time sync.
