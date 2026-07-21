# Android Base Modernization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Harden `AndroidXmlBase`'s `core/` foundation with modern, production-grade infrastructure (crypto/startup safety, splash correctness, logging, initializer ordering, background work, and cold-start profiling) before feature development begins on top of it.

**Architecture:** Single Gradle module (`app`) stays single-module except for one new build-tooling-only module (`:baselineprofile`, Task 7) that contains no business code. Each task is independently buildable, testable, and committable; later tasks build on earlier ones (Task 5 refactors the `Application` init flow that Tasks 1, 2, and 4 first land directly).

**Tech Stack:** Kotlin, Hilt, Coroutines/Flow, Room + SQLCipher, DataStore, AppCompat, androidx.core-splashscreen, Timber, androidx.startup, WorkManager + Hilt-Work, androidx.benchmark/baselineprofile.

## Global Constraints

- minSdk 24, targetSdk/compileSdk 37, Java/Kotlin JVM target 21 (see `app/build.gradle.kts`).
- Version catalog only — every new dependency goes in `gradle/libs.versions.toml`, never a raw coordinate string in a `build.gradle.kts`.
- No new Gradle modules containing business/feature code (per `CLAUDE.md`); the one exception used in this plan is `:baselineprofile`, a test-only Macrobenchmark module.
- Tests use JUnit4 + `org.junit.Assert.*` + hand-written fakes (no MockK in this repo) for JVM unit tests, matching `app/src/test/**` conventions already in place (e.g. `NetworkClientFactoryTest`, `ThemeManagerTest`).
- Kover enforces 80%+ line coverage on core/domain/data/viewmodel; Android glue (Activities, Fragments, `AndroidXmlBaseApplication`) is already excluded in `app/build.gradle.kts`'s `kover { reports { filters { excludes { ... } } } }` block — add new Android-glue-only classes to that same excludes list rather than fighting for coverage on them.
- Every new public `core/*` surface must be reflected in `docs/CORE_MODULES.md` in the same task that adds it (that doc's own rule: "if a class/file isn't listed here, it doesn't exist yet").
- Run `./gradlew :app:testDebugUnitTest` after each task; run `./gradlew check` after Tasks 5, 6, and 7 (broader blast radius).

---

## Task 1: Fix the DB passphrase `runBlocking` ANR risk

**Problem:** `DatabaseModule.provideAppDatabase()` currently calls `runBlocking { secureStore.getString(...) }` directly inside a Hilt `@Provides` method. Whatever thread first triggers this Hilt provider (often the main thread, via a ViewModel constructor) blocks synchronously on encrypted-prefs disk I/O.

**Files:**
- Create: `app/src/main/java/com/example/androidxmlbase/core/storage/database/DbPassphraseProvider.kt`
- Modify: `app/src/main/java/com/example/androidxmlbase/core/di/DatabaseModule.kt`
- Modify: `app/src/main/java/com/example/androidxmlbase/AndroidXmlBaseApplication.kt`
- Modify: `docs/CORE_MODULES.md`
- Test: `app/src/test/java/com/example/androidxmlbase/core/storage/database/DbPassphraseProviderTest.kt`

**Interfaces:**
- Consumes: `SecureStore` (`core/storage/secure/SecureStore.kt`, suspend `getString`/`putString`).
- Produces: `DbPassphraseProvider.getOrCreate(): String` (suspend, memoized) — consumed by Task 5's `DbPassphraseWarmupInitializer`.

- [ ] **Step 1: Write the failing test**

```kotlin
// app/src/test/java/com/example/androidxmlbase/core/storage/database/DbPassphraseProviderTest.kt
package com.example.androidxmlbase.core.storage.database

import com.example.androidxmlbase.core.storage.secure.SecureStore
import com.example.androidxmlbase.core.storage.secure.SecureStoreKey
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DbPassphraseProviderTest {
    @Test
    fun `generates and persists a new passphrase when none exists`() =
        runTest {
            val secureStore = FakeSecureStore()
            val provider = DbPassphraseProvider(secureStore)

            val passphrase = provider.getOrCreate()

            assertNotNull(passphrase)
            assertEquals(passphrase, secureStore.stored["db_passphrase"])
        }

    @Test
    fun `reuses the existing passphrase instead of generating a new one`() =
        runTest {
            val secureStore = FakeSecureStore().apply { stored["db_passphrase"] = "existing-key" }
            val provider = DbPassphraseProvider(secureStore)

            val passphrase = provider.getOrCreate()

            assertEquals("existing-key", passphrase)
        }

    @Test
    fun `caches the passphrase so a second call does not read the store again`() =
        runTest {
            val secureStore = FakeSecureStore()
            val provider = DbPassphraseProvider(secureStore)

            val first = provider.getOrCreate()
            secureStore.stored.clear()
            val second = provider.getOrCreate()

            assertEquals(first, second)
        }

    private class FakeSecureStore : SecureStore {
        val stored = mutableMapOf<String, String>()

        override suspend fun getString(key: SecureStoreKey): String? = stored[key.name]

        override suspend fun putString(
            key: SecureStoreKey,
            value: String,
        ) {
            stored[key.name] = value
        }

        override suspend fun remove(key: SecureStoreKey) {
            stored.remove(key.name)
        }

        override suspend fun clear() {
            stored.clear()
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.androidxmlbase.core.storage.database.DbPassphraseProviderTest"`
Expected: FAIL to compile — `DbPassphraseProvider` is unresolved.

- [ ] **Step 3: Create `DbPassphraseProvider`**

```kotlin
// app/src/main/java/com/example/androidxmlbase/core/storage/database/DbPassphraseProvider.kt
package com.example.androidxmlbase.core.storage.database

import com.example.androidxmlbase.core.storage.secure.SecureStore
import com.example.androidxmlbase.core.storage.secure.SecureStoreKey
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves (and memoizes) the SQLCipher passphrase. `DbPassphraseWarmupInitializer` warms this
 * on a background dispatcher during process startup so `DatabaseModule`'s Hilt `@Provides`
 * boundary — which must stay synchronous — almost always hits the cached value instead of
 * blocking on encrypted-prefs disk I/O.
 */
@Singleton
class DbPassphraseProvider
    @Inject
    constructor(
        private val secureStore: SecureStore,
    ) {
        private val mutex = Mutex()

        @Volatile
        private var cached: String? = null

        suspend fun getOrCreate(): String {
            cached?.let { return it }
            return mutex.withLock {
                cached?.let { return@withLock it }
                val existing = secureStore.getString(DB_PASSPHRASE_KEY)
                val passphrase =
                    existing?.takeIf { it.isNotEmpty() }
                        ?: UUID.randomUUID().toString().also { secureStore.putString(DB_PASSPHRASE_KEY, it) }
                cached = passphrase
                passphrase
            }
        }

        companion object {
            private val DB_PASSPHRASE_KEY = SecureStoreKey("db_passphrase")
        }
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.androidxmlbase.core.storage.database.DbPassphraseProviderTest"`
Expected: PASS (3 tests green).

- [ ] **Step 5: Update `DatabaseModule` to consume the provider**

Replace the full contents of `app/src/main/java/com/example/androidxmlbase/core/di/DatabaseModule.kt`:

```kotlin
package com.example.androidxmlbase.core.di

import android.content.Context
import androidx.room.Room
import com.example.androidxmlbase.core.storage.database.AppDatabase
import com.example.androidxmlbase.core.storage.database.DbPassphraseProvider
import com.example.androidxmlbase.core.storage.database.LocalSettingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        passphraseProvider: DbPassphraseProvider,
    ): AppDatabase {
        System.loadLibrary("sqlcipher")
        // DbPassphraseProvider is warmed from process startup (see DbPassphraseWarmupInitializer)
        // on Dispatchers.IO, so this almost always resolves an already-cached value instead of
        // performing disk I/O on whichever thread first triggers this Hilt provider.
        val passphrase = runBlocking(Dispatchers.IO) { passphraseProvider.getOrCreate() }
        val factory = SupportOpenHelperFactory(passphrase.toByteArray())

        return Room
            .databaseBuilder(
                context,
                AppDatabase::class.java,
                "app_database.db",
            ).openHelperFactory(factory)
            .fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideLocalSettingDao(database: AppDatabase): LocalSettingDao = database.localSettingDao()
}
```

- [ ] **Step 6: Warm the passphrase from `Application.onCreate()`**

In `app/src/main/java/com/example/androidxmlbase/AndroidXmlBaseApplication.kt`, add the injected provider and the warm-up launch (Task 5 later moves this into an Initializer — this step is the minimal, immediately-correct fix):

```kotlin
package com.example.androidxmlbase

import android.app.Application
import com.example.androidxmlbase.core.storage.database.DbPassphraseProvider
import com.example.androidxmlbase.core.ui.theme.ThemeManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class AndroidXmlBaseApplication : Application() {
    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var dbPassphraseProvider: DbPassphraseProvider

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        // Warm the encrypted DB passphrase on a background dispatcher (see DbPassphraseProvider).
        applicationScope.launch(Dispatchers.IO) { dbPassphraseProvider.getOrCreate() }

        // Synchronously load and apply the theme on startup to prevent launch flashing
        runBlocking {
            val initialTheme = themeManager.getTheme()
            themeManager.applyTheme(initialTheme)
        }

        // Observe dynamic user theme configuration updates at runtime
        themeManager.currentTheme
            .onEach { themeManager.applyTheme(it) }
            .launchIn(applicationScope)
    }
}
```

- [ ] **Step 7: Document the new core surface**

In `docs/CORE_MODULES.md`, under the existing `### core/storage/secure` section, add a new subsection right after it:

```markdown
### `core/storage/database`
- `DbPassphraseProvider` — memoized SQLCipher passphrase resolver (`suspend fun getOrCreate(): String`), backed by `SecureStore`. Warmed on `Dispatchers.IO` during process startup so `DatabaseModule`'s Hilt `@Provides` boundary doesn't block on disk I/O.
```

- [ ] **Step 8: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS, no regressions.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/example/androidxmlbase/core/storage/database/DbPassphraseProvider.kt \
        app/src/main/java/com/example/androidxmlbase/core/di/DatabaseModule.kt \
        app/src/main/java/com/example/androidxmlbase/AndroidXmlBaseApplication.kt \
        app/src/test/java/com/example/androidxmlbase/core/storage/database/DbPassphraseProviderTest.kt \
        docs/CORE_MODULES.md
git commit -m "fix: warm and memoize DB passphrase to avoid main-thread runBlocking I/O"
```

---

## Task 2: Fix the theme-init `runBlocking` and expose `isThemeApplied`

**Problem:** `AndroidXmlBaseApplication.onCreate()` calls `runBlocking { themeManager.getTheme(); themeManager.applyTheme(...) }` to avoid a theme flash before the first Activity draws — but this blocks the main thread on a DataStore read. The existing reactive collector (`currentTheme.onEach { applyTheme(it) }.launchIn(applicationScope)`) already applies the theme asynchronously; the `runBlocking` call is a redundant, blocking attempt to force that first emission synchronously. Task 3 replaces it with the Splashscreen API's `setKeepOnScreenCondition`, which needs a signal for "has the theme been applied yet" — `isThemeApplied`.

**Files:**
- Modify: `app/src/main/java/com/example/androidxmlbase/core/ui/theme/ThemeManager.kt`
- Modify: `app/src/main/java/com/example/androidxmlbase/AndroidXmlBaseApplication.kt`
- Modify: `docs/CORE_MODULES.md`
- Test: `app/src/test/java/com/example/androidxmlbase/core/ui/theme/ThemeManagerTest.kt`

**Interfaces:**
- Produces: `ThemeManager.isThemeApplied: StateFlow<Boolean>` — consumed by Task 3's `MainActivity.installSplashScreen()` keep-on-screen condition.

- [ ] **Step 1: Extend the existing test file with a failing test**

Add this test inside the existing `ThemeManagerTest` class in `app/src/test/java/com/example/androidxmlbase/core/ui/theme/ThemeManagerTest.kt` (keep the existing `FakeSettingsStore` and the 3 existing tests as-is):

```kotlin
    @Test
    fun `isThemeApplied stays false until applyTheme runs`() =
        runTest {
            val settingsStore = FakeSettingsStore()
            val themeManager = AndroidThemeManager(settingsStore)

            assertEquals(false, themeManager.isThemeApplied.value)

            themeManager.applyTheme(AppTheme.DARK)

            assertEquals(true, themeManager.isThemeApplied.value)
        }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.androidxmlbase.core.ui.theme.ThemeManagerTest"`
Expected: FAIL to compile — `isThemeApplied` is unresolved.

- [ ] **Step 3: Add `isThemeApplied` to `ThemeManager`**

Replace the full contents of `app/src/main/java/com/example/androidxmlbase/core/ui/theme/ThemeManager.kt`:

```kotlin
package com.example.androidxmlbase.core.ui.theme

import androidx.appcompat.app.AppCompatDelegate
import com.example.androidxmlbase.core.storage.settings.AppSettingsKeys
import com.example.androidxmlbase.core.storage.settings.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface ThemeManager {
    val currentTheme: Flow<AppTheme>

    /** True once the persisted theme has been read and applied at least once this process. */
    val isThemeApplied: StateFlow<Boolean>

    suspend fun getTheme(): AppTheme

    suspend fun setTheme(theme: AppTheme)

    fun applyTheme(theme: AppTheme)
}

@Singleton
class AndroidThemeManager
    @Inject
    constructor(
        private val settingsStore: SettingsStore,
    ) : ThemeManager {
        private val themeAppliedState = MutableStateFlow(false)
        override val isThemeApplied: StateFlow<Boolean> = themeAppliedState.asStateFlow()

        override val currentTheme: Flow<AppTheme> =
            settingsStore
                .observe(AppSettingsKeys.THEME_MODE)
                .map { AppTheme.fromKey(it) }

        override suspend fun getTheme(): AppTheme {
            val key = settingsStore.get(AppSettingsKeys.THEME_MODE)
            return AppTheme.fromKey(key)
        }

        override suspend fun setTheme(theme: AppTheme) {
            settingsStore.set(AppSettingsKeys.THEME_MODE, theme.key)
            applyTheme(theme)
        }

        override fun applyTheme(theme: AppTheme) {
            val nightMode =
                when (theme) {
                    AppTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                    AppTheme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                    AppTheme.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            AppCompatDelegate.setDefaultNightMode(nightMode)
            themeAppliedState.value = true
        }
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.androidxmlbase.core.ui.theme.ThemeManagerTest"`
Expected: PASS (4 tests green).

- [ ] **Step 5: Remove the blocking theme init from `Application.onCreate()`**

In `app/src/main/java/com/example/androidxmlbase/AndroidXmlBaseApplication.kt`, remove the `runBlocking { ... }` block entirely (the reactive collector below it already applies the theme as soon as it loads; Task 3 keeps the splash screen up until `isThemeApplied` flips to `true`, so there is no visible flash). Replace the full file contents:

```kotlin
package com.example.androidxmlbase

import android.app.Application
import com.example.androidxmlbase.core.storage.database.DbPassphraseProvider
import com.example.androidxmlbase.core.ui.theme.ThemeManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AndroidXmlBaseApplication : Application() {
    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var dbPassphraseProvider: DbPassphraseProvider

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        applicationScope.launch(Dispatchers.IO) { dbPassphraseProvider.getOrCreate() }

        // Applies as soon as the persisted theme loads. MainActivity's splash screen (Task 3)
        // stays up until ThemeManager.isThemeApplied is true, so there is no main-thread block
        // and no visible flash once the splash dismisses.
        themeManager.currentTheme
            .onEach { themeManager.applyTheme(it) }
            .launchIn(applicationScope)
    }
}
```

- [ ] **Step 6: Update the existing `core/ui/theme` section in `docs/CORE_MODULES.md`**

This section already exists (lines 126-136). Replace exactly two lines in it, leave the rest untouched.

Replace:
```markdown
- `ThemeManager` (interface) — `currentTheme: Flow<AppTheme>`, `suspend fun getTheme(): AppTheme`, `suspend fun setTheme(theme: AppTheme)`, `fun applyTheme(theme: AppTheme)`.
```
with:
```markdown
- `ThemeManager` (interface) — `currentTheme: Flow<AppTheme>`, `isThemeApplied: StateFlow<Boolean>` (true once the persisted theme has been applied at least once this process), `suspend fun getTheme(): AppTheme`, `suspend fun setTheme(theme: AppTheme)`, `fun applyTheme(theme: AppTheme)`.
```

Replace:
```markdown
**Consumers:** any screen that lets the user switch theme; `applyTheme` is also called on app start to restore the persisted choice.
```
with:
```markdown
**Consumers:** any screen that lets the user switch theme; `applyTheme` is also called on app start to restore the persisted choice; `MainActivity` reads `isThemeApplied` for its splash screen keep-on-screen condition (Task 3).
```

- [ ] **Step 7: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS, no regressions.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/example/androidxmlbase/core/ui/theme/ThemeManager.kt \
        app/src/main/java/com/example/androidxmlbase/AndroidXmlBaseApplication.kt \
        app/src/test/java/com/example/androidxmlbase/core/ui/theme/ThemeManagerTest.kt \
        docs/CORE_MODULES.md
git commit -m "fix: replace blocking theme runBlocking with reactive apply + isThemeApplied signal"
```

---

## Task 3: Jetpack Splashscreen

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/res/values/themes.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/example/androidxmlbase/MainActivity.kt`
- Modify: `docs/CORE_MODULES.md`

**Interfaces:**
- Consumes: `ThemeManager.isThemeApplied` (Task 2).
- Produces: nothing new consumed by later tasks.

- [ ] **Step 1: Add the dependency to the version catalog**

In `gradle/libs.versions.toml`, add to `[versions]` (alphabetical position not required, but keep near other `androidx*` entries):

```toml
coreSplashscreen = "1.2.0"
```

Add to `[libraries]`:

```toml
androidx-core-splashscreen = { group = "androidx.core", name = "core-splashscreen", version.ref = "coreSplashscreen" }
```

- [ ] **Step 2: Add the dependency to `app/build.gradle.kts`**

In the `dependencies { ... }` block, add right after `implementation(libs.androidx.appcompat)`:

```kotlin
    implementation(libs.androidx.core.splashscreen)
```

- [ ] **Step 3: Add the splash theme**

In `app/src/main/res/values/themes.xml`, add a new `<style>` right after the existing `Theme.AndroidXmlBase` style (after its closing `</style>` tag, before the `Widget.AndroidXmlBase.Button` style):

```xml
    <style name="Theme.AndroidXmlBase.Splash" parent="Theme.SplashScreen">
        <item name="windowSplashScreenBackground">@color/color_surface</item>
        <item name="windowSplashScreenAnimatedIcon">@drawable/ic_launcher_foreground</item>
        <item name="postSplashScreenTheme">@style/Theme.AndroidXmlBase</item>
    </style>
```

- [ ] **Step 4: Point MainActivity's manifest theme at the splash theme**

In `app/src/main/AndroidManifest.xml`, change the `MainActivity` entry's theme:

```xml
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.AndroidXmlBase.Splash"
            android:windowSoftInputMode="adjustResize">
```

(Leave `DemoActivity` and `DesignSystemActivity` on `@style/Theme.AndroidXmlBase` — only the launcher Activity needs the splash theme.)

- [ ] **Step 5: Install the splash screen in `MainActivity`**

Replace the full contents of `app/src/main/java/com/example/androidxmlbase/MainActivity.kt`:

```kotlin
package com.example.androidxmlbase

import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.splashscreen.installSplashScreen
import com.example.androidxmlbase.core.localization.LocaleManager
import com.example.androidxmlbase.core.navigation.ActivityDestination
import com.example.androidxmlbase.core.navigation.ActivityNavigator
import com.example.androidxmlbase.core.ui.base.BaseActivity
import com.example.androidxmlbase.core.ui.theme.ThemeManager
import com.example.androidxmlbase.databinding.ActivityMainBinding
import com.example.androidxmlbase.feature.demo.presentation.ui.DemoActivity
import com.example.androidxmlbase.feature.designsystem.presentation.ui.DesignSystemActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {
    @Inject
    lateinit var localeManager: LocaleManager

    @Inject
    lateinit var activityNavigator: ActivityNavigator

    @Inject
    lateinit var themeManager: ThemeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { !themeManager.isThemeApplied.value }
    }

    override fun inflateBinding(inflater: LayoutInflater): ActivityMainBinding = ActivityMainBinding.inflate(inflater)

    override fun onBindingReady(savedInstanceState: Bundle?) {
        binding.btnOpenDemo.setOnClickListener {
            activityNavigator.navigate(this, ActivityDestination(DemoActivity::class))
        }

        binding.btnLangEn.setOnClickListener {
            localeManager.setLanguage("en")
        }
        binding.btnLangVi.setOnClickListener {
            localeManager.setLanguage("vi")
        }
        binding.btnDesignSystem.setOnClickListener {
            activityNavigator.navigate(this, ActivityDestination(DesignSystemActivity::class))
        }
    }
}
```

- [ ] **Step 6: Document in `docs/CORE_MODULES.md`**

Append a sentence to the `core/ui/theme` section's **Consumers** line added in Task 2 (already mentions `MainActivity`'s splash screen) — no further doc change needed since Task 2 already documents the consumer relationship.

