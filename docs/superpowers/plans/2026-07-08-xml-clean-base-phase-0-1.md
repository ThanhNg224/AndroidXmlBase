# Android XML Clean Base — Phase 0 + Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert the Android Studio Compose starter in this repo into an XML + ViewBinding app, then stand up the `core/architecture` MVVM primitives and a minimal `feature/demo` skeleton that proves the primitives work end to end — matching Phase 0 and Phase 1 of `docs/BASE_PROJECT_PORT_PLAN.md`.

**Architecture:** Single Gradle module (`app`). Presentation talks to a `StateViewModel<S, E, F>` base class exposing `StateFlow<S>` for screen state and a `Flow<F>` (backed by a `Channel`) for one-shot effects (toast/navigation). Domain use cases are plain Kotlin classes with no Android dependency. No network/storage/DI framework is introduced in this plan — those are separate future phases.

**Tech Stack:** Kotlin 2.2.10, AGP 9.2.1, AndroidX AppCompat/Material/ConstraintLayout, Kotlin Coroutines, AndroidX Lifecycle ViewModel, JUnit4, Espresso, Turbine.

## Global Constraints

- Baseline check before Task 1 found `./gradlew :app:testDebugUnitTest` already fails on a clean checkout: `androidx.core:core-ktx:1.19.0` requires `compileSdk >= 37`, but the project was pinned to `release(36) { minorApiLevel = 1 }`. Confirmed with the project owner: bump `compileSdk` to `release(37)` in Task 1 (Step 5) to unblock this — a one-line change matching AGP's own recommended action. Do not change `minSdk` (24), `targetSdk` (36), or `compileOptions` (Java 11) — those remain the explicit "keep unless product needs otherwise" decisions from `docs/BASE_PROJECT_PORT_PLAN.md`.
- Keep `applicationId`/`namespace` as `com.example.androidxmlbase` (confirmed with the project owner — do not rename packages in this plan).
- Stay single-module (`app` only). Do not create new Gradle modules.
- Do not introduce a DI framework (Hilt/Koin). Use constructor injection with plain factory classes.
- Do not add network or persistence dependencies (Retrofit, OkHttp, Room, DataStore) — that is Phase 2/3, out of scope here.
- Exact new library versions (verified against Google Maven / Maven Central on 2026-07-08, use exactly these, do not substitute newer/older versions without re-checking):
  - `androidx.appcompat:appcompat:1.7.1`
  - `com.google.android.material:material:1.14.0`
  - `androidx.constraintlayout:constraintlayout:2.2.1`
  - `androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0` (same train as the existing `lifecycleRuntimeKtx` version)
  - `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2`
  - `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2`
  - `org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2`
  - `app.cash.turbine:turbine:1.2.0`
- Do not touch `app/src/test/java/com/example/androidxmlbase/ExampleUnitTest.kt` or `app/src/androidTest/java/com/example/androidxmlbase/ExampleInstrumentedTest.kt` — they are unrelated stock template tests and already pass.
- A physical device is connected (`adb devices` shows one `device`) — use `connectedDebugAndroidTest` for instrumented tests, no emulator needed. Deviation confirmed with the project owner mid-Task-3: the device dropped off `adb` and did not reconnect, and no emulator is configured as a fallback. Task 3's instrumented `DemoActivityTest` steps (Steps 11, 17, and the `connectedDebugAndroidTest` leg of Step 19) are downgraded from required to best-effort for this run — Task 3 ships and commits on `IncrementCounterUseCaseTest` + `DemoViewModelTest` (unit) + `assembleDebug` passing. `DemoActivityTest` stays in the codebase and must be run (and any failures fixed) the next time a device or emulator is available; do not treat its steps as satisfied until that run happens.

---

### Task 1: Convert starter from Compose to XML + ViewBinding (Phase 0)

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/res/values/themes.xml`
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/example/androidxmlbase/MainActivity.kt`
- Create: `app/src/main/res/layout/activity_main.xml`
- Delete: `app/src/main/java/com/example/androidxmlbase/ui/theme/Color.kt`
- Delete: `app/src/main/java/com/example/androidxmlbase/ui/theme/Theme.kt`
- Delete: `app/src/main/java/com/example/androidxmlbase/ui/theme/Type.kt`
- Test: `app/src/androidTest/java/com/example/androidxmlbase/MainActivityTest.kt`

**Interfaces:**
- Produces: `MainActivity` (now `AppCompatActivity`, inflates `ActivityMainBinding`), layout id `R.layout.activity_main` with view `R.id.tvGreeting` (`TextView`) and `R.id.btnOpenDemo` (`Button`, wired in Task 3), string `R.string.hello_android`. Task 3 modifies this same `MainActivity.kt` to add demo navigation.

