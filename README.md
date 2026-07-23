# AndroidXmlBase

A production-ready, state-of-the-art Android template project utilizing **XML layouts**, **ViewBinding**, **MVVM**, and **Clean Architecture**. This project is optimized for speed, maintainability, and enterprise-grade scalability.

---

## 🚀 Key Features

* **Clean Architecture**: Strictly separated layers (Presentation, Domain, Data) with clean package structures.
* **Hilt Dependency Injection**: Automated DI module bindings set up at package levels for clean encapsulation.
* **Encrypted Room Database**: Secured local database powered by Room and SQLCipher, utilizing runtime Keystore-stored passphrases.
* **Secure Key-Value Store**: Built-in Android KeyStore-backed `EncryptedSecureStore` implementing safe cryptographic operations.
* **Per-App Locale & Language**: Built-in compatibility with Android 13's native per-app language settings, utilizing Google Jetpack compatibility.
* **Unified Theme System**: Automated Dark/Light/System theme toggles integrating Jetpack DataStore, featuring zero-flash startup loading.
* **OkHttp Token Authenticator**: Self-healing token refresh middleware to intercept expired sessions (401) and retry API calls seamlessly.
* **Type-Safe UI Delegates**: Non-deprecated, runtime-safe property delegates (`intentExtra`, `fragmentArg`) to load bundle extras warning-free.
* **Material 3 Design System**: Styled M3 themes, custom components (SegmentedButton, IconButton), and Facebook Shimmer placeholder loaders.
* **Strict Quality Gates**: Integrated static code analysis (Detekt), style formatters (KtLint), and code coverage tracking (Kover).

---

## 🛠️ Gradle Commands

Build and test commands configured in the template:

```bash
./gradlew assembleDebug        # Build debug APK
./gradlew assembleRelease      # Build release APK
./gradlew test                 # Run JVM Unit Tests
./gradlew check                # Full quality gate (unit tests, ktlint, detekt, Kover)
./gradlew :app:ktlintFormat    # Automatically format Kotlin styles
```

---

## 📂 Architecture Layout

```
app/src/main/java/com/example/androidxmlbase/
├── AndroidXmlBaseApplication.kt
├── MainActivity.kt             # App shell and top-level navigation
├── appshell/                   # Shell-owned destinations such as Home
├── core/                       # Shared modules
│   ├── architecture/           # Base result states & ViewModels
│   ├── di/                     # Dependency injection module bindings
│   ├── localization/           # Multi-language locale manager
│   ├── navigation/             # Activity and fragment transition navigators
│   ├── network/                # Api clients, file transfers, and Token Authenticators
│   ├── storage/                # Database (SQLCipher) and Datastore preferences
│   ├── time/                   # Monotonic clocks
│   └── ui/                     # Base classes, custom components, and type-safe delegates
├── feature/                    # Product capability scopes
│   └── settings/               # Canonical product feature
└── sample/                     # Reference implementations, not product features
    ├── demo/                   # Clean Architecture data/state sample
    └── designsystem/           # Reusable UI showcase
```