- [ ] **Step 7: Manual verification (no automated test — this is Android UI/manifest wiring, already excluded from Kover like other Activities)**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

Then install and launch on a device/emulator running API 24+ and API 31+ (splash rendering differs pre/post Android 12):
```bash
./gradlew :app:installDebug
adb shell am start -n com.example.androidxmlbase/.MainActivity
```
Expected: brief system splash (icon + `color_surface` background) then MainActivity appears already in the correct light/dark theme — no flash or recreate visible. Toggle the device's dark mode (with theme set to `SYSTEM`) and relaunch to confirm both branches.

- [ ] **Step 8: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/res/values/themes.xml \
        app/src/main/AndroidManifest.xml app/src/main/java/com/example/androidxmlbase/MainActivity.kt
git commit -m "feat: add Jetpack Splashscreen synced to theme readiness"
```

---

## Task 4: Timber logging

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/example/androidxmlbase/core/logging/ReleaseTree.kt`
- Modify: `app/src/main/java/com/example/androidxmlbase/AndroidXmlBaseApplication.kt`
- Modify: `docs/CORE_MODULES.md`
- Test: `app/src/test/java/com/example/androidxmlbase/core/logging/ReleaseTreeTest.kt`

**Interfaces:**
- Produces: `ReleaseTree` (a `timber.log.Timber.Tree`) — planted by Task 5's `TimberInitializer` instead of directly in `Application.onCreate()` once that task lands.

