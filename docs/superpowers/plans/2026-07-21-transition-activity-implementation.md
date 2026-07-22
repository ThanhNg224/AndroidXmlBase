# Generic TransitionActivity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the single-purpose `LanguageTransitionActivity` (which shows a stale-locale loading text) with a generic, reusable `TransitionActivity` that runs a registered `TransitionAction` behind a theme-aware Lottie animation, with zero text.

**Architecture:** One concrete `TransitionActivity` (one Manifest entry, forever) hosts a Hilt multibinding map of `String -> TransitionAction`. `LanguageTransitionAction` is the first (and, for this plan, only) registered action, replacing `LanguageTransitionActivity`'s logic. Task 1 builds the new infrastructure additively (nothing existing is touched, so the app keeps building/passing at every step); Task 2 migrates the one real call site and deletes the old Activity/layout/strings/theme.

**Tech Stack:** Kotlin, Hilt (`@Binds @IntoMap @StringKey`), Coroutines (`lifecycleScope`, `delay`), Lottie-Android (`com.airbnb.android:lottie`), Material Components (`MaterialColors`), JUnit4 + `androidx.test` instrumented tests (real device/emulator, real Hilt graph — this repo has no Hilt test-double/runner infra, see `EncryptedSecureStoreTest.kt` and `MainActivityTest.kt` for the established pattern).

## Global Constraints

- minSdk 24, targetSdk/compileSdk 37, Kotlin JVM target 21 (`app/build.gradle.kts`).
- Version catalog only — every dependency version/coordinate goes in `gradle/libs.versions.toml`, never a raw string in a `build.gradle.kts`.
- No hardcoded `dp`/`sp` in layouts — use `@dimen/_<n>sdp`/`_<n>ssp` (project convention, see `docs/STANDARD.md`).
- No hardcoded hex colors in layouts/code outside launcher assets — read theme colors via `MaterialColors.getColor(view, attrResId)` at runtime, same as the design system already does.
- Kotlin: avoid `!!`; prefer `requireNotNull`/safe calls; keep `CancellationException` propagating (no blanket `catch (e: Exception)`).
- Instrumented tests in this repo run against the real app + real Hilt graph (no fakes/mocks injected) — see `EncryptedSecureStoreTest.kt`, `MainActivityTest.kt`, `LocaleConfigurationContractTest.kt`. Any test that mutates global state (like the app locale) must reset it in `@After`.
- Run `./gradlew :app:compileDebugKotlin` after adding new Kotlin files, `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=<FQCN>` to run a single instrumented test class, `./gradlew check` before the final commit of Task 2.

---

## Task 1: Add the generic `TransitionActivity` infrastructure (additive, nothing existing changes)

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/example/androidxmlbase/core/ui/transition/TransitionAction.kt`
- Create: `app/src/main/java/com/example/androidxmlbase/core/ui/transition/LanguageTransitionAction.kt`
- Create: `app/src/main/java/com/example/androidxmlbase/core/di/TransitionModule.kt`
- Create: `app/src/main/res/raw/loading_pulse.json`
- Create: `app/src/main/res/layout/activity_transition.xml`
- Create: `app/src/main/java/com/example/androidxmlbase/core/ui/base/TransitionActivity.kt`
- Modify: `app/src/main/res/values/themes.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Test: `app/src/androidTest/java/com/example/androidxmlbase/core/ui/base/TransitionActivityTest.kt`

**Interfaces:**
- Consumes: `LocaleManager` (`core/localization/LocaleManager.kt`, `fun setLanguage(language: AppLanguage)`, `fun useSystemLanguage()`, `fun currentLanguage(): AppLanguage?`), `AppLanguage` (`core/localization/AppLanguage.kt`, `findByLanguageTag(tag: String): AppLanguage?`, `.languageTag: String`), `Bundle.getTyped<T>(key: String, clazz: Class<T>): T?` (`core/ui/util/ArgumentDelegates.kt`).
- Produces: `TransitionAction` (fun interface, `suspend fun perform(extras: Bundle)`), `LanguageTransitionAction.KEY: String`, `LanguageTransitionAction.EXTRA_LANGUAGE_TAG: String`, `TransitionActivity.createIntent(context: Context, actionKey: String, extras: Bundle = Bundle.EMPTY): Intent` — all consumed by Task 2.