- [ ] **Step 1: Write the failing instrumented test**

Create `app/src/androidTest/java/com/example/androidxmlbase/MainActivityTest.kt`:

```kotlin
package com.example.androidxmlbase

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun greetingTextView_isDisplayed_withExpectedText() {
        onView(withId(R.id.tvGreeting))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.hello_android)))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.androidxmlbase.MainActivityTest`
Expected: BUILD FAILED — `compileDebugAndroidTestKotlin` fails with `unresolved reference: tvGreeting` and `unresolved reference: hello_android` (the layout/string don't exist yet).

- [ ] **Step 3: Update the version catalog — remove Compose, add View-system libraries**

Replace `gradle/libs.versions.toml` with:

```toml
[versions]
agp = "9.2.1"
coreKtx = "1.19.0"
junit = "4.13.2"
junitVersion = "1.3.0"
espressoCore = "3.7.0"
lifecycleRuntimeKtx = "2.11.0"
kotlin = "2.2.10"
appcompat = "1.7.1"
material = "1.14.0"
constraintlayout = "2.2.1"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
material = { group = "com.google.android.material", name = "material", version.ref = "material" }
androidx-constraintlayout = { group = "androidx.constraintlayout", name = "constraintlayout", version.ref = "constraintlayout" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
```

- [ ] **Step 4: Remove the Compose compiler plugin from the root build script**

Replace `build.gradle.kts` with:

```kotlin
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
}
```

- [ ] **Step 5: Update `app/build.gradle.kts` — enable ViewBinding, swap dependencies**

Replace `app/build.gradle.kts` with:

```kotlin
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.androidxmlbase"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.androidxmlbase"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
```

`compileSdk` moves from `release(36) { minorApiLevel = 1 }` to `release(37)` here — confirmed with the project owner after the baseline-check failure above. This is the only global-constraint deviation in this plan.

- [ ] **Step 6: Delete the Compose theme package**

```bash
rm app/src/main/java/com/example/androidxmlbase/ui/theme/Color.kt
rm app/src/main/java/com/example/androidxmlbase/ui/theme/Theme.kt
rm app/src/main/java/com/example/androidxmlbase/ui/theme/Type.kt
rmdir app/src/main/java/com/example/androidxmlbase/ui/theme
rmdir app/src/main/java/com/example/androidxmlbase/ui
```

- [ ] **Step 7: Switch the app theme to a Material Components base**

Replace `app/src/main/res/values/themes.xml` with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>

    <style name="Theme.AndroidXmlBase" parent="Theme.MaterialComponents.DayNight.NoActionBar" />
</resources>
```

`AppCompatActivity` (and Material widgets) require a `Theme.AppCompat`/`Theme.MaterialComponents`/`Theme.Material3` descendant; the previous `android:Theme.Material.Light.NoActionBar` is a platform theme and would crash at runtime with `MainActivity` now extending `AppCompatActivity`.

- [ ] **Step 8: Remove unused Compose-template colors**

Replace `app/src/main/res/values/colors.xml` with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>
</resources>
```

- [ ] **Step 9: Add the greeting and demo-navigation strings**

Replace `app/src/main/res/values/strings.xml` with:

```xml
<resources>
    <string name="app_name">AndroidXmlBase</string>
    <string name="hello_android">Hello Android!</string>
    <string name="view_demo">View Demo</string>
</resources>
```

- [ ] **Step 10: Create the XML layout**

Create `app/src/main/res/layout/activity_main.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <TextView
        android:id="@+id/tvGreeting"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/hello_android"
        app:layout_constraintBottom_toTopOf="@id/btnOpenDemo"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintVertical_chainStyle="packed" />

    <Button
        android:id="@+id/btnOpenDemo"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:text="@string/view_demo"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/tvGreeting" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

`btnOpenDemo` has no click handler yet — Task 3 wires it once `DemoActivity` exists.

- [ ] **Step 11: Rewrite MainActivity for ViewBinding**

Replace `app/src/main/java/com/example/androidxmlbase/MainActivity.kt` with:

```kotlin
package com.example.androidxmlbase

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.androidxmlbase.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
```

- [ ] **Step 12: Run the instrumented test to verify it passes**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.androidxmlbase.MainActivityTest`
Expected: BUILD SUCCESSFUL, `1 test, 0 failures` in the HTML report at `app/build/reports/androidTests/connected/debug/`.

- [ ] **Step 13: Run the full debug build and unit tests**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL. No Compose dependency remains anywhere in `app/build.gradle.kts` or `gradle/libs.versions.toml`.

- [ ] **Step 14: Commit**

```bash
git add gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts \
  app/src/main/res/values/themes.xml app/src/main/res/values/colors.xml \
  app/src/main/res/values/strings.xml app/src/main/res/layout/activity_main.xml \
  app/src/main/java/com/example/androidxmlbase/MainActivity.kt \
  app/src/androidTest/java/com/example/androidxmlbase/MainActivityTest.kt
git rm -r app/src/main/java/com/example/androidxmlbase/ui
git commit -m "$(cat <<'EOF'
feat: convert starter app from Compose to XML + ViewBinding

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: Core architecture primitives (Phase 1a)

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/example/androidxmlbase/core/architecture/UiState.kt`
- Create: `app/src/main/java/com/example/androidxmlbase/core/architecture/UiEvent.kt`
- Create: `app/src/main/java/com/example/androidxmlbase/core/architecture/UiEffect.kt`
- Create: `app/src/main/java/com/example/androidxmlbase/core/architecture/ResultState.kt`
- Create: `app/src/main/java/com/example/androidxmlbase/core/architecture/AppDispatchers.kt`
- Create: `app/src/main/java/com/example/androidxmlbase/core/architecture/UseCase.kt`
- Create: `app/src/main/java/com/example/androidxmlbase/core/architecture/StateViewModel.kt`
- Create: `app/src/test/java/com/example/androidxmlbase/testutil/MainDispatcherRule.kt`
- Test: `app/src/test/java/com/example/androidxmlbase/core/architecture/ResultStateTest.kt`
- Test: `app/src/test/java/com/example/androidxmlbase/core/architecture/DefaultAppDispatchersTest.kt`
- Test: `app/src/test/java/com/example/androidxmlbase/core/architecture/StateViewModelTest.kt`

**Interfaces:**
- Consumes: nothing from Task 1 (pure Kotlin/Jetpack, no dependency on `MainActivity`).
- Produces (used by Task 3):
  - `interface UiState`, `interface UiEvent`, `interface UiEffect` — empty marker interfaces.
  - `sealed interface ResultState<out T>` with `Loading`, `Success<T>(val data: T)`, `Error(val message: String, val cause: Throwable? = null)`, plus `inline fun <T, R> ResultState<T>.fold(onLoading: () -> R, onSuccess: (T) -> R, onError: (String, Throwable?) -> R): R`.
  - `interface AppDispatchers { val main: CoroutineDispatcher; val io: CoroutineDispatcher; val default: CoroutineDispatcher }` and `class DefaultAppDispatchers : AppDispatchers`.
  - `interface UseCase<in P, R> { suspend operator fun invoke(params: P): R }`.
  - `abstract class StateViewModel<S : UiState, E : UiEvent, F : UiEffect>(initialState: S) : ViewModel()` exposing `val state: StateFlow<S>`, `val effect: Flow<F>`, `protected val currentState: S`, `abstract fun onEvent(event: E)`, `protected fun setState(reducer: S.() -> S)`, `protected fun sendEffect(effect: F)`.
  - `class MainDispatcherRule(testDispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher()` — JVM test util for any future `StateViewModel` subclass test.

- [ ] **Step 1: Add coroutines, ViewModel and test dependencies**

Replace `gradle/libs.versions.toml` with:

```toml
[versions]
agp = "9.2.1"
coreKtx = "1.19.0"
junit = "4.13.2"
junitVersion = "1.3.0"
espressoCore = "3.7.0"
lifecycleRuntimeKtx = "2.11.0"
kotlin = "2.2.10"
appcompat = "1.7.1"
material = "1.14.0"
constraintlayout = "2.2.1"
kotlinxCoroutines = "1.10.2"
turbine = "1.2.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-viewmodel-ktx = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
material = { group = "com.google.android.material", name = "material", version.ref = "material" }
androidx-constraintlayout = { group = "androidx.constraintlayout", name = "constraintlayout", version.ref = "constraintlayout" }
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "kotlinxCoroutines" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "kotlinxCoroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "kotlinxCoroutines" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
```

Update `app/build.gradle.kts` dependencies block to:

```kotlin
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
```

(Leave everything else in `app/build.gradle.kts` — `plugins`, `android { ... }` — exactly as Task 1 left it.)

- [ ] **Step 2: Write the failing tests for `ResultState`**

Create `app/src/test/java/com/example/androidxmlbase/core/architecture/ResultStateTest.kt`:

```kotlin
package com.example.androidxmlbase.core.architecture

import org.junit.Assert.assertEquals
import org.junit.Test

class ResultStateTest {

    @Test
    fun `fold invokes onSuccess branch for Success`() {
        val result: ResultState<Int> = ResultState.Success(42)

        val text = result.fold(
            onLoading = { "loading" },
            onSuccess = { "success:$it" },
            onError = { message, _ -> "error:$message" },
        )

        assertEquals("success:42", text)
    }

    @Test
    fun `fold invokes onError branch for Error`() {
        val result: ResultState<Int> = ResultState.Error("boom")

        val text = result.fold(
            onLoading = { "loading" },
            onSuccess = { "success:$it" },
            onError = { message, _ -> "error:$message" },
        )

        assertEquals("error:boom", text)
    }

    @Test
    fun `fold invokes onLoading branch for Loading`() {
        val result: ResultState<Int> = ResultState.Loading

        val text = result.fold(
            onLoading = { "loading" },
            onSuccess = { "success:$it" },
            onError = { message, _ -> "error:$message" },
        )

        assertEquals("loading", text)
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.androidxmlbase.core.architecture.ResultStateTest"`
Expected: BUILD FAILED — `compileDebugUnitTestKotlin` fails with `unresolved reference: ResultState` (the type does not exist yet).

- [ ] **Step 4: Implement `UiState`, `UiEvent`, `UiEffect`, `ResultState`**

Create `app/src/main/java/com/example/androidxmlbase/core/architecture/UiState.kt`:

```kotlin
package com.example.androidxmlbase.core.architecture

interface UiState
```

Create `app/src/main/java/com/example/androidxmlbase/core/architecture/UiEvent.kt`:

```kotlin
package com.example.androidxmlbase.core.architecture

interface UiEvent
```

Create `app/src/main/java/com/example/androidxmlbase/core/architecture/UiEffect.kt`:

```kotlin
package com.example.androidxmlbase.core.architecture

interface UiEffect
```

Create `app/src/main/java/com/example/androidxmlbase/core/architecture/ResultState.kt`:

```kotlin
package com.example.androidxmlbase.core.architecture

sealed interface ResultState<out T> {
    data object Loading : ResultState<Nothing>
    data class Success<T>(val data: T) : ResultState<T>
    data class Error(val message: String, val cause: Throwable? = null) : ResultState<Nothing>
}

inline fun <T, R> ResultState<T>.fold(
    onLoading: () -> R,
    onSuccess: (T) -> R,
    onError: (String, Throwable?) -> R,
): R = when (this) {
    is ResultState.Loading -> onLoading()
    is ResultState.Success -> onSuccess(data)
    is ResultState.Error -> onError(message, cause)
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.androidxmlbase.core.architecture.ResultStateTest"`
Expected: BUILD SUCCESSFUL, 3 tests passed.

- [ ] **Step 6: Write the failing test for `AppDispatchers`**

Create `app/src/test/java/com/example/androidxmlbase/core/architecture/DefaultAppDispatchersTest.kt`:

```kotlin
package com.example.androidxmlbase.core.architecture

import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultAppDispatchersTest {

    private val dispatchers: AppDispatchers = DefaultAppDispatchers()

    @Test
    fun `exposes the IO dispatcher`() {
        assertEquals(Dispatchers.IO, dispatchers.io)
    }

    @Test
    fun `exposes the Default dispatcher`() {
        assertEquals(Dispatchers.Default, dispatchers.default)
    }
}
```

- [ ] **Step 7: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.androidxmlbase.core.architecture.DefaultAppDispatchersTest"`
Expected: BUILD FAILED — `unresolved reference: AppDispatchers` / `DefaultAppDispatchers`.

- [ ] **Step 8: Implement `AppDispatchers`**

Create `app/src/main/java/com/example/androidxmlbase/core/architecture/AppDispatchers.kt`:

```kotlin
package com.example.androidxmlbase.core.architecture

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

interface AppDispatchers {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}

class DefaultAppDispatchers : AppDispatchers {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
}
```

- [ ] **Step 9: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.androidxmlbase.core.architecture.DefaultAppDispatchersTest"`
Expected: BUILD SUCCESSFUL, 2 tests passed.

- [ ] **Step 10: Implement the `UseCase` convention (no test — plain contract)**

Create `app/src/main/java/com/example/androidxmlbase/core/architecture/UseCase.kt`:

```kotlin
package com.example.androidxmlbase.core.architecture

interface UseCase<in P, R> {
    suspend operator fun invoke(params: P): R
}
```

This is the convention for use cases that need coroutines (e.g. calling a repository). It has no implementer yet in this plan — `feature/demo`'s `IncrementCounterUseCase` (Task 3) is purely synchronous domain logic with no I/O, so it stays a plain callable class instead of implementing this suspend contract. The first real implementer arrives with the network core (Phase 3).

- [ ] **Step 11: Add the JVM test dispatcher rule**

Create `app/src/test/java/com/example/androidxmlbase/testutil/MainDispatcherRule.kt`:

```kotlin
package com.example.androidxmlbase.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

This is shared JVM test infrastructure: every `StateViewModel` subclass test (this task's `StateViewModelTest` below, and Task 3's `DemoViewModelTest`) needs `Dispatchers.Main` set before `viewModelScope` will work on the JVM.

- [ ] **Step 12: Write the failing tests for `StateViewModel`**

Create `app/src/test/java/com/example/androidxmlbase/core/architecture/StateViewModelTest.kt`:

```kotlin
package com.example.androidxmlbase.core.architecture

import app.cash.turbine.test
import com.example.androidxmlbase.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private data class CounterState(val value: Int = 0) : UiState

private sealed interface CounterEvent : UiEvent {
    data object Increment : CounterEvent
}

private sealed interface CounterEffect : UiEffect {
    data class Announce(val text: String) : CounterEffect
}

private class CounterViewModel : StateViewModel<CounterState, CounterEvent, CounterEffect>(CounterState()) {
    override fun onEvent(event: CounterEvent) {
        when (event) {
            is CounterEvent.Increment -> {
                setState { copy(value = value + 1) }
                sendEffect(CounterEffect.Announce("incremented to ${currentState.value}"))
            }
        }
    }
}

class StateViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `setState updates the exposed state flow`() = runTest {
        val viewModel = CounterViewModel()

        viewModel.state.test {
            assertEquals(0, awaitItem().value)
            viewModel.onEvent(CounterEvent.Increment)
            assertEquals(1, awaitItem().value)
        }
    }

    @Test
    fun `sendEffect emits a one-shot effect`() = runTest {
        val viewModel = CounterViewModel()

        viewModel.effect.test {
            viewModel.onEvent(CounterEvent.Increment)
            assertEquals(CounterEffect.Announce("incremented to 1"), awaitItem())
        }
    }
}
```

- [ ] **Step 13: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.androidxmlbase.core.architecture.StateViewModelTest"`
Expected: BUILD FAILED — `unresolved reference: StateViewModel`.