- [ ] **Step 1: Add the dependency to the version catalog**

In `gradle/libs.versions.toml`, add to `[versions]`:

```toml
timber = "5.0.1"
```

Add to `[libraries]`:

```toml
timber = { group = "com.jakewharton.timber", name = "timber", version.ref = "timber" }
```

- [ ] **Step 2: Add the dependency to `app/build.gradle.kts`**

Add right after `implementation(libs.shimmer)`:

```kotlin
    implementation(libs.timber)
```

- [ ] **Step 3: Write the failing test**

```kotlin
// app/src/test/java/com/example/androidxmlbase/core/logging/ReleaseTreeTest.kt
package com.example.androidxmlbase.core.logging

import android.util.Log
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseTreeTest {
    private val tree = ReleaseTree()

    @Test
    fun `treats warnings and errors as loggable`() {
        assertTrue(tree.isLoggable(tag = "Test", priority = Log.WARN))
        assertTrue(tree.isLoggable(tag = "Test", priority = Log.ERROR))
    }

    @Test
    fun `filters out verbose debug and info priorities`() {
        assertFalse(tree.isLoggable(tag = "Test", priority = Log.VERBOSE))
        assertFalse(tree.isLoggable(tag = "Test", priority = Log.DEBUG))
        assertFalse(tree.isLoggable(tag = "Test", priority = Log.INFO))
    }
}
```