- [ ] **Step 1: Add the Lottie dependency to the version catalog**

In `gradle/libs.versions.toml`, add the version next to `shimmer` (line 28):

```toml
shimmer = "0.5.0"
lottie = "6.7.1"
```

And the library entry next to `shimmer` in `[libraries]` (line 79):

```toml
shimmer = { group = "com.facebook.shimmer", name = "shimmer", version.ref = "shimmer" }
lottie = { group = "com.airbnb.android", name = "lottie", version.ref = "lottie" }
```

- [ ] **Step 2: Add the Lottie implementation dependency**

In `app/build.gradle.kts`, add next to `implementation(libs.shimmer)` (line 88):

```kotlin
implementation(libs.shimmer)
implementation(libs.lottie)
```

- [ ] **Step 3: Write the failing instrumented test**

```kotlin
// app/src/androidTest/java/com/example/androidxmlbase/core/ui/base/TransitionActivityTest.kt
package com.example.androidxmlbase.core.ui.base

import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.androidxmlbase.core.localization.AppLanguage
import com.example.androidxmlbase.core.localization.LocaleManager
import com.example.androidxmlbase.core.ui.transition.LanguageTransitionAction
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransitionActivityTest {
    private val localeManager = LocaleManager()

    @get:Rule
    val activityRule =
        ActivityScenarioRule<TransitionActivity>(
            TransitionActivity.createIntent(
                context = InstrumentationRegistry.getInstrumentation().targetContext,
                actionKey = LanguageTransitionAction.KEY,
                extras = bundleOf(LanguageTransitionAction.EXTRA_LANGUAGE_TAG to AppLanguage.VIETNAMESE.languageTag),
            ),
        )

    @Before
    fun setUp() {
        localeManager.useSystemLanguage()
    }

    @After
    fun tearDown() {
        localeManager.useSystemLanguage()
    }

    @Test
    fun transitionActivity_runsActionThenFinishes() {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (activityRule.scenario.state != Lifecycle.State.DESTROYED && System.currentTimeMillis() < deadline) {
            Thread.sleep(POLL_INTERVAL_MS)
        }

        assertEquals(Lifecycle.State.DESTROYED, activityRule.scenario.state)
        assertEquals(AppLanguage.VIETNAMESE, localeManager.currentLanguage())
    }

    private companion object {
        const val TIMEOUT_MS = 3_000L
        const val POLL_INTERVAL_MS = 50L
    }
}
```

- [ ] **Step 4: Run the test to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.androidxmlbase.core.ui.base.TransitionActivityTest`
Expected: FAIL — compile error, `TransitionActivity` and `LanguageTransitionAction` don't exist yet.

- [ ] **Step 5: Create the `TransitionAction` interface**

```kotlin
// app/src/main/java/com/example/androidxmlbase/core/ui/transition/TransitionAction.kt
package com.example.androidxmlbase.core.ui.transition

import android.os.Bundle

/**
 * A single unit of async work run by [com.example.androidxmlbase.core.ui.base.TransitionActivity]
 * while it covers the screen. Register implementations via a Hilt `@IntoMap` binding keyed by a
 * unique action key so callers can request them by that key without a new Activity subclass.
 */
fun interface TransitionAction {
    suspend fun perform(extras: Bundle)
}
```

- [ ] **Step 6: Create `LanguageTransitionAction`**

```kotlin
// app/src/main/java/com/example/androidxmlbase/core/ui/transition/LanguageTransitionAction.kt
package com.example.androidxmlbase.core.ui.transition

import android.os.Bundle
import com.example.androidxmlbase.core.localization.AppLanguage
import com.example.androidxmlbase.core.localization.LocaleManager
import com.example.androidxmlbase.core.ui.util.getTyped
import javax.inject.Inject

