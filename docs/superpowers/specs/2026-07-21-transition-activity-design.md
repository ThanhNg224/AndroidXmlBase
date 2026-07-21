# Generic `TransitionActivity` Design

**Date:** 2026-07-21
**Status:** Approved by user, pending implementation plan.

## Problem

`LanguageTransitionActivity` covers the screen with a full-screen loader while
`LocaleManager.setLanguage(...)` runs and the rest of the app recreates itself
in the target locale. Today it shows a text status
(`R.string.language_change_loading`) resolved from `Resources` **at the
moment `onCreate` runs** — i.e. still in the *old* locale — and the locale
switch itself only happens after a delay inside
`applyLanguageAfterTransitionIsOpaque()`. The activity finishes before the new
locale would ever be reflected in that text, so the loading text is always
visibly wrong when switching (e.g. still English while transitioning to
Vietnamese).

`LanguageTransitionActivity` is also single-purpose despite being, in
substance, generic infrastructure: "cover the screen opaquely, run one piece
of async work, then get out of the way." Base-building phase infra is exempt
from strict YAGNI (`CLAUDE.md` § Current Phase), and "cover the screen during
an async operation that also recreates the app" (theme switch, logout,
account switch, pre-launch sync, ...) is infrastructure every app built on
this base is likely to need again.

## Goals

1. Eliminate the locale-mismatch bug at the root: don't show any
   locale-dependent text at all during the transition.
2. Turn the mechanism into reusable `core/` infrastructure: adding a new
   "opaque transition" use case in the future costs one small class, not a
   new `Activity` subclass + a new `<activity>` manifest entry.
3. Modernize the visual: replace the text status with a brand-neutral,
   theme-aware Lottie loading animation.

## Non-goals

- No action registry beyond a static Hilt multibinding map — no remote
  config, no dynamically loaded/pluggable actions, no queueing of multiple
  pending transitions.
- No change to the Activity window enter/exit animations
  (`anim/fade_in.xml` / `anim/fade_out.xml`, referenced by
  `Animation_AndroidXmlBase_LanguageTransition`) — orthogonal to this work.
- Not introducing Lottie as a general illustration system for the rest of the
  app — scoped to this one core component for now.
- Not changing `LocaleManager`, `AppLanguage`, or the manifest's
  `configChanges="locale|layoutDirection"` handling.

## Architecture

Single concrete `TransitionActivity`, one `<activity>` manifest entry, ever.
Each use case is a small `TransitionAction` implementation registered into a
Hilt multibinding map keyed by a string action key — no new Activity
subclass, no new manifest entry, when a future use case is added.

```kotlin
// core/ui/transition/TransitionAction.kt
fun interface TransitionAction {
    suspend fun perform(extras: Bundle)
}

// core/ui/transition/LanguageTransitionAction.kt
class LanguageTransitionAction @Inject constructor(
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

// core/di/TransitionModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class TransitionModule {
    @Binds
    @IntoMap
    @StringKey(LanguageTransitionAction.KEY)
    abstract fun bindLanguageTransitionAction(impl: LanguageTransitionAction): TransitionAction
}

// core/ui/base/TransitionActivity.kt
@AndroidEntryPoint
class TransitionActivity : ComponentActivity() {
    @Inject lateinit var actions: Map<String, @JvmSuppressWildcards TransitionAction>

    // onCreate: setContentView(loader layout: opaque background + centered
    // LottieAnimationView, no text) -> start the loop animation ->
    // lifecycleScope.launch {
    //     minDwell(ENTER_DURATION)
    //     actions.getValue(actionKey).perform(intent.extras ?: Bundle.EMPTY)
    //     minDwell(SETTLE_DURATION)
    //     finish()
    // }
    // onSaveInstanceState: same "work already completed" boolean flag as
    // today, generalized (covers process death mid-transition).

    companion object {
        private const val EXTRA_ACTION_KEY = "extra_action_key"

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

Callers build their own extras `Bundle` and pass their action's `KEY`:

```kotlin
startActivity(
    TransitionActivity.createIntent(
        context = this,
        actionKey = LanguageTransitionAction.KEY,
        extras = bundleOf(LanguageTransitionAction.EXTRA_LANGUAGE_TAG to language?.languageTag.orEmpty()),
    ),
)
```

`LanguageTransitionActivity.kt` is deleted; its logic moves into
`LanguageTransitionAction`. `MainActivity.requestLanguageChange` is updated to
call `TransitionActivity.createIntent(..., LanguageTransitionAction.KEY, ...)`
instead. The manifest's `.LanguageTransitionActivity` entry
(`configChanges="locale|layoutDirection"`, `Theme.AndroidXmlBase.LanguageTransition`)
is replaced by a single `.core.ui.base.TransitionActivity` entry keeping the
same `configChanges` and a renamed `Theme.AndroidXmlBase.Transition` theme
(same window animation style, generic name).

`Bundle.getTyped` (already modernized in the deprecation cleanup) is reused
as-is for reading action extras type-safely — no new bundle-parsing code
needed.

## Visual asset

A hand-authored `res/raw/loading_pulse.json` Lottie animation: three dots
pulsing in sequence, ~1.2s loop, no hardcoded brand colors — colors are set
at runtime via `LottieAnimationView.addValueCallback` reading the current
theme's `colorPrimary`/`colorOnSurface`, so day/night and future re-theming
work with no asset changes. Hand-authoring (rather than downloading a
third-party Lottie asset) avoids taking on unclear redistribution/attribution
terms in a template repo meant to be forked repeatedly.

New dependency: `com.airbnb.android:lottie` (added to
`gradle/libs.versions.toml`, current stable version resolved at
implementation time).

The existing `view_full_screen_loader.xml` / `FullScreenLoaderView` (used by
`ResultStateOverlay` for in-screen loading states) is untouched — it's a
different use case (loading indicator *within* an existing screen) from this
opaque cross-screen transition cover, and keeps its text label since that
text is always resolved in the already-current locale.

## Data flow

1. Caller builds an `Intent` via `TransitionActivity.createIntent(context, actionKey, extras)`.
2. `TransitionActivity.onCreate` shows the opaque Lottie loader immediately (blocks input, no recreate on locale/layoutDirection changes).
3. `TransitionActivity` looks up the registered `TransitionAction` for `actionKey` and calls `perform(extras)`.
4. A minimum visible duration is enforced on both the "enter" and "settle" ends (same two-constant timing concept as today) so fast transitions don't flicker and slow ones don't feel abrupt.
5. `finish()`.

## Error handling

`TransitionAction.perform` is `suspend` and unguarded at the `TransitionActivity` level — if it throws, the exception propagates (matches today's behavior; nothing is silently swallowed). `LanguageTransitionAction.perform` can't realistically fail. A future action that can fail is responsible for its own fallback/recovery before returning; `TransitionActivity` stays a thin, action-agnostic host and doesn't grow per-action error UI.

An unknown `actionKey` (e.g. a stale `PendingIntent` from before an action was renamed) is a programmer error: `actions.getValue(actionKey)` throws `NoSuchElementException` rather than silently no-op'ing.

## Testing

- `LocaleConfigurationContractTest` (existing instrumented test) is unchanged — it tests manifest config-changes and the locale registry, not this class.
- New/updated instrumented test(s) for `TransitionActivity`, driven through `LanguageTransitionAction` as the concrete example: verifies the activity finishes after the action completes, respects the minimum dwell duration, and that `LocaleManager`'s target locale is applied by the time the activity finishes.
- Exact test list is finalized during implementation planning.