- [ ] **Step 4: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.androidxmlbase.core.logging.ReleaseTreeTest"`
Expected: FAIL to compile — `ReleaseTree` is unresolved.

- [ ] **Step 5: Create `ReleaseTree`**

```kotlin
// app/src/main/java/com/example/androidxmlbase/core/logging/ReleaseTree.kt
package com.example.androidxmlbase.core.logging

import android.util.Log
import timber.log.Timber

/**
 * Release-build Timber tree: drops VERBOSE/DEBUG/INFO entirely and forwards only WARN+ to
 * `android.util.Log`. Hook a crash-reporting SDK's `log()`/`recordException()` into [log] once
 * this base picks one — kept as plain `Log.println` until then so no reporting vendor is
 * hardcoded into the base.
 */
class ReleaseTree : Timber.Tree() {
    override fun isLoggable(
        tag: String?,
        priority: Int,
    ): Boolean = priority >= Log.WARN

    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        if (isLoggable(tag, priority)) {
            Log.println(priority, tag, message)
        }
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.androidxmlbase.core.logging.ReleaseTreeTest"`
Expected: PASS (2 tests green).

- [ ] **Step 7: Plant the tree in `Application.onCreate()`**

In `app/src/main/java/com/example/androidxmlbase/AndroidXmlBaseApplication.kt`, plant Timber first thing in `onCreate()` (Task 5 later moves this into `TimberInitializer`):

