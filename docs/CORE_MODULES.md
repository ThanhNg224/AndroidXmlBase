# CORE_MODULES.md

One section per `core/*` package that actually exists in this codebase today (verified against `app/src/main/java/com/example/androidxmlbase/core/` directly, not reconstructed from earlier phase plans). Each section lists the real public API surface and which feature(s) currently consume it. If a class/file isn't listed here, it doesn't exist yet — don't assume it does.

## `core/architecture`

The MVVM primitives every feature is built on. Framework-light: only `StateViewModel` depends on `androidx.lifecycle`.

- `UiState` — empty marker interface. Feature state classes implement it (e.g. `DemoUiState`).
- `UiEvent` — empty marker interface. Feature event sealed interfaces implement it (e.g. `DemoUiEvent`).
- `UiEffect` — empty marker interface for one-shot effects (e.g. `DemoUiEffect`). `DesignSystemViewModel` uses the bare `UiEffect` interface directly (no dedicated effect type) since it never emits one.
- `ResultState<out T>` (sealed interface) — `Loading`, `Success<T>(val data: T)`, `Error(val message: String, val cause: Throwable? = null)`. Plus `inline fun <T, R> ResultState<T>.fold(onLoading, onSuccess, onError): R`.
- `DomainResult<out T>` (sealed interface) — `Success<T>(data)` and `Error(error: AppError)` for domain/data results that should not carry UI strings.
- `AppError` (sealed interface) — reusable error categories: `Http(code, serverMessage)`, `Network(cause)`, `Parse(cause)`, and `EmptyBody`.
- `AppDispatchers` (`main`/`io`/`default` `CoroutineDispatcher`s) + `DefaultAppDispatchers` implementation. Bound in Hilt and used by blocking/IO-heavy adapters such as `EncryptedSecureStore` and `OkHttpFileTransferClient`.
- `UseCase<in P, R>` — `suspend operator fun invoke(params: P): R`. Implementers: `SaveDemoCountUseCase`, `FetchDemoMessageUseCase`. See `docs/FEATURE_TEMPLATE.md` section 4 for when to implement it vs. stay a plain class.
- `StateViewModel<S : UiState, E : UiEvent, F : UiEffect>(initialState: S)` (abstract, extends `ViewModel`) — exposes `state: StateFlow<S>`, `effect: Flow<F>` (buffered `Channel`-backed), `protected val currentState: S`, `abstract fun onEvent(event: E)`, `protected fun setState(reducer: S.() -> S)` (implemented via `MutableStateFlow.update {}`, atomic under concurrent calls), `protected fun sendEffect(effect: F)`.

**Consumers:** `DemoViewModel`, `DesignSystemViewModel` (both extend `StateViewModel`); `DomainResult`/`AppError` are used by `feature/demo`'s repository/use case path so data/domain can report failure without UI strings; `ResultState` is used by `feature/designsystem` (`DesignSystemUiState.demoResult`) and by `core/ui/base` render helpers. The `fold` extension specifically has exactly one real call site, `DesignSystemActivity.render()` (for picking display text). `UseCase<in P, R>` is implemented by `feature/demo`'s `SaveDemoCountUseCase`/`FetchDemoMessageUseCase`.

## `core/storage`

A typed, testable settings store backed by Jetpack DataStore (`androidx.datastore:datastore-preferences`).

- `SettingsKey<T>` (sealed class, `name`/`defaultValue`) with 5 typed subclasses: `StringKey`, `IntKey`, `LongKey`, `BooleanKey`, `FloatKey`.
- `SettingsStore` (interface) — `fun <T> observe(key): Flow<T>`, `suspend fun <T> get(key): T`, `suspend fun <T> set(key, value)`, `suspend fun <T> remove(key)`.
- `DataStoreSettingsStore(dataStore: DataStore<Preferences>)` — the only implementation. Takes a `DataStore<Preferences>` directly (never a `Context`) so it stays unit-testable on the JVM.
- `Context.appSettingsDataStore` — the `preferencesDataStore(name = "app_settings")` delegate; the one place a `Context` is involved, kept out of the testable class.
- `AppSettingsKeys` — exactly 4 app-wide keys: `THEME_MODE` (String, default `"system"`), `FIRST_OPEN_AT` (Long, default `0L`), `OPEN_COUNT` (Int, default `0`), `DEBUG_LOGGING_ENABLED` (Boolean, default `false`).
- `SecureStoreKey`, `SecureStore`, `SecureStoreKeys` — string-secret storage contract for tokens/secrets. Built-in keys: `AUTH_TOKEN`, `REFRESH_TOKEN`.
- `EncryptedSecureStore` — AndroidX Security-backed implementation using encrypted SharedPreferences behind the `SecureStore` interface. It is provided as the app-wide `SecureStore` by Hilt.

