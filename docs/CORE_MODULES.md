# CORE_MODULES.md

One section per `core/*` package that actually exists in this codebase today (verified against `app/src/main/java/com/example/androidxmlbase/core/` directly, not reconstructed from earlier phase plans). Each section lists the real public API surface and which feature(s) currently consume it. If a class/file isn't listed here, it doesn't exist yet — don't assume it does.

## `core/architecture`

The MVVM primitives every feature is built on. Framework-light: only `StateViewModel` depends on `androidx.lifecycle`.

- `UiState` — empty marker interface. Feature state classes implement it (e.g. `DemoUiState`).
- `UiEvent` — empty marker interface. Feature event sealed interfaces implement it (e.g. `DemoUiEvent`).
- `UiEffect` — empty marker interface for one-shot effects (e.g. `DemoUiEffect`). `DesignSystemViewModel` uses the bare `UiEffect` interface directly (no dedicated effect type) since it never emits one.
- `AppDispatchers` (`main`/`io`/`default` `CoroutineDispatcher`s) + `DefaultAppDispatchers` implementation. Bound in Hilt and used by blocking/IO-heavy adapters.
- `UseCase<in P, R>` — `suspend operator fun invoke(params: P): R`. Implementers: `SaveDemoCountUseCase`, `FetchDemoMessageUseCase`. See `docs/FEATURE_TEMPLATE.md` section 4 for when to implement it vs. stay a plain class.
- `StateViewModel<S : UiState, E : UiEvent, F : UiEffect>(initialState: S)` (abstract, extends `ViewModel`) — exposes `state: StateFlow<S>`, `effect: Flow<F>` (buffered `Channel`-backed), `protected val currentState: S`, `abstract fun onEvent(event: E)`, `protected fun setState(reducer: S.() -> S)` (implemented via `MutableStateFlow.update {}`, atomic under concurrent calls), `protected fun sendEffect(effect: F)`.

### `core/architecture/result`
- `ResultState<out T>` (sealed interface) — `Loading`, `Success<T>(val data: T)`, `Error(val message: String, val cause: Throwable? = null)`. Plus `inline fun <T, R> ResultState<T>.fold(onLoading, onSuccess, onError): R`.
- `DomainResult<out T>` (sealed interface) — `Success<T>(data)` and `Error(error: AppError)` for domain/data results that should not carry UI strings. Contains `map` extension function to transform Success cases.
- `AppError` (sealed interface) — reusable error categories: `Http(code, serverMessage)`, `Network(cause)`, `Parse(cause)`, `EmptyBody`, and `Business(code, message)`.

**Consumers:** `DemoViewModel`, `DesignSystemViewModel` (both extend `StateViewModel`); `DomainResult`/`AppError` are used by `feature/demo`'s repository/use case path so data/domain can report failure without UI strings; `ResultState` is used by `feature/designsystem` (`DesignSystemUiState.demoResult`) and by `core/ui/base` render helpers. `UseCase<in P, R>` is implemented by `feature/demo`'s `SaveDemoCountUseCase`/`FetchDemoMessageUseCase`.