```kotlin
package com.example.androidxmlbase

import android.app.Application
import com.example.androidxmlbase.core.logging.ReleaseTree
import com.example.androidxmlbase.core.storage.database.DbPassphraseProvider
import com.example.androidxmlbase.core.ui.theme.ThemeManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class AndroidXmlBaseApplication : Application() {
    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var dbPassphraseProvider: DbPassphraseProvider

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else ReleaseTree())

        applicationScope.launch(Dispatchers.IO) { dbPassphraseProvider.getOrCreate() }

        themeManager.currentTheme
            .onEach { themeManager.applyTheme(it) }
            .launchIn(applicationScope)
    }
}
```

- [ ] **Step 8: Document in `docs/CORE_MODULES.md`**

Add a new section after `core/localization`:

```markdown
## `core/logging`

- `ReleaseTree` (extends `timber.log.Timber.Tree`) — filters to WARN+ only, forwards to `android.util.Log`. Planted instead of `Timber.DebugTree()` in release builds.

**Consumers:** `AndroidXmlBaseApplication` plants `Timber.DebugTree()` in debug builds and `ReleaseTree` in release builds. Feature code should call `Timber.tag(...).d/i/w/e(...)` instead of `android.util.Log` directly.
```

- [ ] **Step 9: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS, no regressions.

- [ ] **Step 10: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts \
        app/src/main/java/com/example/androidxmlbase/core/logging/ReleaseTree.kt \
        app/src/main/java/com/example/androidxmlbase/AndroidXmlBaseApplication.kt \
        app/src/test/java/com/example/androidxmlbase/core/logging/ReleaseTreeTest.kt \
        docs/CORE_MODULES.md