**Deferred, and why:** No SharedPreferences-to-DataStore migration helper exists. It remains optional for apps migrating from an existing legacy project; brand-new apps on this base should start with `SettingsStore`/`SecureStore` directly.

**Feature-specific keys do not belong in `AppSettingsKeys`.** `feature/demo`'s counter key (`demo_counter_count`, an `IntKey`) is a private constant inside `DemoRepositoryImpl`, not in this object — see `docs/FEATURE_TEMPLATE.md` anti-pattern 2.

**Consumers:** `DemoRepositoryImpl` (feature-private counter key + `SettingsStore`); `SecureStoreAuthTokenProvider` (reads `SecureStoreKeys.AUTH_TOKEN`). `THEME_MODE`, `FIRST_OPEN_AT`, `OPEN_COUNT`, `DEBUG_LOGGING_ENABLED` have no consumer yet — reserved for a future theming/analytics/logging core.

## `core/network`

A Retrofit/OkHttp facade that classifies every call into a uniform `ApiResult<T>`.

- `ApiResult<out T>` (sealed interface) — `Success<T>(data)`, `HttpError(code, message)`, `NetworkError(cause)`, `ParseError(cause)`, `EmptyBody`.
- `ApiConfig(baseUrl: String, enableLogging: Boolean)`.
- `AuthTokenProvider` (interface, `suspend fun getToken(): String?`) + `SecureStoreAuthTokenProvider` (reads `SecureStoreKeys.AUTH_TOKEN`) + `NoOpAuthTokenProvider` for tests/demo overrides.
- `ConnectivityChecker` (interface, `fun isConnected(): Boolean`) + `AndroidConnectivityChecker(context)` (real impl via `ConnectivityManager`).
- `ApiClient` (interface) — `suspend fun <T> execute(call: suspend () -> retrofit2.Response<T>): ApiResult<T>`.
- `RetrofitApiClient` — the `ApiClient` implementation; classifies success/HTTP error/empty body, catches `IOException` as `NetworkError`, any other `Exception` as `ParseError`, and always rethrows `CancellationException` before those catches.
- `NetworkModule` (object) — reusable factory functions for `OkHttpClient` and `Retrofit`; Hilt calls these from `core/di/NetworkDiModule`.
- `interceptor/AuthTokenInterceptor` — adds `Authorization: Bearer <token>` via `runBlocking { authTokenProvider.getToken() }` when a token is present.
- `interceptor/ConnectivityInterceptor` — throws `NoConnectivityException` (an `IOException`) before any request leaves the device if `ConnectivityChecker.isConnected()` is false.
- `FileTransferClient` + `OkHttpFileTransferClient` — download, upload, and streaming support over OkHttp `Request`.
- `TransferResult<T>` — `Progress`, `Success<T>`, `Failure`; transfer-specific aliases: `DownloadResult`, `UploadResult`, `StreamResult`.
- `HttpTransferResponse`, `StreamChunk`, `ProgressRequestBody` — upload/stream/download support types.

Serializer is kotlinx.serialization, not Gson, by explicit decision (no reflection, already idiomatic-Kotlin codebase) — do not introduce Gson alongside it.

**Consumers:** `feature/demo`'s `DemoApiService`/`DemoRemoteDataSourceImpl`; `DemoApiService` is provided by `feature/demo/di/DemoNetworkModule`, while `core/di/NetworkDiModule` only provides the reusable Retrofit/OkHttp stack. `ApiConfig` reads `BuildConfig.API_BASE_URL` and `BuildConfig.API_ENABLE_LOGGING`; replace those build config fields before shipping a real product on this base. `SecureStoreAuthTokenProvider` is the default auth provider; write `SecureStoreKeys.AUTH_TOKEN` to attach a bearer token.

## `core/di`

Hilt modules for app-wide wiring.

- `AppCoreBindingsModule` — binds `DefaultAppDispatchers` to `AppDispatchers` and `EncryptedSecureStore` to `SecureStore`.
- `AppCoreModule` — provides `SettingsStore` and `LocaleManager`.
- `NetworkBindingsModule` — binds `RetrofitApiClient`, `SecureStoreAuthTokenProvider`, and `OkHttpFileTransferClient`.
- `NetworkDiModule` — provides `ApiConfig`, `ConnectivityChecker`, `OkHttpClient`, and `Retrofit`. Feature-specific Retrofit services belong in that feature's own DI module.