class LanguageTransitionAction
    @Inject
    constructor(
        private val localeManager: LocaleManager,
    ) : TransitionAction {
        override suspend fun perform(extras: Bundle) {
            val tag = extras.getTyped(EXTRA_LANGUAGE_TAG, String::class.java)
            AppLanguage.findByLanguageTag(tag.orEmpty())?.let(localeManager::setLanguage)
                ?: localeManager.useSystemLanguage()
        }

        companion object {
            const val KEY = "language"
            const val EXTRA_LANGUAGE_TAG = "extra_language_tag"
        }
    }
```

- [ ] **Step 7: Create the Hilt multibinding module**

```kotlin
// app/src/main/java/com/example/androidxmlbase/core/di/TransitionModule.kt
package com.example.androidxmlbase.core.di

import com.example.androidxmlbase.core.ui.transition.LanguageTransitionAction
import com.example.androidxmlbase.core.ui.transition.TransitionAction
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

@Module
@InstallIn(SingletonComponent::class)
abstract class TransitionModule {
    @Binds
    @IntoMap
    @StringKey(LanguageTransitionAction.KEY)
    abstract fun bindLanguageTransitionAction(impl: LanguageTransitionAction): TransitionAction
}
```

- [ ] **Step 8: Create the Lottie animation asset**

A brand-neutral, hand-authored 3-dot sequential pulse, 300x100, 60fps, 90-frame (1.5s) seamless loop. The fill color (`#3F51B5`, matching `color_primary`) is a fallback only — `TransitionActivity` overrides it at runtime via `addValueCallback` so it always matches the live theme.

```json
{
  "v": "5.9.0",
  "fr": 60,
  "ip": 0,
  "op": 90,
  "w": 300,
  "h": 100,
  "nm": "loading_pulse",
  "ddd": 0,
  "assets": [],
  "layers": [
    {
      "ddd": 0, "ind": 1, "ty": 4, "nm": "dot1", "sr": 1,
      "ks": {
        "o": { "a": 1, "k": [
          {"t": 0, "s": [30], "i": {"x": [0.667], "y": [1]}, "o": {"x": [0.333], "y": [0]}},
          {"t": 10, "s": [100], "i": {"x": [0.667], "y": [1]}, "o": {"x": [0.333], "y": [0]}},
          {"t": 20, "s": [30]},
          {"t": 90, "s": [30]}
        ]},
        "r": {"a": 0, "k": 0},
        "p": {"a": 0, "k": [60, 50, 0]},
        "a": {"a": 0, "k": [0, 0, 0]},
        "s": {"a": 0, "k": [100, 100, 100]}
      },
      "ao": 0,
      "shapes": [
        {"ty": "el", "nm": "Ellipse", "p": {"a": 0, "k": [0, 0]}, "s": {"a": 0, "k": [40, 40]}},
        {"ty": "fl", "nm": "Fill", "c": {"a": 0, "k": [0.247, 0.318, 0.710, 1]}, "o": {"a": 0, "k": 100}}
      ],
      "ip": 0, "op": 90, "st": 0, "bm": 0
    },
    {
      "ddd": 0, "ind": 2, "ty": 4, "nm": "dot2", "sr": 1,
      "ks": {
        "o": { "a": 1, "k": [
          {"t": 0, "s": [30]},
          {"t": 30, "s": [30], "i": {"x": [0.667], "y": [1]}, "o": {"x": [0.333], "y": [0]}},
          {"t": 40, "s": [100], "i": {"x": [0.667], "y": [1]}, "o": {"x": [0.333], "y": [0]}},
          {"t": 50, "s": [30]},
          {"t": 90, "s": [30]}
        ]},
        "r": {"a": 0, "k": 0},
        "p": {"a": 0, "k": [150, 50, 0]},
        "a": {"a": 0, "k": [0, 0, 0]},
        "s": {"a": 0, "k": [100, 100, 100]}
      },
      "ao": 0,
      "shapes": [
        {"ty": "el", "nm": "Ellipse", "p": {"a": 0, "k": [0, 0]}, "s": {"a": 0, "k": [40, 40]}},
        {"ty": "fl", "nm": "Fill", "c": {"a": 0, "k": [0.247, 0.318, 0.710, 1]}, "o": {"a": 0, "k": 100}}
      ],
      "ip": 0, "op": 90, "st": 0, "bm": 0
    },
    {
      "ddd": 0, "ind": 3, "ty": 4, "nm": "dot3", "sr": 1,
      "ks": {
        "o": { "a": 1, "k": [
          {"t": 0, "s": [30]},
          {"t": 60, "s": [30], "i": {"x": [0.667], "y": [1]}, "o": {"x": [0.333], "y": [0]}},
          {"t": 70, "s": [100], "i": {"x": [0.667], "y": [1]}, "o": {"x": [0.333], "y": [0]}},
          {"t": 80, "s": [30]},
          {"t": 90, "s": [30]}
        ]},
        "r": {"a": 0, "k": 0},
        "p": {"a": 0, "k": [240, 50, 0]},
        "a": {"a": 0, "k": [0, 0, 0]},
        "s": {"a": 0, "k": [100, 100, 100]}
      },
      "ao": 0,
      "shapes": [
        {"ty": "el", "nm": "Ellipse", "p": {"a": 0, "k": [0, 0]}, "s": {"a": 0, "k": [40, 40]}},
        {"ty": "fl", "nm": "Fill", "c": {"a": 0, "k": [0.247, 0.318, 0.710, 1]}, "o": {"a": 0, "k": 100}}
      ],
      "ip": 0, "op": 90, "st": 0, "bm": 0
    }
  ],
  "markers": []
}
```