**Why three result types, not one:** `ApiResult` (`core/network`), `DomainResult`/`AppError` (here), and `ResultState` (here) look similar but belong to different layers on purpose — `ApiResult` carries Retrofit/HTTP-shaped errors and must not leak past data sources; `DomainResult`/`AppError` are the domain-safe, UI-string-free version repositories/use cases return; `ResultState` is presentation-only and is what a ViewModel exposes to a View. Each layer maps the one below into its own type (see `feature/demo`'s mapper) instead of passing the lower type through. Don't collapse these into one shared type — that would leak Retrofit/HTTP types into the domain or UI layer.

## `core/storage`

### `core/storage/settings`
A typed, testable settings store backed by Jetpack DataStore (`androidx.datastore:datastore-preferences`).
- `SettingsKey<T>` (sealed class, `name`/`defaultValue`) with 5 typed subclasses: `StringKey`, `IntKey`, `LongKey`, `BooleanKey`, `FloatKey`.
- `SettingsStore` (interface) — `fun <T> observe(key): Flow<T>`, `suspend fun <T> get(key): T`, `suspend fun <T> set(key, value)`, `suspend fun <T> remove(key)`.
- `DataStoreSettingsStore(dataStore: DataStore<Preferences>)` — the only implementation. Takes a `DataStore<Preferences>` directly (never a `Context`) so it stays unit-testable on the JVM.
- `Context.appSettingsDataStore` — the `preferencesDataStore(name = "app_settings")` delegate; the one place a `Context` is involved, kept out of the testable class.
- `AppSettingsKeys` — exactly 4 app-wide keys: `THEME_MODE` (String, default `"system"`), `FIRST_OPEN_AT` (Long, default `0L`), `OPEN_COUNT` (Int, default `0`), `DEBUG_LOGGING_ENABLED` (Boolean, default `false`).

### `core/storage/secure`
- `SecureStoreKey`, `SecureStore`, `SecureStoreKeys` — string-secret storage contract for tokens/secrets. Built-in keys: `AUTH_TOKEN`, `REFRESH_TOKEN`.
- `EncryptedSecureStore` — AndroidX Security-backed implementation using encrypted SharedPreferences behind the `SecureStore` interface. Provided as the app-wide `SecureStore` by Hilt.

**Consumers:** `DemoRepositoryImpl` (feature-private counter key + `SettingsStore`); `SecureStoreAuthTokenProvider` (reads `SecureStoreKeys.AUTH_TOKEN`).

## `core/network`

- `ApiResult<out T>` (sealed interface) — `Success<T>(data)`, `HttpError(code, message)`, `NetworkError(cause)`, `ParseError(cause)`, `EmptyBody`.
- `ApiConfig(baseUrl: String, enableLogging: Boolean)`.
- `ApiClient` (interface) — `suspend fun <T> execute(call: suspend () -> retrofit2.Response<T>): ApiResult<T>`.
- `RetrofitApiClient` — the `ApiClient` implementation; classifies success/HTTP error/empty body, catches `IOException` as `NetworkError`, any other `Exception` as `ParseError`, and always rethrows `CancellationException` before those catches.
- `NetworkClientFactory` (object) — reusable factory functions for `OkHttpClient` and `Retrofit` with 30-second timeouts configured. Named apart from `core/di/NetworkModule` (the Hilt module) so "factory" vs. "DI wiring" stays unambiguous.

### `core/network/auth`
- `AuthTokenProvider` (interface, `suspend fun getToken(): String?`) + `SecureStoreAuthTokenProvider` (reads `SecureStoreKeys.AUTH_TOKEN`) + `NoOpAuthTokenProvider` for tests/demo overrides.
- `AuthTokenInterceptor` — adds token returned by `AuthTokenProvider.getToken()` directly into `"Authorization"` header if not null/blank.

### `core/network/connectivity`
- `ConnectivityChecker` (interface, `fun isConnected(): Boolean`) + `AndroidConnectivityChecker(context)` (real impl via `ConnectivityManager`).
- `ConnectivityInterceptor` — throws `NoConnectivityException` (an `IOException`) before any request leaves the device if `ConnectivityChecker.isConnected()` is false.

### `core/network/transfer`
- `FileTransferClient` + `OkHttpFileTransferClient` — download, upload, and streaming support over OkHttp `Request`.
- `TransferResult<T>` — `Progress`, `Success<T>`, `Failure`; transfer-specific aliases: `DownloadResult`, `UploadResult`, `StreamResult`.
- `HttpTransferResponse`, `StreamChunk`, `ProgressRequestBody` — upload/stream/download support types.

**Consumers:** `feature/demo`'s `DemoApiService`/`DemoRemoteDataSourceImpl`.

## `core/di`

Hilt modules for app-wide wiring.

- `AppCoreBindingsModule` — binds `DefaultAppDispatchers` to `AppDispatchers`, `EncryptedSecureStore` to `SecureStore`, `SecureStoreAuthTokenProvider` to `AuthTokenProvider`, `AndroidElapsedRealtimeClock` to `ElapsedRealtimeClock`, and `AndroidStringProvider` to `StringProvider`.
- `AppCoreModule` — provides `SettingsStore` and `LocaleManager`.
- `NetworkBindingsModule` — binds `RetrofitApiClient` and `OkHttpFileTransferClient`.
- `NetworkModule` — provides `ApiConfig`, `ConnectivityChecker`, `OkHttpClient`, and `Retrofit` (built via `core/network/NetworkClientFactory`). Feature-specific Retrofit services belong in that feature's own DI module.

## `core/localization`

Per-app language switching, backed by AndroidX's per-app language API (`AppCompatDelegate.setApplicationLocales`). The manifest declares `android:localeConfig="@xml/locales_config"` and opts into AppCompat `autoStoreLocales`.

- `LanguageOption(code: String, displayName: String)` + `val SUPPORTED_LANGUAGES` sample data (en, vi).
- `LocaleTagMapper` (object) — `toRegionalTag(languageCode): String`, mapping `vi`→`vi-VN`, `ko`→`ko-KR`, `zh-TW`→`zh-TW` (passthrough otherwise).
- `AppLocaleApplier` (interface, `fun applyLocales(tag: String)`) + `AppCompatLocaleApplier` (real impl) — injected as an interface so `LocaleManager` is unit-testable.
- `LocaleManager(localeApplier = AppCompatLocaleApplier())` — synchronous `fun setLanguage(languageCode: String)` mapped through `LocaleTagMapper` and applied.

**Consumers:** `MainActivity`'s EN/VI buttons drive `LocaleManager.setLanguage(...)` directly.

## `core/ui/responsive`

A `smallestScreenWidthDp` clamp to avoid tablet/wide-screen layout issues.

- `ResponsiveConfig(enabled: Boolean = true, minSmallestScreenWidthDp: Int = 320, maxSmallestScreenWidthDp: Int = 480)`.
- `ResponsiveContextWrapper` (object) — `wrap(context, config): Context`, clamps `smallestScreenWidthDp` into `[min, max]` via `createConfigurationContext`.

**Consumers:** `BaseActivity.attachBaseContext`.

## `core/ui/text`

- `StringProvider` (interface) — `fun getString(@StringRes resId: Int): String`, lets a ViewModel resolve string resources without holding an Activity/View `Context`.
- `AndroidStringProvider` — the real implementation, backed by an injected `@ApplicationContext Context`. Bound in Hilt via `AppCoreBindingsModule`.

## `core/ui/base`

Shared UI infrastructure.

- `BaseActivity<VB : ViewBinding>` (abstract) — ViewBinding lifecycle, responsive context wrapping, immersive full-screen display cutout setup, and exit transitions.
- `BaseFragment<VB : ViewBinding>` — Fragment view lifecycle binding and flow collector.
- `BaseDialogFragment<VB : ViewBinding>` — rounded dialog fragment base using `R.drawable.bg_dialog`.
- `BaseBottomSheetDialogFragment<VB : ViewBinding>` — Material bottom-sheet view base.
- `collectOnStartedBy(lifecycleOwner, action)` (in `LifecycleFlowExtensions.kt`) — shared lifecycle-safe Flow collection; each Base* host's `collectOnStarted` delegates here with its own `LifecycleOwner` (the host itself for `BaseActivity`, `viewLifecycleOwner` for the Fragment/BottomSheet hosts).
- `renderResultState(result, contentRoot, dialogHost, onSuccess)` (in `ResultStateOverlay.kt`) — shared full-screen-loader + `PromptDialogFragment` error rendering; `BaseActivity`/`BaseFragment.bindResultState` both delegate here so the loading/error UI stays identical across hosts.
- `Debouncer` — pure rate limiter with `View.setOnDebouncedClickListener` click rate limiting.
- `ResultRenderState(isLoadingVisible, isContentVisible, isErrorVisible, errorMessage)` — visibility-only projection of a `ResultState<T>`. Not the same mechanism as `ResultStateOverlay`: this one toggles View visibility for screens that render inline (e.g. `feature/designsystem`); `ResultStateOverlay` drives a full-screen loader + dialog for `bindResultState` callers. Pick per-screen based on whether the loading/error UI should be inline or overlay the whole screen.

## `core/ui/components`

- `ButtonStyleDelegate` — shape/ripple background logic, resolving ripple color from `colorControlHighlight`.
- `FrameButton` (`FrameLayout` subclass) — custom shape button implementing `ButtonStyleDelegate`. Enforces 48dp minimum touch target.
- `ShadowLayout` (`FrameLayout` subclass) — rounded shadow layout drawn via elevation outline.
- `CustomSwitch` (`MaterialSwitch` subclass) — track and thumb tinted from color tokens, text hidden.
- `CustomToast` (object) — show Snackbar toast styled on base colors, returns the Snackbar instance.
- `FullScreenLoaderView` — custom full-screen loading spinner overlay shown during async operations.
- `PromptDialogFragment` — custom status dialog fragment supporting message, technical code, status icon (Success, Error, Info) and primary/secondary action handlers.

## `core/ui/util`

- `Shape` (enum: `RECTANGLE`, `OVAL`).
- `ShapeUtils` (object) — `buildDrawable(...)` programmatically creates GradientDrawables.

## `core/ui/theme`

App-wide light/dark/system theme, backed by AppCompat's night mode and persisted through `SettingsStore`.

- `AppTheme` (enum: `LIGHT`, `DARK`, `SYSTEM`, each with a `key: String`) — `AppTheme.fromKey(key)` maps a stored key back to an enum value, defaulting to `SYSTEM` if unrecognized.
- `ThemeManager` (interface) — `currentTheme: Flow<AppTheme>`, `suspend fun getTheme(): AppTheme`, `suspend fun setTheme(theme: AppTheme)`, `fun applyTheme(theme: AppTheme)`.
- `AndroidThemeManager` — the only implementation; reads/writes `AppSettingsKeys.THEME_MODE` via `SettingsStore` and applies the theme through `AppCompatDelegate.setDefaultNightMode`.
- `ThemeModule` (Hilt `@Module`) — binds `AndroidThemeManager` to `ThemeManager`.

**Consumers:** any screen that lets the user switch theme; `applyTheme` is also called on app start to restore the persisted choice.

## `core/navigation`

- `NavigationOptions` — option model containing custom `TransitionType` (DEFAULT, NONE, SLIDE_HORIZONTAL, FADE).
- `ActivityDestination` — typed activity target model.
- `ActivityNavigator` — navigates using transition override animations (SLIDE_HORIZONTAL, FADE).

## `core/time`

- `ElapsedRealtimeClock` — Monotonic clock interface using `SystemClock.elapsedRealtime()` for secure elapsed timing.

---

No other packages exist. Check the source tree before creating new code.
