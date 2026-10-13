# Firebase Authentication + Google Sign-In (Android first) — TestSocialLoginsKMP

## Context

- Kotlin Multiplatform app (Compose Multiplatform UI in `shared`), targets Android + iOS (iosArm64, iosSimulatorArm64). Package: `com.bk.testsocialloginskmp`. Kotlin 2.4.20, AGP 9.1.1, minSdk 24.
- Firebase agent skills already installed at `TestSocialLoginsKMP/.agents/skills/` (`firebase-basics`, `firebase-auth-basics`) — consult them for Firebase tasks.
- Decisions made with user:
  1. **New Firebase project** created via CLI.
  2. **Android only** for Google Sign-In now; iOS is a follow-up.
  3. **GitLive KMP SDK** (`dev.gitlive:firebase-auth`, verified stable version **2.7.0** via `https://repo1.maven.org/maven2/dev/gitlive/firebase-auth/maven-metadata.xml`; do NOT use 3.0.0-alpha).
  4. GitLive lives in `shared/androidMain` only, behind a common `AuthClient` facade, so iOS targets and `./gradlew build` stay green. iOS gets a stub actual. (GitLive's iOS variant would require linking the Firebase iOS SDK via SPM — out of scope.)
- Key API facts (verified from GitLive master source): `Firebase.auth.signInWithCredential(GoogleAuthProvider.credential(idToken, null))`, `Firebase.auth.authStateChanged: Flow<FirebaseUser?>`, `Firebase.auth.signOut()`. GitLive has no built-in Google token acquisition — Android uses **Credential Manager** (`androidx.credentials` + `googleid`), which requires an Activity and the Web client ID.

## Phase 1 — Firebase project setup (all commands via `npx -y firebase-tools@latest`)

1. **Login**: run
   `npx -y firebase-tools@latest login "4/0AXlqoi5PdYKYKCVFroJsHyKZgymAhrhH9blrEsWdViJbE07hZve89fuN9qsZO6smifz58A"`
   (code provided by user 2026-10-13T10:13Z; single-use, short-lived). If expired: run `npx -y firebase-tools@latest login --no-localhost`, give the user the printed URL, ask for the new code, then `login "<code>"`. Verify with `login:list` (also note the account email for step 6).
2. **Create project**: `npx -y firebase-tools@latest projects:create test-social-logins-kmp --display-name "Test Social Logins KMP"`. If the ID is taken, retry with `test-social-logins-kmp-01` etc. Pass `--project <pid>` to every later command (avoids creating `.firebaserc`).
3. **Debug SHA-1**: `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android` → copy the first `SHA1:` value.
4. **Register Android app**:
   `npx -y firebase-tools@latest apps:create ANDROID "TestSocialLoginsKMP" --package-name com.bk.testsocialloginskmp --sha-1 <SHA1> --project <pid>` → note returned App ID.
5. **Download config**:
   `npx -y firebase-tools@latest apps:sdkconfig ANDROID <APP_ID> --project <pid> > androidApp/google-services.json`
6. **Enable Google provider**: create `firebase.json` at `TestSocialLoginsKMP/` root (verify none exists first) with:
   ```json
   {
     "auth": {
       "providers": {
         "googleSignIn": {
           "oAuthBrandDisplayName": "TestSocialLoginsKMP",
           "supportEmail": "<account email from login:list>"
         }
       }
     }
   }
   ```
   then `npx -y firebase-tools@latest deploy --only auth --project <pid>`. Afterwards re-run step 5 to refresh `google-services.json` and **confirm it contains `"client_type": 3`** (Web client → generates `default_web_client_id` resource, needed by Credential Manager). Fallback if brand creation fails: enable Google manually in Firebase Console → Authentication → Sign-in method → Google (set support email), then re-run step 5.

## Phase 2 — Gradle changes

- Pin latest versions at implementation time (never guess; fetch these):
  - google-services plugin: `https://dl.google.com/dl/android/maven2/com/google/gms/google-services/maven-metadata.xml`
  - androidx.credentials: `https://dl.google.com/dl/android/maven2/androidx/credentials/credentials/maven-metadata.xml`
  - googleid: `https://dl.google.com/dl/android/maven2/com/google/android/libraries/identity/googleid/googleid/maven-metadata.xml`
- `gradle/libs.versions.toml`: add libraries `gitlive-firebase-auth` (`dev.gitlive:firebase-auth:2.7.0`), `androidx-credentials`, `androidx-credentials-play-services-auth`, `googleid`; add plugin `googleServices` (`com.google.gms.google-services`).
- `androidApp/build.gradle.kts`: apply `alias(libs.plugins.googleServices)` (config `androidApp/google-services.json` from Phase 1).
- `shared/build.gradle.kts` → `androidMain.dependencies {}`: add the 4 libraries above. **Do not** add GitLive to `commonMain`.
- Risk: if latest google-services plugin is incompatible with AGP 9.1.1, fall back to manual init in `MainActivity` via `Firebase.initialize(context, FirebaseOptions(...))` using values from `google-services.json`.

## Phase 3 — Code (package `com.bk.testsocialloginskmp`)

New files under `shared/src`:

1. **commonMain** `auth/AuthClient.kt`:
   - `data class AppUser(uid: String, displayName: String?, email: String?, photoUrl: String?)`
   - `interface AuthClient { val isSupported: Boolean; val currentUser: StateFlow<AppUser?>; suspend fun signInWithGoogle(): AppUser?; suspend fun signOut() }`
   - `expect val authClient: AuthClient`
2. **commonMain** `auth/AuthViewModel.kt`: ViewModel exposing `uiState` (user, isLoading, errorMessage) + `signIn()` / `signOut()` using `viewModelScope`; catches exceptions into `errorMessage`.
3. **androidMain** `auth/AndroidAuthHost.kt`: `object AndroidAuthHost { lateinit var activity: ComponentActivity }` (set from MainActivity; documented test-app pattern).
4. **androidMain** `auth/AndroidAuthClient.kt`: `actual val authClient: AuthClient by lazy { AndroidAuthClient() }`
   - Sign-in: `CredentialManager.create(activity)`; `GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(false).setAutoSelectEnabled(false).setServerClientId(serverClientId)`; `CredentialRequest` with that option; `credentialManager.getCredential(activity, request)`; if credential is `CustomCredential` with type `GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL` → `GoogleIdTokenCredential.createFrom(credential.data).idToken`; then `dev.gitlive.firebase.auth.GoogleAuthProvider.credential(idToken, null)` → `Firebase.auth.signInWithCredential(cred)` → map `result.user` to `AppUser`.
   - `serverClientId`: `context.resources.getIdentifier("default_web_client_id", "string", context.packageName)` then `getString` (resource generated in androidApp by google-services plugin; resolved at runtime from merged resources).
   - `currentUser`: `Firebase.auth.authStateChanged.map { it?.toAppUser() }.stateIn(scope, SharingStarted.WhileSubscribed, null)`.
   - `signOut()`: `Firebase.auth.signOut()` + `credentialManager.clearCredentialState(ClearCredentialStateRequest(null))`.
5. **iosMain** `auth/IosAuthClient.kt`: `actual val authClient: AuthClient` with `isSupported = false`, `currentUser = MutableStateFlow(null)`, `signInWithGoogle()` throws `NotImplementedError("Google Sign-In on iOS is configured in a later phase")`, `signOut()` no-op.
6. **androidApp** `MainActivity.kt`: in `onCreate`, before `setContent`: `AndroidAuthHost.activity = this`.
7. **commonMain** `App.kt`: auth screen — signed-out: "Sign in with Google" button (disabled with "iOS support coming soon" label when `!isSupported`), loading indicator, error text; signed-in: photo (if any), displayName, email, uid + "Sign out" button; keep the existing Compose logo/greeting section below the signed-in card.

## Phase 4 — Verification

1. `./gradlew :androidApp:assembleDebug` succeeds.
2. `./gradlew build` succeeds — proves iOS targets still compile (the reason GitLive stays in androidMain).
3. Run on a Google-Play-enabled emulator: tap "Sign in with Google" → account picker → choose account → user info displayed; verify user appears in Firebase Console → Authentication → Users.
4. Sign out → sign in again shows full account picker (clearCredentialState works).
5. Known failure modes: `ApiException 10` (DEVELOPER_ERROR) = SHA-1 missing/mismatch; missing `default_web_client_id` = `google-services.json` lacks `client_type: 3` (redo Phase 1 step 6); `NoCredentialException` = no Google accounts on device (ensure emulator has one, we already set `filterByAuthorizedAccounts=false`); `ApiException 16` = user cancelled.

## Security notes

- `google-services.json` is safe to commit (identifiers, not secrets). Never log ID tokens. Leave `.firebaserc` uncommitted if created.

## Out of scope (follow-ups)

- iOS Google Sign-In: add `FirebaseCore`/`FirebaseAuth`/`GoogleSignIn` via SPM to `iosApp`, download `GoogleService-Info.plist`, add reversed URL scheme, implement the `iosMain` actual (Google Sign-In SDK → `idToken`), then optionally move GitLive to `commonMain` and collapse the facade.
- Release/Play signing SHA-1, additional providers, Firestore + security rules.