- [ ] **Step 14: Implement `StateViewModel`**

Create `app/src/main/java/com/example/androidxmlbase/core/architecture/StateViewModel.kt`:

```kotlin
package com.example.androidxmlbase.core.architecture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

abstract class StateViewModel<S : UiState, E : UiEvent, F : UiEffect>(
    initialState: S,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effect = Channel<F>(Channel.BUFFERED)
    val effect: Flow<F> = _effect.receiveAsFlow()

    protected val currentState: S
        get() = _state.value

    abstract fun onEvent(event: E)

    protected fun setState(reducer: S.() -> S) {
        _state.value = currentState.reducer()
    }

    protected fun sendEffect(effect: F) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }
}
```

- [ ] **Step 15: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.androidxmlbase.core.architecture.StateViewModelTest"`
Expected: BUILD SUCCESSFUL, 2 tests passed.

- [ ] **Step 16: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass (existing `ExampleUnitTest` plus the 7 new tests from this task).

- [ ] **Step 17: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts \
  app/src/main/java/com/example/androidxmlbase/core \
  app/src/test/java/com/example/androidxmlbase/core \
  app/src/test/java/com/example/androidxmlbase/testutil
git commit -m "$(cat <<'EOF'
feat: add core/architecture MVVM primitives (UiState/UiEvent/UiEffect, ResultState, AppDispatchers, UseCase, StateViewModel)

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: Demo feature skeleton + architecture docs (Phase 1b)