Concrete Activities use `@AndroidEntryPoint`; ViewModels use `@HiltViewModel`; feature interface bindings live beside the feature, e.g. `feature/demo/di/DemoModule`.

## `core/localization`

Per-app language switching, backed by AndroidX's per-app language API (`AppCompatDelegate.setApplicationLocales`). The manifest declares `android:localeConfig="@xml/locales_config"` for the supported locales and opts into AppCompat `autoStoreLocales`, so the base no longer stores language in `SettingsStore` or wraps every Activity context manually.

- `LanguageOption(code: String, displayName: String)` + `val SUPPORTED_LANGUAGES = listOf(LanguageOption("en", "English"), LanguageOption("vi", "Tiếng Việt"))` — forward-looking sample data with **no real consumer yet**: `MainActivity`'s two EN/VI buttons call `localeManager.setLanguage("en")`/`setLanguage("vi")` with raw string literals, not `SUPPORTED_LANGUAGES`. Wire an actual dynamic selector through this list (instead of hand-adding more hardcoded buttons) the next time a 3rd language is needed.
- `LocaleTagMapper` (object) — `toRegionalTag(languageCode): String`, mapping `vi`→`vi-VN`, `ko`→`ko-KR`, `zh-TW`→`zh-TW` (passthrough otherwise).
- `AppLocaleApplier` (interface, `fun applyLocales(tag: String)`) + `AppCompatLocaleApplier` (real impl, calls `AppCompatDelegate.setApplicationLocales`) — injected as an interface specifically so `LocaleManager` stays JVM-unit-testable (no Robolectric in this project; calling the real AndroidX API directly from a unit test would crash).
- `LocaleManager(localeApplier = AppCompatLocaleApplier())` — `suspend fun setLanguage(languageCode: String)`, maps through `LocaleTagMapper`, then applies the resulting tag (or blank passthrough for "system default").

**Consumers:** `MainActivity`'s EN/VI buttons drive `LocaleManager.setLanguage(...)` directly.

## `core/ui/responsive`

A `smallestScreenWidthDp` clamp to avoid a tablet/wide-screen resource explosion.

- `ResponsiveConfig(enabled: Boolean = true, minSmallestScreenWidthDp: Int = 320, maxSmallestScreenWidthDp: Int = 480)`.
- `ResponsiveContextWrapper` (object) — `wrap(context, config): Context`, clamps `Configuration.smallestScreenWidthDp` into `[min, max]` via `createConfigurationContext`; returns the context unchanged if `config.enabled` is false.

**Consumers:** `BaseActivity.attachBaseContext`. Every `BaseActivity` subclass can override `protected open val responsiveConfig: ResponsiveConfig` to change the clamp; none currently do (all use the default).

## `core/ui/base`

Shared `Activity` infrastructure, extracted once real duplication existed across `MainActivity`/`DemoActivity`/`DesignSystemActivity` (Phase 6).

- `BaseActivity<VB : ViewBinding>` (abstract, extends `AppCompatActivity`) — owns responsive `attachBaseContext` wrapping and ViewBinding lifecycle. Locale is handled by AppCompat per-app locales, not by custom context wrapping. Subclasses implement `protected abstract fun inflateBinding(inflater: LayoutInflater): VB` and `protected abstract fun onBindingReady(savedInstanceState: Bundle?)`. `onCreate` is intentionally overridable because Hilt needs to subclass Activities for injection; app screens should still use `onBindingReady`.
- `BaseFragment<VB : ViewBinding>` — ViewBinding lifecycle + `onBindingReady(view, savedInstanceState)` + lifecycle-safe `collectOnStarted`.
- `BaseDialogFragment<VB : ViewBinding>` — AlertDialog-backed ViewBinding dialog base.
- `BaseBottomSheetDialogFragment<VB : ViewBinding>` — Material bottom-sheet ViewBinding base.
- `Debouncer(intervalMs: Long = 600L)` — pure, JVM-testable rate limiter: `fun shouldAllow(nowMs: Long): Boolean`. Plus `View.setOnDebouncedClickListener(intervalMs, action)`, the Android-only glue that ignores clicks arriving within `intervalMs` of the last accepted one (uses `System.currentTimeMillis()`, not `SystemClock`, specifically so the pure `Debouncer` core stays testable without Robolectric).
- `ResultRenderState(isLoadingVisible, isContentVisible, isErrorVisible, errorMessage: String?)` — a visibility-only projection of a `ResultState<T>`. Plus `fun <T> ResultState<T>.toRenderState(): ResultRenderState` and the Android-only `fun ResultRenderState.applyVisibilityTo(loadingView, contentView, errorView)`.