Save as `app/src/main/res/raw/loading_pulse.json`.

- [ ] **Step 9: Create the layout**

```xml
<?xml version="1.0" encoding="utf-8"?>
<merge xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">

    <com.airbnb.lottie.LottieAnimationView
        android:id="@+id/transitionLoader"
        android:layout_width="@dimen/_96sdp"
        android:layout_height="@dimen/_96sdp"
        android:layout_gravity="center"
        app:lottie_autoPlay="true"
        app:lottie_loop="true"
        app:lottie_rawRes="@raw/loading_pulse" />

</merge>
```

Save as `app/src/main/res/layout/activity_transition.xml`. (`<merge>` root: this is set via `Activity.setContentView`, which inflates into the window's existing content `FrameLayout` — same pattern already used by `activity_language_transition.xml`.)

- [ ] **Step 10: Create `TransitionActivity`**

```kotlin
// app/src/main/java/com/example/androidxmlbase/core/ui/base/TransitionActivity.kt
package com.example.androidxmlbase.core.ui.base

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.example.androidxmlbase.R
import com.example.androidxmlbase.core.ui.transition.TransitionAction
import com.google.android.material.color.MaterialColors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Opaque full-screen host for a single [TransitionAction]: shows a theme-aware loading
 * animation, runs the action registered under the caller-supplied action key, then finishes.
 * One Activity, one manifest entry — a new transition use case only needs a new
 * [TransitionAction] implementation registered into the Hilt multibinding map, not a new
 * Activity subclass or manifest entry.
 */
@AndroidEntryPoint
class TransitionActivity : ComponentActivity() {
    @Inject
    lateinit var actions: Map<String, @JvmSuppressWildcards TransitionAction>

    private var actionHasCompleted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transition)
        tintLoaderToTheme()

        if (savedInstanceState?.getBoolean(STATE_ACTION_COMPLETED) == true) {
            actionHasCompleted = true
            finishAfterSettleDelay()
        } else {
            runActionAfterTransitionIsOpaque()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_ACTION_COMPLETED, actionHasCompleted)
        super.onSaveInstanceState(outState)
    }

    private fun tintLoaderToTheme() {
        val loader = findViewById<LottieAnimationView>(R.id.transitionLoader)
        val primaryColor = MaterialColors.getColor(loader, com.google.android.material.R.attr.colorPrimary)
        loader.addValueCallback(KeyPath("**"), LottieProperty.COLOR) { primaryColor }
    }

    private fun runActionAfterTransitionIsOpaque() {
        lifecycleScope.launch {
            delay(TRANSITION_ENTER_DURATION_MS.milliseconds)
            val actionKey = requireNotNull(intent.getStringExtra(EXTRA_ACTION_KEY)) { "Missing $EXTRA_ACTION_KEY extra" }
            actions.getValue(actionKey).perform(intent.extras ?: Bundle.EMPTY)
            actionHasCompleted = true
            finishAfterSettleDelay()
        }
    }

    private fun finishAfterSettleDelay() {
        lifecycleScope.launch {
            delay(TRANSITION_SETTLE_DURATION_MS.milliseconds)
            finish()
        }
    }

    companion object {
        private const val EXTRA_ACTION_KEY = "extra_action_key"
        private const val STATE_ACTION_COMPLETED = "state_action_completed"
        private const val TRANSITION_ENTER_DURATION_MS = 320L
        private const val TRANSITION_SETTLE_DURATION_MS = 80L

        fun createIntent(
            context: Context,
            actionKey: String,
            extras: Bundle = Bundle.EMPTY,
        ): Intent =
            Intent(context, TransitionActivity::class.java)
                .putExtra(EXTRA_ACTION_KEY, actionKey)
                .putExtras(extras)
    }
}
```

- [ ] **Step 11: Add the new theme, animation style, and manifest entry (additive — do not touch the existing `LanguageTransition` entries yet)**

In `app/src/main/res/values/themes.xml`, add after the existing `Theme.AndroidXmlBase.LanguageTransition` block:

```xml
<style name="Theme.AndroidXmlBase.Transition" parent="Theme.AndroidXmlBase">
    <item name="android:windowAnimationStyle">@style/Animation_AndroidXmlBase_Transition</item>
    <item name="android:windowIsTranslucent">false</item>
</style>
```

And after the existing `Animation_AndroidXmlBase_LanguageTransition` block:

```xml
<style name="Animation_AndroidXmlBase_Transition">
    <item name="android:activityOpenEnterAnimation">@anim/fade_in</item>
    <item name="android:activityOpenExitAnimation">@anim/no_anim</item>
    <item name="android:activityCloseEnterAnimation">@anim/no_anim</item>
    <item name="android:activityCloseExitAnimation">@anim/fade_out</item>
</style>
```

In `app/src/main/AndroidManifest.xml`, add a new `<activity>` entry after the existing `.LanguageTransitionActivity` entry:

```xml
<activity
    android:name=".core.ui.base.TransitionActivity"
    android:configChanges="locale|layoutDirection"
    android:exported="false"
    android:theme="@style/Theme.AndroidXmlBase.Transition" />
```

- [ ] **Step 12: Run the test to verify it passes**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.androidxmlbase.core.ui.base.TransitionActivityTest`
Expected: PASS (1 test, `transitionActivity_runsActionThenFinishes`).

- [ ] **Step 13: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts \
        app/src/main/java/com/example/androidxmlbase/core/ui/transition/ \
        app/src/main/java/com/example/androidxmlbase/core/di/TransitionModule.kt \
        app/src/main/java/com/example/androidxmlbase/core/ui/base/TransitionActivity.kt \
        app/src/main/res/raw/loading_pulse.json \
        app/src/main/res/layout/activity_transition.xml \
        app/src/main/res/values/themes.xml \
        app/src/main/AndroidManifest.xml \
        app/src/androidTest/java/com/example/androidxmlbase/core/ui/base/TransitionActivityTest.kt
git commit -m "feat: add generic TransitionActivity with Hilt action registry"
```

---

## Task 2: Migrate `LanguageTransitionActivity` off, delete it and its now-unused resources

**Files:**
- Modify: `app/src/main/java/com/example/androidxmlbase/MainActivity.kt`
- Modify: `app/src/androidTest/java/com/example/androidxmlbase/core/localization/LocaleConfigurationContractTest.kt`
- Delete: `app/src/main/java/com/example/androidxmlbase/LanguageTransitionActivity.kt`
- Delete: `app/src/main/res/layout/activity_language_transition.xml`
- Modify: `app/src/main/AndroidManifest.xml` (remove the old `.LanguageTransitionActivity` entry)
- Modify: `app/src/main/res/values/themes.xml` (remove the old `LanguageTransition` theme + animation-style blocks)
- Modify: `app/src/main/res/values/strings.xml` and `app/src/main/res/values-vi/strings.xml` (remove `language_change_loading`)

**Interfaces:**
- Consumes: `TransitionActivity.createIntent(context, actionKey, extras)`, `LanguageTransitionAction.KEY`, `LanguageTransitionAction.EXTRA_LANGUAGE_TAG` (all from Task 1).
- Produces: nothing further — this is the last task in the plan.

- [ ] **Step 1: Update `MainActivity`'s call site**

In `app/src/main/java/com/example/androidxmlbase/MainActivity.kt`, add imports:

```kotlin
import androidx.core.os.bundleOf
import com.example.androidxmlbase.core.ui.base.TransitionActivity
import com.example.androidxmlbase.core.ui.transition.LanguageTransitionAction
```

Replace:

```kotlin
        isLanguageChangeInProgress = true
        startActivity(LanguageTransitionActivity.createIntent(this, language))
```

with:

```kotlin
        isLanguageChangeInProgress = true
        startActivity(
            TransitionActivity.createIntent(
                context = this,
                actionKey = LanguageTransitionAction.KEY,
                extras = bundleOf(LanguageTransitionAction.EXTRA_LANGUAGE_TAG to language?.languageTag.orEmpty()),
            ),
        )
```

- [ ] **Step 2: Update `LocaleConfigurationContractTest`**

In `app/src/androidTest/java/com/example/androidxmlbase/core/localization/LocaleConfigurationContractTest.kt`, replace the import:

```kotlin
import com.example.androidxmlbase.LanguageTransitionActivity
```

with:

```kotlin
import com.example.androidxmlbase.core.ui.base.TransitionActivity
```

Rename the test and update the `ComponentName` reference:

```kotlin
    @Test
    fun transitionActivity_keepsItsOpaqueWindowAcrossLocaleRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val activityInfo =
            context.packageManager.getActivityInfo(
                ComponentName(context, TransitionActivity::class.java),
                0,
            )
        val requiredConfigChanges = ActivityInfo.CONFIG_LOCALE or ActivityInfo.CONFIG_LAYOUT_DIRECTION

        assertEquals(requiredConfigChanges, activityInfo.configChanges and requiredConfigChanges)
    }
```

- [ ] **Step 3: Delete the old Activity and its layout**

```bash
git rm app/src/main/java/com/example/androidxmlbase/LanguageTransitionActivity.kt
git rm app/src/main/res/layout/activity_language_transition.xml
```

- [ ] **Step 4: Remove the old manifest entry**

In `app/src/main/AndroidManifest.xml`, delete the `.LanguageTransitionActivity` `<activity>` block (the new `.core.ui.base.TransitionActivity` entry from Task 1 stays).

- [ ] **Step 5: Remove the old theme and animation-style blocks**

In `app/src/main/res/values/themes.xml`, delete the `Theme.AndroidXmlBase.LanguageTransition` style block and the `Animation_AndroidXmlBase_LanguageTransition` style block (the new `.Transition`-named ones from Task 1 stay).

- [ ] **Step 6: Remove the unused string resource**

Delete the `language_change_loading` line from both `app/src/main/res/values/strings.xml` and `app/src/main/res/values-vi/strings.xml`.

- [ ] **Step 7: Run the instrumented tests**

Run: `./gradlew :app:connectedDebugAndroidTest`
Expected: all instrumented tests pass, including `TransitionActivityTest`, the renamed `LocaleConfigurationContractTest.transitionActivity_keepsItsOpaqueWindowAcrossLocaleRecreation`, and the untouched `MainActivityTest`.

- [ ] **Step 8: Run the full quality gate**

Run: `./gradlew check`
Expected: BUILD SUCCESSFUL — unit tests, Kover coverage, ktlint, detekt, and lint all pass (no leftover reference to the deleted class/resources).

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/example/androidxmlbase/MainActivity.kt \
        app/src/androidTest/java/com/example/androidxmlbase/core/localization/LocaleConfigurationContractTest.kt \
        app/src/main/AndroidManifest.xml \
        app/src/main/res/values/themes.xml \
        app/src/main/res/values/strings.xml \
        app/src/main/res/values-vi/strings.xml
git rm app/src/main/java/com/example/androidxmlbase/LanguageTransitionActivity.kt
git rm app/src/main/res/layout/activity_language_transition.xml
git commit -m "refactor: migrate language change to TransitionActivity, drop LanguageTransitionActivity"
```