**Files:**
- Create: `app/src/main/java/com/example/androidxmlbase/feature/demo/domain/usecase/IncrementCounterUseCase.kt`
- Create: `app/src/main/java/com/example/androidxmlbase/feature/demo/presentation/state/DemoUiState.kt`
- Create: `app/src/main/java/com/example/androidxmlbase/feature/demo/presentation/state/DemoUiEvent.kt`
- Create: `app/src/main/java/com/example/androidxmlbase/feature/demo/presentation/state/DemoUiEffect.kt`
- Create: `app/src/main/java/com/example/androidxmlbase/feature/demo/presentation/viewmodel/DemoViewModel.kt`
- Create: `app/src/main/java/com/example/androidxmlbase/feature/demo/presentation/viewmodel/DemoViewModelFactory.kt`
- Create: `app/src/main/java/com/example/androidxmlbase/feature/demo/presentation/ui/DemoActivity.kt`
- Create: `app/src/main/res/layout/activity_demo.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/example/androidxmlbase/MainActivity.kt`
- Modify: `docs/ARCHITECTURE.md`
- Test: `app/src/test/java/com/example/androidxmlbase/feature/demo/domain/usecase/IncrementCounterUseCaseTest.kt`
- Test: `app/src/test/java/com/example/androidxmlbase/feature/demo/presentation/viewmodel/DemoViewModelTest.kt`
- Test: `app/src/androidTest/java/com/example/androidxmlbase/feature/demo/presentation/ui/DemoActivityTest.kt`