**Consumers:** `MainActivity`, `DemoActivity`, `DesignSystemActivity` all extend `BaseActivity`. `DemoActivity`'s increment button (`FrameButton`) uses `setOnDebouncedClickListener` — the one real usage so far, chosen because it's the control most likely to be rapid-tapped. `DesignSystemActivity.render()` consumes `toRenderState()`.

## `core/navigation`

Small Activity-navigation helper layer, intentionally lighter than Jetpack Navigation until the app has fragment graphs.

- `NavigationOptions(clearTask, singleTop, noAnimation)` — pure option model with `toIntentFlags()`.
- `ActivityDestination(activityClass, extras, options)` — typed Activity destination.
- `ActivityNavigator` — `navigate(context, destination)` and `finish(activity)`, injectable by Hilt.
- `BundleCompat` — internal defensive copy helper for extras.

## `core/ui/components`

The custom Views this base ships today.

- `ButtonStyleDelegate(targetView, shape, backgroundColor, cornerRadiusPx, strokeWidthPx = 0f, strokeColor = Color.TRANSPARENT)` — shared shape/ripple background logic. Framework-attribute-agnostic by design: it takes plain resolved values, not an `AttributeSet`, so each concrete button variant parses its own `obtainStyledAttributes` and hands the delegate plain values — this is what keeps the delegate reusable once more variants exist. `apply()` builds the shape drawable via `ShapeUtils` and wraps it in a themed `RippleDrawable` (ripple color resolved from the theme's `colorControlHighlight`, with a hardcoded ~20%-alpha-black fallback if the theme somehow lacks it).
- `FrameButton` (`FrameLayout` subclass) — the **only** concrete `ButtonStyleDelegate` consumer built so far. Custom attrs: `app:buttonBackgroundColor`, `app:buttonCornerRadius`, `app:buttonStrokeWidth`, `app:buttonStrokeColor`, `app:buttonShape` (`rectangle` | `oval`). Sets `isClickable = true`/`isFocusable = true`, exposes button accessibility semantics, and enforces a 48dp minimum touch target.
- `ShadowLayout` (`FrameLayout` subclass) — draws a soft shadow via elevation + a rounded `ViewOutlineProvider`, not a hand-rolled blurred draw. **Requires the caller to set `android:elevation` on the instance** — `ShadowLayout` does not force its own elevation internally. Custom attrs: `app:shadowCornerRadius`, `app:shadowBackgroundColor`.
- `CustomSwitch` (`MaterialSwitch` subclass, `com.google.android.material.materialswitch.MaterialSwitch`) — tints track/thumb from `color_primary`/`color_on_surface`/`color_surface` tokens; no custom attrs, no hand-rolled touch/accessibility handling.
- `CustomToast` (object) — `show(anchorView: View, message: String, duration: Int = Snackbar.LENGTH_SHORT)`. Backed by `Snackbar`, not a custom `Toast` view (`Toast.setView` is deprecated since API 30 and custom toast views are suppressed while backgrounded) — takes a `View` anchor, not a `Context`.

**Deferred, and why:** No `LinearButton`, `CardButton`, `ConstraintButton`, or the reference project's other button-variant shapes (8 more exist there, all `ButtonStyleDelegate` on a different base View). Per the Phase 5 plan: *"Porting all 9 with zero real screens needing more than one shape would be speculative, unused code... Adding `LinearButton`/`CardButton`/etc. is a follow-up whenever a real screen needs that specific shape — do not add them speculatively now."* `FlowLayout` is also deferred, same reasoning (no current consumer).

**Consumers:** `activity_demo.xml` (`FrameButton` for `btnIncrement`); `activity_design_system.xml` (all 5 components — the live showcase, see `docs/DESIGN_SYSTEM.md`).

## `core/ui/util`

- `Shape` (enum: `RECTANGLE`, `OVAL`).
- `ShapeUtils` (object) — `buildDrawable(shape, cornerRadiusPx, fillColor, strokeWidthPx = 0f, strokeColor = Color.TRANSPARENT): GradientDrawable`, shared by every custom View above so none of them hand-roll `GradientDrawable` setup individually. Internal helpers `resolveCornerRadiusPx` (clamps negative radii to 0) and `resolveStrokeWidthPx` (rounds to nearest int, guarantees any positive width renders as at least 1px) are unit-tested; the drawable-building itself needs the Android framework and isn't.

**Consumers:** `ButtonStyleDelegate` and `CustomToast` both call `ShapeUtils.buildDrawable`.

---

No `core/common`, `core/designsystem`, `core/analytics`, or `core/logging` package exists yet. Do not assume any of them exist; check the source tree first.