git commit -m "feat: add Timber logging with a WARN+-only release tree"
```

---

## Task 5: Jetpack App Startup (formalize initialization)

**Rationale:** `Application.onCreate()` has picked up three unrelated concerns across Tasks 1, 2, and 4 (DB passphrase warm-up, theme apply, Timber planting). `androidx.startup` gives each concern its own ordered, declarative `Initializer` instead of letting `onCreate()` grow into a dumping ground as more concerns land later (WorkManager's config in Task 6, and anything a future feature adds). This task also promotes the ad-hoc `applicationScope` field into a proper Hilt-provided `@ApplicationScope`-qualified `CoroutineScope`, since `Initializer`s only receive a `Context` and need to reach app-scoped dependencies through a Hilt `EntryPoint`.

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/example/androidxmlbase/core/di/CoroutineScopeModule.kt`
- Create: `app/src/main/java/com/example/androidxmlbase/core/startup/AppStartupEntryPoint.kt`
- Create: `app/src/main/java/com/example/androidxmlbase/core/startup/TimberInitializer.kt`
- Create: `app/src/main/java/com/example/androidxmlbase/core/startup/DbPassphraseWarmupInitializer.kt`
- Create: `app/src/main/java/com/example/androidxmlbase/core/startup/ThemeApplyInitializer.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/example/androidxmlbase/AndroidXmlBaseApplication.kt`
- Modify: `docs/CORE_MODULES.md`
- Test: `app/src/test/java/com/example/androidxmlbase/core/logging/ReleaseTreeTest.kt` (unchanged, still passes), no new unit tests (Initializers are pure Android glue — verified via Step 8's manual run, same posture as Activities)

**Interfaces:**
- Consumes: `DbPassphraseProvider` (Task 1), `ThemeManager` (Task 2), `ReleaseTree` (Task 4).
- Produces: `@ApplicationScope CoroutineScope` — consumed by Task 6's WorkManager wiring if a future feature needs to launch fire-and-forget work at the app scope.

- [ ] **Step 1: Add the dependency to the version catalog**

In `gradle/libs.versions.toml`, add to `[versions]`:

```toml
startupRuntime = "1.2.0"
```

Add to `[libraries]`:

```toml
androidx-startup-runtime = { group = "androidx.startup", name = "startup-runtime", version.ref = "startupRuntime" }
```

- [ ] **Step 2: Add the dependency to `app/build.gradle.kts`**

Add right after `implementation(libs.androidx.core.splashscreen)`:

```kotlin
    implementation(libs.androidx.startup.runtime)
```

- [ ] **Step 3: Promote `applicationScope` to a Hilt-provided qualifier**

```kotlin
// app/src/main/java/com/example/androidxmlbase/core/di/CoroutineScopeModule.kt
package com.example.androidxmlbase.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopeModule {
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
```

- [ ] **Step 4: Add the Hilt `EntryPoint` the Initializers use**

```kotlin
// app/src/main/java/com/example/androidxmlbase/core/startup/AppStartupEntryPoint.kt
package com.example.androidxmlbase.core.startup

import com.example.androidxmlbase.core.di.ApplicationScope
import com.example.androidxmlbase.core.storage.database.DbPassphraseProvider
import com.example.androidxmlbase.core.ui.theme.ThemeManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope

/**
 * androidx.startup `Initializer`s are instantiated by reflection (no-arg constructor) from a
 * `ContentProvider` that runs before `Application.onCreate()`, so they can't use constructor
 * injection. This `EntryPoint` is how they reach Hilt-provided singletons instead. Safe to call
 * this early: Hilt's component is built lazily on first access, not on `Application.onCreate()`.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppStartupEntryPoint {
    fun dbPassphraseProvider(): DbPassphraseProvider

    fun themeManager(): ThemeManager

    @ApplicationScope
    fun applicationScope(): CoroutineScope
}
```

- [ ] **Step 5: Create the three Initializers**

```kotlin
// app/src/main/java/com/example/androidxmlbase/core/startup/TimberInitializer.kt
package com.example.androidxmlbase.core.startup

import android.content.Context
import androidx.startup.Initializer
import com.example.androidxmlbase.BuildConfig
import com.example.androidxmlbase.core.logging.ReleaseTree
import timber.log.Timber

class TimberInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else ReleaseTree())
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
```

```kotlin
// app/src/main/java/com/example/androidxmlbase/core/startup/DbPassphraseWarmupInitializer.kt
package com.example.androidxmlbase.core.startup

import android.content.Context
import dagger.hilt.android.EntryPointAccessors
import androidx.startup.Initializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DbPassphraseWarmupInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        val entryPoint =
            EntryPointAccessors.fromApplication(context.applicationContext, AppStartupEntryPoint::class.java)
        entryPoint.applicationScope().launch(Dispatchers.IO) {
            entryPoint.dbPassphraseProvider().getOrCreate()
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(TimberInitializer::class.java)
}
```

```kotlin
// app/src/main/java/com/example/androidxmlbase/core/startup/ThemeApplyInitializer.kt
package com.example.androidxmlbase.core.startup

import android.content.Context
import androidx.startup.Initializer
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ThemeApplyInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        val entryPoint =
            EntryPointAccessors.fromApplication(context.applicationContext, AppStartupEntryPoint::class.java)
        val themeManager = entryPoint.themeManager()
        themeManager.currentTheme
            .onEach { themeManager.applyTheme(it) }
            .launchIn(entryPoint.applicationScope())
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(TimberInitializer::class.java)
}
```

- [ ] **Step 6: Register the Initializers and disable default component discovery noise in the manifest**

In `app/src/main/AndroidManifest.xml`, add this `<provider>` block right after the `</activity>` for `DesignSystemActivity` (before the existing `AppLocalesMetadataHolderService` `<service>`):

```xml
        <provider
            android:name="androidx.startup.InitializationProvider"
            android:authorities="${applicationId}.androidx-startup"
            android:exported="false"
            tools:node="merge">
            <meta-data
                android:name="com.example.androidxmlbase.core.startup.TimberInitializer"
                android:value="androidx.startup" />
            <meta-data
                android:name="com.example.androidxmlbase.core.startup.DbPassphraseWarmupInitializer"
                android:value="androidx.startup" />
            <meta-data
                android:name="com.example.androidxmlbase.core.startup.ThemeApplyInitializer"
                android:value="androidx.startup" />
        </provider>
```

- [ ] **Step 7: Shrink `AndroidXmlBaseApplication` down to just the Hilt anchor**

Replace the full contents of `app/src/main/java/com/example/androidxmlbase/AndroidXmlBaseApplication.kt`:

```kotlin
package com.example.androidxmlbase

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Startup work (logging, DB passphrase warm-up, theme application) runs via the
 * `androidx.startup` `Initializer`s in `core/startup/`, registered in AndroidManifest.xml —
 * not here. See `docs/CORE_MODULES.md` → `core/startup`.
 */
@HiltAndroidApp
class AndroidXmlBaseApplication : Application()
```

- [ ] **Step 8: Manual verification**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

```bash
./gradlew :app:installDebug
adb shell am start -n com.example.androidxmlbase/.MainActivity
adb logcat | grep -i "androidxmlbase"
```
Expected: app launches with correct theme and no flash (same as Task 3's check); no crash from `EntryPointAccessors` (would show as a `RuntimeException` in logcat if the Hilt component wasn't reachable that early — it will be reachable, per Step 4's doc comment).

- [ ] **Step 9: Run the full unit test suite and `check`**

Run: `./gradlew check`
Expected: PASS. (Broader check since this task touches the manifest and DI graph.)

- [ ] **Step 10: Document in `docs/CORE_MODULES.md`**

Add a new section after `core/logging`:

```markdown
## `core/startup`

Formalizes process-startup work via `androidx.startup.Initializer` instead of `Application.onCreate()`.

- `AppStartupEntryPoint` (Hilt `@EntryPoint`) — how Initializers (instantiated by reflection, no constructor injection available) reach `DbPassphraseProvider`, `ThemeManager`, and the `@ApplicationScope CoroutineScope`.
- `TimberInitializer` — plants `Timber.DebugTree()` (debug) or `ReleaseTree()` (release).
- `DbPassphraseWarmupInitializer` — warms `DbPassphraseProvider` on `Dispatchers.IO`. Depends on `TimberInitializer`.
- `ThemeApplyInitializer` — collects `ThemeManager.currentTheme` and applies it reactively. Depends on `TimberInitializer`.

All three are registered as `<meta-data>` entries under `androidx.startup.InitializationProvider` in `AndroidManifest.xml`.

**Consumers:** `AndroidXmlBaseApplication` no longer does any of this directly — see its class doc comment.
```

Also update `core/di`'s section to mention `CoroutineScopeModule`:

```markdown
- `CoroutineScopeModule` — provides the `@ApplicationScope`-qualified, `SupervisorJob() + Dispatchers.Default` `CoroutineScope` used for app-wide fire-and-forget work (startup Initializers, and any future feature's background triggers).
```

- [ ] **Step 11: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts \
        app/src/main/java/com/example/androidxmlbase/core/di/CoroutineScopeModule.kt \
        app/src/main/java/com/example/androidxmlbase/core/startup/ \
        app/src/main/AndroidManifest.xml \
        app/src/main/java/com/example/androidxmlbase/AndroidXmlBaseApplication.kt \
        docs/CORE_MODULES.md
git commit -m "refactor: formalize app startup via androidx.startup Initializers"
```

---

## Task 6: WorkManager + Hilt Worker

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/example/androidxmlbase/AndroidXmlBaseApplication.kt`
- Create: `app/src/main/java/com/example/androidxmlbase/core/work/SampleHeartbeatWorker.kt`
- Modify: `app/build.gradle.kts` Kover excludes list
- Modify: `docs/CORE_MODULES.md`
- Test: `app/src/androidTest/java/com/example/androidxmlbase/core/work/SampleHeartbeatWorkerTest.kt`

**Interfaces:**
- Consumes: `@ApplicationScope CoroutineScope` pattern is not needed here (`HiltWorkerFactory` is provided automatically by `androidx.hilt:hilt-work`'s own Dagger module once `@HiltAndroidApp` is present).
- Produces: `SampleHeartbeatWorker` — a copy-this-shape reference `@HiltWorker` `CoroutineWorker`. Not scheduled anywhere by default; it exists to prove the Hilt+WorkManager wiring compiles and runs, and to give feature developers a worked example (parallel to `docs/FEATURE_TEMPLATE.md`).

- [ ] **Step 1: Add dependencies to the version catalog**

In `gradle/libs.versions.toml`, add to `[versions]`:

```toml
workRuntime = "2.11.2"
androidxHilt = "1.4.0"
```

Add to `[libraries]`:

```toml
androidx-work-runtime = { group = "androidx.work", name = "work-runtime", version.ref = "workRuntime" }
androidx-hilt-work = { group = "androidx.hilt", name = "hilt-work", version.ref = "androidxHilt" }
androidx-hilt-compiler = { group = "androidx.hilt", name = "hilt-compiler", version.ref = "androidxHilt" }
```

- [ ] **Step 2: Add dependencies to `app/build.gradle.kts`**

Add to `dependencies { ... }`, right after `implementation(libs.androidx.startup.runtime)`:

```kotlin
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
```

- [ ] **Step 3: Disable WorkManager's default initializer**

In `app/src/main/AndroidManifest.xml`, add one more `<meta-data>` inside the `<provider android:name="androidx.startup.InitializationProvider" ...>` block added in Task 5 (so WorkManager doesn't build its own default `Configuration` before our `Configuration.Provider` runs):

```xml
            <meta-data
                android:name="androidx.work.WorkManagerInitializer"
                android:value="androidx.startup"
                tools:node="remove" />
```

- [ ] **Step 4: Implement `Configuration.Provider` with the Hilt worker factory**

Replace the full contents of `app/src/main/java/com/example/androidxmlbase/AndroidXmlBaseApplication.kt`:

```kotlin
package com.example.androidxmlbase

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Startup work (logging, DB passphrase warm-up, theme application) runs via the
 * `androidx.startup` `Initializer`s in `core/startup/`, registered in AndroidManifest.xml —
 * not here. See `docs/CORE_MODULES.md` → `core/startup`.
 *
 * `Configuration.Provider` supplies `HiltWorkerFactory` so `@HiltWorker` classes (see
 * `core/work/`) get constructor injection; WorkManager's default initializer is disabled in the
 * manifest so this custom configuration is the one actually used.
 */
@HiltAndroidApp
class AndroidXmlBaseApplication :
    Application(),
    Configuration.Provider {
        @Inject
        lateinit var workerFactory: HiltWorkerFactory

        override val workManagerConfiguration: Configuration
            get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
    }
```

- [ ] **Step 5: Create the reference `@HiltWorker`**

```kotlin
// app/src/main/java/com/example/androidxmlbase/core/work/SampleHeartbeatWorker.kt
package com.example.androidxmlbase.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Reference implementation showing the `@HiltWorker` + `CoroutineWorker` pattern this base wires
 * up (Hilt worker factory, `Configuration.Provider` in `AndroidXmlBaseApplication`). Copy this
 * shape for real background work — this worker itself is not scheduled anywhere by default.
 */
@HiltWorker
class SampleHeartbeatWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted workerParameters: WorkerParameters,
    ) : CoroutineWorker(context, workerParameters) {
        override suspend fun doWork(): Result {
            Timber.tag(TAG).i("Heartbeat worker executed")
            return Result.success()
        }

        private companion object {
            const val TAG = "SampleHeartbeatWorker"
        }
    }
```

- [ ] **Step 6: Add the `work-testing` dependency for the instrumented test**

In `gradle/libs.versions.toml`, add to `[libraries]` (reuses the `workRuntime` version already added in Step 1):

```toml
androidx-work-testing = { group = "androidx.work", name = "work-testing", version.ref = "workRuntime" }
```

In `app/build.gradle.kts`, add to `dependencies { ... }` right after `androidTestImplementation(libs.androidx.espresso.core)`:

```kotlin
    androidTestImplementation(libs.androidx.work.testing)
```

- [ ] **Step 7: Write the instrumented test**

```kotlin
// app/src/androidTest/java/com/example/androidxmlbase/core/work/SampleHeartbeatWorkerTest.kt
package com.example.androidxmlbase.core.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SampleHeartbeatWorkerTest {
    @Test
    fun doWork_returnsSuccess() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val worker = TestListenableWorkerBuilder<SampleHeartbeatWorker>(context).build()

            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.success(), result)
        }
}
```

- [ ] **Step 8: Run the instrumented test (requires a connected device/emulator)**

Run: `./gradlew connectedAndroidTest`
Expected: PASS.

If no device/emulator is available, note this explicitly as unverified in the task's commit/PR description rather than skipping the step silently.

- [ ] **Step 9: Exclude the Worker from Kover's JVM-only coverage (it's verified via `connectedAndroidTest`, not `test`)**

In `app/build.gradle.kts`, add `"*.core.work.SampleHeartbeatWorker"` to the existing `kover { reports { filters { excludes { classes(...) } } } }` list, right after `"*.core.ui.components.*"`.

- [ ] **Step 10: Document in `docs/CORE_MODULES.md`**

Add a new section after `core/startup`:

```markdown
## `core/work`

WorkManager wiring: `AndroidXmlBaseApplication` implements `Configuration.Provider`, supplying `HiltWorkerFactory` so `@HiltWorker` classes get constructor injection. WorkManager's default initializer is disabled in `AndroidManifest.xml` (`androidx.work.WorkManagerInitializer` removed from the `androidx.startup.InitializationProvider` merge) so this custom configuration is the one actually used.

- `SampleHeartbeatWorker` (`@HiltWorker`, `CoroutineWorker`) — reference implementation only, not scheduled by default. Copy this shape (constructor pattern, `@Assisted context`/`@Assisted workerParameters`) for real background work.

**Consumers:** none yet — this is infrastructure for the first feature that needs background work.
```

- [ ] **Step 11: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS, no regressions.

- [ ] **Step 12: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/AndroidManifest.xml \
        app/src/main/java/com/example/androidxmlbase/AndroidXmlBaseApplication.kt \
        app/src/main/java/com/example/androidxmlbase/core/work/SampleHeartbeatWorker.kt \
        app/src/androidTest/java/com/example/androidxmlbase/core/work/SampleHeartbeatWorkerTest.kt \
        docs/CORE_MODULES.md
git commit -m "feat: wire WorkManager with Hilt worker factory and a reference @HiltWorker"
```

---

## Task 7: Baseline Profiles

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts` (root)
- Modify: `settings.gradle.kts`
- Modify: `app/build.gradle.kts`
- Create: `baselineprofile/build.gradle.kts`
- Create: `baselineprofile/src/main/AndroidManifest.xml`
- Create: `baselineprofile/src/main/java/com/example/androidxmlbase/baselineprofile/BaselineProfileGenerator.kt`
- Modify: `docs/CORE_MODULES.md`

**Interfaces:**
- Consumes: `MainActivity`'s `btnOpenDemo` view id (`app/src/main/res/layout/activity_main.xml`) as the one interaction exercised in the generated profile.
- Produces: `app/src/main/generated/baselineProfiles/baseline-prof.txt` (generated at build time by the plugin — not hand-written, not committed by this task; the plugin writes it during `:app:generateReleaseBaselineProfile`).

- [ ] **Step 1: Add plugin + dependency versions to the version catalog**

In `gradle/libs.versions.toml`, add to `[versions]`:

```toml
baselineProfile = "1.3.1"
benchmarkMacro = "1.4.1"
profileinstaller = "1.4.1"
uiautomator = "2.2.0"
```

Add to `[libraries]`:

```toml
androidx-profileinstaller = { group = "androidx.profileinstaller", name = "profileinstaller", version.ref = "profileinstaller" }
androidx-benchmark-macro-junit4 = { group = "androidx.benchmark", name = "benchmark-macro-junit4", version.ref = "benchmarkMacro" }
androidx-uiautomator = { group = "androidx.test.uiautomator", name = "uiautomator", version.ref = "uiautomator" }
```

Add to `[plugins]`:

```toml
android-test = { id = "com.android.test", version.ref = "agp" }
baselineprofile = { id = "androidx.baselineprofile", version.ref = "baselineProfile" }
```

- [ ] **Step 2: Register the plugins at the root and declare the new module**

In `build.gradle.kts` (root), add to the `plugins { ... }` block:

```kotlin
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.baselineprofile) apply false
```

In `settings.gradle.kts`, add after `include(":app")`:

```kotlin
include(":baselineprofile")
```

- [ ] **Step 3: Create the `:baselineprofile` module**

```kotlin
// baselineprofile/build.gradle.kts
plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.example.androidxmlbase.baselineprofile"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 28
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}

baselineProfile {
    useConnectedDevices = true
}
```

- [ ] **Step 4: Add the module's manifest**

```xml
<!-- baselineprofile/src/main/AndroidManifest.xml -->
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" />
```

- [ ] **Step 5: Write the profile generator**

```kotlin
// baselineprofile/src/main/java/com/example/androidxmlbase/baselineprofile/BaselineProfileGenerator.kt
package com.example.androidxmlbase.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the app's critical startup path (cold launch → open the demo screen → back) so the
 * generated `baseline-prof.txt` covers real code paths, not just process init.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generateStartupProfile() =
        baselineProfileRule.collect(packageName = "com.example.androidxmlbase") {
            pressHome()
            startActivityAndWait()

            device.wait(Until.hasObject(By.res(packageName, "btnOpenDemo")), 5_000)
            device.findObject(By.res(packageName, "btnOpenDemo"))?.click()
            device.wait(Until.hasObject(By.pkg(packageName).depth(0)), 5_000)
            device.pressBack()
        }
}
```

- [ ] **Step 6: Wire the app module to consume the generated profile**

In `app/build.gradle.kts`, add to the `plugins { ... }` block:

```kotlin
    alias(libs.plugins.baselineprofile)
```

Add to `dependencies { ... }`, right after `implementation(libs.androidx.startup.runtime)`:

```kotlin
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))
```

- [ ] **Step 7: Generate and verify the profile (requires a connected device/emulator, API 28+)**

Run: `./gradlew :app:generateReleaseBaselineProfile`
Expected: task succeeds and writes `app/src/main/generated/baselineProfiles/baseline-prof.txt` with a non-empty rule list (hundreds of `H`/`S`/`P` lines referencing `Lcom/example/androidxmlbase/...` classes).

If no device/emulator is available, note this explicitly as unverified rather than skipping the step silently — this task cannot be considered done without having generated a real profile at least once.

- [ ] **Step 8: Commit the generated profile alongside the wiring**

```bash
git add gradle/libs.versions.toml build.gradle.kts settings.gradle.kts app/build.gradle.kts \
        baselineprofile/ app/src/main/generated/baselineProfiles/baseline-prof.txt
git commit -m "feat: add Baseline Profile generation module and wire it into :app"
```

- [ ] **Step 9: Document in `docs/CORE_MODULES.md`**

Add a new top-level section at the end of the file:

```markdown
## `:baselineprofile` (separate Gradle module, not `core/`)

A `com.android.test`-type module containing only a Macrobenchmark profile generator — no business/feature code. Exempted from the single-module rule in `CLAUDE.md` because it's closer to `androidTest` than to a feature module.

- `BaselineProfileGenerator` — drives a cold launch + "open demo screen" + back, via `BaselineProfileRule`. Run `./gradlew :app:generateReleaseBaselineProfile` to regenerate `app/src/main/generated/baselineProfiles/baseline-prof.txt` after significant startup-path changes.

**Consumers:** `:app` (via `baselineProfile(project(":baselineprofile"))` and `androidx.profileinstaller:profileinstaller`, which installs the checked-in profile at app install time).
```

- [ ] **Step 10: Final full verification**

Run: `./gradlew check`
Expected: PASS.

---

## Post-plan follow-up (not part of this plan's scope)

- Once a crash-reporting SDK is chosen, wire it into `ReleaseTree.log()` (Task 4) instead of the current `Log.println` fallback.
- Once a feature needs scheduled/periodic background work, copy `SampleHeartbeatWorker`'s shape (Task 6) instead of writing a new `Worker` from scratch.
- Re-run `./gradlew :app:generateReleaseBaselineProfile` (Task 7) whenever the app's cold-start path changes meaningfully.