**Interfaces:**
- Consumes: `StateViewModel<S, E, F>`, `UiState`, `UiEvent`, `UiEffect` (Task 2); `MainActivity` and the `Theme.AndroidXmlBase` theme (Task 1).
- Produces: `DemoActivity` reachable from `MainActivity`'s `btnOpenDemo` button; no `data/` package (this feature never reads or writes real data, so a repository/data source would be an unused abstraction — one gets added when Phase 2 or 3 gives the feature something to fetch or persist).

- [ ] **Step 1: Write the failing test for the counter business rule**

Create `app/src/test/java/com/example/androidxmlbase/feature/demo/domain/usecase/IncrementCounterUseCaseTest.kt`:

```kotlin
package com.example.androidxmlbase.feature.demo.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class IncrementCounterUseCaseTest {

    private val useCase = IncrementCounterUseCase()

    @Test
    fun `increments count by one when below the max`() {
        val result = useCase(currentCount = 0)

        assertEquals(1, result.count)
        assertEquals(false, result.capped)
    }

    @Test
    fun `reports capped when reaching the max count`() {
        val result = useCase(currentCount = 9)

        assertEquals(10, result.count)
        assertEquals(true, result.capped)
    }

    @Test
    fun `does not increment beyond the max count`() {
        val result = useCase(currentCount = 10)

        assertEquals(10, result.count)
        assertEquals(true, result.capped)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.androidxmlbase.feature.demo.domain.usecase.IncrementCounterUseCaseTest"`
