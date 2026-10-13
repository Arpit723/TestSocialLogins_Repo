This is a Kotlin Multiplatform project targeting Android, iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

### Credentials & configuration

This project commits its Google/Firebase client configuration to git on purpose. These files contain
**public identifiers, not secrets** — every value in them ships inside the installed APK/IPA and can be
extracted by anyone, so keeping them out of the repo would provide no security. The real protection is
key restriction in Google Cloud Console (see below).

| File | What it contains | Where it is consumed |
| --- | --- | --- |
| [`androidApp/google-services.json`](./androidApp/google-services.json) | Firebase app ID, project ID, OAuth client IDs (Android + Web), Android API key | The `googleServices` Gradle plugin ([`androidApp/build.gradle.kts:6`](./androidApp/build.gradle.kts)) generates Android resources from it; `serverClientId()` reads `default_web_client_id` at [`AndroidAuthClient.kt:118`](./shared/src/androidMain/kotlin/com/bk/testsocialloginskmp/auth/AndroidAuthClient.kt) |
| [`iosApp/iosApp/GoogleService-Info.plist`](./iosApp/iosApp/GoogleService-Info.plist) | Client ID, reversed client ID, Android client ID, iOS API key, Google app ID, bundle ID | `FirebaseApp.configure()` loads it at launch; the sign-in bridge reads the client ID at [`GoogleSignInBridge.swift:12`](./iosApp/iosApp/GoogleSignInBridge.swift) |
| [`iosApp/iosApp/Info.plist`](./iosApp/iosApp/Info.plist) | Google URL scheme `com.googleusercontent.apps.…` (line 14), must match `REVERSED_CLIENT_ID` in `GoogleService-Info.plist` | iOS uses it to return to the app after the Google OAuth redirect |

**Why these are safe to commit**

- Firebase API keys are public identifiers by design (Firebase's official stance): they identify a
  project, they do not authorize access to its data.
- Abuse is prevented by Google Cloud Console key restrictions, not secrecy: the Android key is
  restricted to package name `com.bk.testsocialloginskmp` plus SHA-1 fingerprints of the signing
  keystores, and the iOS key to bundle ID `com.bk.testsocialloginskmp.TestSocialLoginsKMP`.
- OAuth client IDs are likewise public; the ID tokens they produce are only meaningful once verified
  server-side.

**Rule for real secrets**

Never place actual secrets (server API keys, service-account credentials, release signing material) in
client code or config files — client apps cannot keep secrets. Route anything sensitive through a
backend you control.

**Rotating a Firebase API key (if ever needed)**

1. Firebase Console → Project settings → delete the old key / generate a new one.
2. Re-download `google-services.json` and `GoogleService-Info.plist`.
3. Replace both files in the repo and commit. Old values remain in git history — acceptable for public
   identifiers, no history rewrite needed.

**Note on `debug.keystore`**

`androidApp/debug.keystore` (referenced with the standard debug passwords at
[`androidApp/build.gradle.kts:39-46`](./androidApp/build.gradle.kts)) is committed intentionally so every
machine produces APKs signed with the same SHA-1 that is registered in Firebase. This is standard
practice for debug builds and is not a secret.

**Starting a new Firebase project**

Reference templates with obviously fake placeholder values are provided:
[`androidApp/google-services.json.template`](./androidApp/google-services.json.template) and
[`iosApp/iosApp/GoogleService-Info.plist.template`](./iosApp/iosApp/GoogleService-Info.plist.template).
Copy each template over the real file path and replace the placeholders with the values from the files
downloaded from the Firebase Console.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…