# LandlordSpaces Android app

Native Kotlin app (Jetpack Compose). It signs in with the same account as the website through `/api/mobile/v1`,
shows a home dashboard, and opens the marketplace, messages, account pages and Floor Plan Studio inside the app, already signed in.

## Get the installable file (no Android Studio needed)

1. Put the project on GitHub (the `android` folder and `.github/workflows/android.yml` must be in the repository).
2. Open the repository on GitHub, then **Actions > Android app > Run workflow**.
3. When it finishes (about 5 minutes), open the run and download **landlordspaces-android**. It contains:
   - `app-release.apk` : install this on a phone to test.
   - `app-release.aab` : this is what you upload to Google Play.

Without a signing key the file is signed with a test key, which is fine for testing but cannot be published to Play.

## Signing key for Google Play (do this once, keep the file safe)

```
keytool -genkeypair -v -keystore landlordspaces-upload.jks -alias landlordspaces -keyalg RSA -keysize 2048 -validity 10000
```

Then in GitHub: **Settings > Secrets and variables > Actions** and add:

| Secret | Value |
|---|---|
| `LS_KEYSTORE_B64` | the keystore file as base64 (PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("landlordspaces-upload.jks"))`) |
| `LS_KEYSTORE_PASSWORD` | the keystore password |
| `LS_KEY_ALIAS` | `landlordspaces` |
| `LS_KEY_PASSWORD` | the key password |

Never lose the keystore or its passwords, and never commit them.

## Releasing a new version

Edit `versionCode` (must go up every release) and `versionName` in `app/build.gradle.kts`, push, download the build.
Upload the APK to the website admin (Android releases) so members see the update prompt, and the AAB to Google Play.

## Not in this first version

Push notifications (needs a Firebase `google-services.json`), and Play Store listing material (#110).

Build trigger.