Expected: BUILD FAILED — `unresolved reference: IncrementCounterUseCase`.

- [ ] **Step 3: Implement `IncrementCounterUseCase`**

Create `app/src/main/java/com/example/androidxmlbase/feature/demo/domain/usecase/IncrementCounterUseCase.kt`:

```kotlin
package com.example.androidxmlbase.feature.demo.domain.usecase

class IncrementCounterUseCase {

    operator fun invoke(currentCount: Int): IncrementResult {
        if (currentCount >= MAX_COUNT) {
            return IncrementResult(count = currentCount, capped = true)
        }
        val nextCount = currentCount + 1
        return IncrementResult(count = nextCount, capped = nextCount == MAX_COUNT)
    }

    data class IncrementResult(val count: Int, val capped: Boolean)

    private companion object {
        const val MAX_COUNT = 10
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.androidxmlbase.feature.demo.domain.usecase.IncrementCounterUseCaseTest"`
Expected: BUILD SUCCESSFUL, 3 tests passed.

- [ ] **Step 5: Create the UI state/event/effect contracts (no test — plain data holders)**

Create `app/src/main/java/com/example/androidxmlbase/feature/demo/presentation/state/DemoUiState.kt`:

```kotlin
package com.example.androidxmlbase.feature.demo.presentation.state

import com.example.androidxmlbase.core.architecture.UiState

data class DemoUiState(
    val count: Int = 0,
) : UiState
```

Create `app/src/main/java/com/example/androidxmlbase/feature/demo/presentation/state/DemoUiEvent.kt`:

```kotlin
package com.example.androidxmlbase.feature.demo.presentation.state

import com.example.androidxmlbase.core.architecture.UiEvent

sealed interface DemoUiEvent : UiEvent {
    data object IncrementClicked : DemoUiEvent
}
```

Create `app/src/main/java/com/example/androidxmlbase/feature/demo/presentation/state/DemoUiEffect.kt`:

```kotlin
package com.example.androidxmlbase.feature.demo.presentation.state

import com.example.androidxmlbase.core.architecture.UiEffect

sealed interface DemoUiEffect : UiEffect {
    data class ShowToast(val message: String) : DemoUiEffect
}
```

- [ ] **Step 6: Write the failing test for `DemoViewModel`**

Create `app/src/test/java/com/example/androidxmlbase/feature/demo/presentation/viewmodel/DemoViewModelTest.kt`:

```kotlin
package com.example.androidxmlbase.feature.demo.presentation.viewmodel

import app.cash.turbine.test
import com.example.androidxmlbase.feature.demo.domain.usecase.IncrementCounterUseCase
import com.example.androidxmlbase.feature.demo.presentation.state.DemoUiEffect
import com.example.androidxmlbase.feature.demo.presentation.state.DemoUiEvent
import com.example.androidxmlbase.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DemoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `increment event increases the count in state`() = runTest {
        val viewModel = DemoViewModel(IncrementCounterUseCase())

        viewModel.state.test {
            assertEquals(0, awaitItem().count)
            viewModel.onEvent(DemoUiEvent.IncrementClicked)
            assertEquals(1, awaitItem().count)
        }
    }

    @Test
    fun `reaching the max count emits a show-toast effect`() = runTest {
        val viewModel = DemoViewModel(IncrementCounterUseCase())

        viewModel.effect.test {
            repeat(10) { viewModel.onEvent(DemoUiEvent.IncrementClicked) }
            assertEquals(DemoUiEffect.ShowToast("Max count reached"), awaitItem())
        }
    }
}
```

- [ ] **Step 7: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.androidxmlbase.feature.demo.presentation.viewmodel.DemoViewModelTest"`
Expected: BUILD FAILED — `unresolved reference: DemoViewModel`.

- [ ] **Step 8: Implement `DemoViewModel` and its factory**

Create `app/src/main/java/com/example/androidxmlbase/feature/demo/presentation/viewmodel/DemoViewModel.kt`:

```kotlin
package com.example.androidxmlbase.feature.demo.presentation.viewmodel

import com.example.androidxmlbase.core.architecture.StateViewModel
import com.example.androidxmlbase.feature.demo.domain.usecase.IncrementCounterUseCase
import com.example.androidxmlbase.feature.demo.presentation.state.DemoUiEffect
import com.example.androidxmlbase.feature.demo.presentation.state.DemoUiEvent
import com.example.androidxmlbase.feature.demo.presentation.state.DemoUiState

class DemoViewModel(
    private val incrementCounter: IncrementCounterUseCase,
) : StateViewModel<DemoUiState, DemoUiEvent, DemoUiEffect>(DemoUiState()) {

    override fun onEvent(event: DemoUiEvent) {
        when (event) {
            is DemoUiEvent.IncrementClicked -> onIncrementClicked()
        }
    }

    private fun onIncrementClicked() {
        val result = incrementCounter(currentState.count)
        setState { copy(count = result.count) }
        if (result.capped) {
            sendEffect(DemoUiEffect.ShowToast("Max count reached"))
        }
    }
}
```

Create `app/src/main/java/com/example/androidxmlbase/feature/demo/presentation/viewmodel/DemoViewModelFactory.kt`:

```kotlin
package com.example.androidxmlbase.feature.demo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.androidxmlbase.feature.demo.domain.usecase.IncrementCounterUseCase

class DemoViewModelFactory(
    private val incrementCounter: IncrementCounterUseCase = IncrementCounterUseCase(),
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DemoViewModel(incrementCounter) as T
    }
}
```

- [ ] **Step 9: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.androidxmlbase.feature.demo.presentation.viewmodel.DemoViewModelTest"`
Expected: BUILD SUCCESSFUL, 2 tests passed.

- [ ] **Step 10: Write the failing instrumented test for `DemoActivity`**

Create `app/src/androidTest/java/com/example/androidxmlbase/feature/demo/presentation/ui/DemoActivityTest.kt`:

```kotlin
package com.example.androidxmlbase.feature.demo.presentation.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidxmlbase.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DemoActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(DemoActivity::class.java)

    @Test
    fun incrementButton_updatesCountText() {
        onView(withId(R.id.tvCount)).check(matches(withText("0")))

        repeat(10) {
            onView(withId(R.id.btnIncrement)).perform(click())
        }

        onView(withId(R.id.tvCount)).check(matches(withText("10")))
    }
}
```

- [ ] **Step 11: Run the test to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.androidxmlbase.feature.demo.presentation.ui.DemoActivityTest`
Expected: BUILD FAILED — `unresolved reference: DemoActivity` (compile failure in `compileDebugAndroidTestKotlin`).

- [ ] **Step 12: Add the demo strings**

Update `app/src/main/res/values/strings.xml` to:

```xml
<resources>
    <string name="app_name">AndroidXmlBase</string>
    <string name="hello_android">Hello Android!</string>
    <string name="view_demo">View Demo</string>
    <string name="demo_title">Demo</string>
    <string name="increment">Increment</string>
</resources>
```

- [ ] **Step 13: Create the demo layout**

Create `app/src/main/res/layout/activity_demo.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".feature.demo.presentation.ui.DemoActivity">

    <TextView
        android:id="@+id/tvCount"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="0"
        app:layout_constraintBottom_toTopOf="@id/btnIncrement"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintVertical_chainStyle="packed" />

    <Button
        android:id="@+id/btnIncrement"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:text="@string/increment"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/tvCount" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

- [ ] **Step 14: Implement `DemoActivity`**

Create `app/src/main/java/com/example/androidxmlbase/feature/demo/presentation/ui/DemoActivity.kt`:

```kotlin
package com.example.androidxmlbase.feature.demo.presentation.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.androidxmlbase.databinding.ActivityDemoBinding
import com.example.androidxmlbase.feature.demo.presentation.state.DemoUiEffect
import com.example.androidxmlbase.feature.demo.presentation.state.DemoUiEvent
import com.example.androidxmlbase.feature.demo.presentation.viewmodel.DemoViewModel
import com.example.androidxmlbase.feature.demo.presentation.viewmodel.DemoViewModelFactory
import kotlinx.coroutines.launch

class DemoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDemoBinding

    private val viewModel: DemoViewModel by lazy {
        ViewModelProvider(this, DemoViewModelFactory()).get(DemoViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnIncrement.setOnClickListener {
            viewModel.onEvent(DemoUiEvent.IncrementClicked)
        }

        observeState()
        observeEffects()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.tvCount.text = state.count.toString()
                }
            }
        }
    }

    private fun observeEffects() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        is DemoUiEffect.ShowToast ->
                            Toast.makeText(this@DemoActivity, effect.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 15: Register `DemoActivity` in the manifest**

Update `app/src/main/AndroidManifest.xml` to:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.AndroidXmlBase">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.AndroidXmlBase"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        <activity
            android:name=".feature.demo.presentation.ui.DemoActivity"
            android:exported="false"
            android:label="@string/demo_title"
            android:theme="@style/Theme.AndroidXmlBase" />
    </application>

</manifest>
```

- [ ] **Step 16: Wire the "View Demo" button in `MainActivity`**

Update `app/src/main/java/com/example/androidxmlbase/MainActivity.kt` to:

```kotlin
package com.example.androidxmlbase

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.androidxmlbase.databinding.ActivityMainBinding
import com.example.androidxmlbase.feature.demo.presentation.ui.DemoActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnOpenDemo.setOnClickListener {
            startActivity(Intent(this, DemoActivity::class.java))
        }
    }
}
```

- [ ] **Step 17: Run the instrumented test to verify it passes**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.androidxmlbase.feature.demo.presentation.ui.DemoActivityTest`
Expected: BUILD SUCCESSFUL, `1 test, 0 failures`.

- [ ] **Step 18: Document the real package layout**

Append to the end of `docs/ARCHITECTURE.md` (after the existing "Architecture Review Checklist" section):

```markdown

## Current Package Layout (Phase 0-1)

Everything above this section describes the target architecture. The folders below are what actually exists in the codebase today; check here (or the source tree) before assuming a core module already exists.

app/src/main/java/com/example/androidxmlbase/
  MainActivity.kt                            # launcher screen, XML + ViewBinding
  core/
    architecture/
      UiState.kt
      UiEvent.kt
      UiEffect.kt
      ResultState.kt
      AppDispatchers.kt
      UseCase.kt
      StateViewModel.kt
  feature/
    demo/
      domain/
        usecase/IncrementCounterUseCase.kt
      presentation/
        state/DemoUiState.kt, DemoUiEvent.kt, DemoUiEffect.kt
        viewmodel/DemoViewModel.kt, DemoViewModelFactory.kt
        ui/DemoActivity.kt

`feature/demo` has no `data/` package. It does not read or write any real data, so a repository or data source would be an abstraction with nothing to abstract. That layer gets added once Phase 2 (storage) or Phase 3 (network) gives the feature something to fetch or persist.
```

- [ ] **Step 19: Run the full build and test suite**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest`
Expected: BUILD SUCCESSFUL, all unit and instrumented tests pass.

- [ ] **Step 20: Commit**

```bash
git add app/src/main/java/com/example/androidxmlbase/feature \
  app/src/main/java/com/example/androidxmlbase/MainActivity.kt \
  app/src/main/res/layout/activity_demo.xml \
  app/src/main/res/values/strings.xml \
  app/src/main/AndroidManifest.xml \
  app/src/test/java/com/example/androidxmlbase/feature \
  app/src/androidTest/java/com/example/androidxmlbase/feature \
  docs/ARCHITECTURE.md
git commit -m "$(cat <<'EOF'
feat: add feature/demo skeleton wired through StateViewModel, reachable from MainActivity

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```
