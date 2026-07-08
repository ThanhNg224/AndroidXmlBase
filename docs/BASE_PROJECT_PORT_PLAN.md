# Android XML Clean Base - Port Plan

## Goal

Build a reusable Android XML base project that keeps the strongest parts of the HeyJapan architecture while removing app-specific product logic.

This base should be easy to clone for a new Android app and should provide:

- MVVM + Clean Architecture structure.
- XML UI with ViewBinding, not Compose.
- Strong core modules for storage, API, localization, responsive resources, design system, custom views, logging, analytics, and navigation helpers.
- A sample feature that proves the full stack works end to end.

Source project for reference:

```text
/Users/thanhng224/Dev/Kalapa/heyjapan-android-main
```

Target project:

```text
/Users/thanhng224/AndroidStudioProjects/AndroidXmlBase
```

## Current Target Project State

The target project was created by Android Studio and currently looks like a Compose template:

- `app/build.gradle.kts` enables `buildFeatures.compose = true`.
- Version catalog includes Compose BOM and Compose dependencies.
- The requested target is Android XML, so the first implementation phase must convert the starter app away from Compose and toward XML + ViewBinding.

Do not port feature/business logic from HeyJapan directly. Port the reusable platform ideas and rewrite where the current implementation is too tied to HeyJapan.

## Target Architecture

Use MVVM as the presentation pattern and Clean Architecture as the dependency boundary.

Dependency direction:

```text
UI -> ViewModel -> UseCase -> Repository interface -> RepositoryImpl -> DataSource/API/DB/Storage
```

Rules:

- UI renders state and emits user events.
- ViewModel owns presentation logic only.
- UseCase owns single business action or orchestration.
- Domain does not depend on Android framework classes.
- Repository interfaces live in `domain`.
- Repository implementations and data sources live in `data`.
- Feature modules must not depend on other feature modules directly.
- Shared modules must not contain feature-specific code.

Recommended feature folder:

```text
feature/<name>/
  data/
    datasource/
    dto/
    mapper/
    repository/
  domain/
    entity/
    repository/
    usecase/
  presentation/
    model/
    viewmodel/
    ui/
```

Recommended core folder:

```text
core/
  architecture/
  common/
  storage/
  network/
  localization/
  designsystem/
  ui/
  analytics/
  navigation/
  logging/
```

## What To Extract From HeyJapan

### 1. Architecture Base

Reference files:

```text
app/src/main/java/com/eup/heyjapan/clean_architecture/feature/base_feature/data/datasources/BaseLocalDataSource.kt
app/src/main/java/com/eup/heyjapan/clean_architecture/feature/base_feature/data/datasources/BaseRemoteDataSource.kt
app/src/main/java/com/eup/heyjapan/clean_architecture/feature/base_feature/data/repositories/BaseRepositoryImpl.kt
app/src/main/java/com/eup/heyjapan/clean_architecture/feature/base_feature/domain/repositories/BaseRepository.kt
app/src/main/java/com/eup/heyjapan/clean_architecture/feature/base_feature/presentation/viewmodel/BaseViewModel.kt
```

Decision:

- Do not copy blindly.
- Rewrite into `core/architecture`.
- Keep the idea of base repository/data source/viewmodel.
- Improve state/event/effect handling.

Target concepts:

```text
UiState
UiEvent
UiEffect
StateViewModel<S, E, F>
ResultState<T>
AppDispatchers
UseCase conventions
```

Acceptance:

- Sample feature uses `StateViewModel`.
- Navigation/toast/dialog one-shot actions use `UiEffect`, not sticky state.
- Domain layer has no Android `Context`, `View`, `LiveData`, or resource dependency.

### 2. Storage / Preferences

Reference files:

```text
app/src/main/java/com/eup/heyjapan/clean_architecture/core/helpers/storage/preference/PreferencesHelper.kt
app/src/main/java/com/eup/heyjapan/clean_architecture/core/helpers/storage/preference/PreferenceKeys.kt
```

Current HeyJapan strengths:

- Centralized key list.
- Thin storage abstraction.
- Used widely by repositories/data sources.

Problems to fix:

- Uses `SharedPreferences`.
- `saveData(key: String, value: Any)` is weakly typed.
- `PreferenceKeys` mixes many feature-specific keys into one large file.
- Some keys depend on HeyJapan domain concepts.

Target:

```text
core/storage/
  SettingsKey<T>
  SettingsStore
  DataStoreSettingsStore
  SecureStore
  AppSettingsKeys
```

Use Jetpack DataStore for normal key-value settings. Keep SecureStore separate for secrets/tokens if needed.

Decision:

- Rewrite, do not copy.
- Split keys into base keys and feature keys.
- Keep migration support from SharedPreferences optional, useful if this base is later used to modernize an existing app.

Acceptance:

- Typed get/set/remove APIs.
- `Flow<T>` read support.
- Unit tests cover String, Int, Long, Boolean, Float, default values, remove, and migration.

### 3. Network / API Core

Reference files:

```text
app/src/main/java/com/eup/heyjapan/clean_architecture/core/helpers/network/client/NetworkClient.kt
app/src/main/java/com/eup/heyjapan/clean_architecture/core/helpers/network/client/RetrofitNetworkClientImpl.kt
app/src/main/java/com/eup/heyjapan/clean_architecture/core/helpers/network/client/DelegatingNetworkClient.kt
app/src/main/java/com/eup/heyjapan/clean_architecture/core/helpers/network/client/CronetNetworkClientImpl.kt
app/src/main/java/com/eup/heyjapan/clean_architecture/core/helpers/network/NetworkResponse.kt
app/src/main/java/com/eup/heyjapan/clean_architecture/core/helpers/network/NetworkException.kt
app/src/main/java/com/eup/heyjapan/clean_architecture/core/helpers/network/RequestOptions.kt
app/src/main/java/com/eup/heyjapan/clean_architecture/core/helpers/network/interceptor/retrofit/AuthTokenInterceptor.kt
app/src/main/java/com/eup/heyjapan/clean_architecture/core/helpers/network/interceptor/retrofit/ConnectivityInterceptor.kt
```

Current HeyJapan strengths:

- Has a `NetworkClient` facade.
- Supports generic HTTP calls.
- Has upload/download/stream APIs.
- Has auth and connectivity interceptors.
- Has separate response/error model.

Problems to fix:

- Some API behavior is tied to HeyJapan base URLs/payment domains.
- Generic response conversion is risky.
- Error handling should be more explicit.
- Cronet should be optional, not required in the base.

Target:

```text
core/network/
  ApiClient
  ApiResult<T>
  ApiError
  ApiConfig
  RequestOptions
  AuthTokenProvider
  ConnectivityChecker
  RetrofitApiClient
  Interceptors
```

Decision:

- Rewrite around the same facade idea.
- Use Retrofit + OkHttp as default.
- Keep streaming/download support because it was a strong part of the old app.
- Make auth token provider injectable.
- Make logging behavior depend on build type and a runtime debug flag.

Acceptance:

- Unit tests for success, HTTP error, no connection, empty body, parse error.
- MockWebServer integration tests for headers, query params, body, and auth.
- Sample feature performs a fake API call through repository/usecase/viewmodel.

### 4. Localization

Reference files:

```text
app/src/main/java/com/eup/heyjapan/utils/helper/AppContextWrapper.java
app/src/main/java/com/eup/heyjapan/utils/helper/LocaleHelper.java
app/src/main/java/com/eup/heyjapan/dialog/LanguageSelectorBottomSheet.java
app/src/main/java/com/eup/heyjapan/clean_architecture/feature/settings/data/datasources/local/SettingsLocalDataSource.kt
```

Current HeyJapan strengths:

- Language override is applied consistently through wrapped context.
- Has custom locale mapping such as `vi -> vi_VN`, `ko -> ko_KR`, `zh-TW -> zh_TW`.
- Language is persisted and used by features.

Problems to fix:

- `AppContextWrapper` mixes locale handling and responsive `smallestScreenWidthDp` override.
- `LocaleHelper` depends on legacy `Preference`.
- Java implementation should become Kotlin.
- New project should align with Android/AppCompat per-app language support where possible.

Target:

```text
core/localization/
  LocaleManager
  AppLocale
  LocaleStore
  LocaleContextWrapper
  LanguageOption
```

Decision:

- Split localization from responsive resource policy.
- Store language through `SettingsStore`.
- Use AppCompat/Android per-app language API where applicable.
- Keep context wrapper as compatibility fallback for XML/activity resources.

Acceptance:

- Runtime language switch works without restarting the entire process where possible.
- Activity `attachBaseContext` or base activity path is documented.
- Tests cover locale mapping.

### 5. Responsive Size / Resource Policy

Reference files:

```text
app/src/main/java/com/eup/heyjapan/utils/helper/AppContextWrapper.java
app/src/main/java/com/eup/heyjapan/clean_architecture/core/extensions/SdpExtensions.kt
docs/standard.md
```

Current HeyJapan strengths:

- Strong convention around `sdp/ssp`.
- `AppContextWrapper` clamps `smallestScreenWidthDp` to avoid tablet/wide-screen resource explosion.
- The standard doc has clear UI rules for no hardcoded `dp/sp`.

Problems to fix:

- Locale and responsive width are coupled.
- Clamp values are hardcoded.
- The policy should be opt-in/configurable per app.

Target:

```text
core/ui/responsive/
  ResponsiveConfig
  ResponsiveContextWrapper
  Int.sdp()
  Int.ssp()
```

Decision:

- Keep the idea, rewrite cleanly.
- Make clamp thresholds explicit in `ResponsiveConfig`.
- Keep sdp/ssp library convention.

Acceptance:

- XML docs require `@dimen/_<n>sdp` and `@dimen/_<n>ssp`.
- BaseActivity applies locale wrapper and responsive wrapper in a defined order.
- Demo screen validates common dimensions on phone and tablet emulator.

### 6. Design System / Custom Views

Reference files:

```text
app/src/main/java/com/eup/heyjapan/clean_architecture/core/ui/buttons/ButtonStyleDelegate.kt
app/src/main/java/com/eup/heyjapan/clean_architecture/core/ui/buttons/FrameButton.kt
app/src/main/java/com/eup/heyjapan/clean_architecture/core/ui/buttons/LinearButton.kt
app/src/main/java/com/eup/heyjapan/clean_architecture/core/ui/buttons/RelativeButton.kt
app/src/main/java/com/eup/heyjapan/clean_architecture/core/ui/buttons/ConstraintButton.kt
app/src/main/java/com/eup/heyjapan/clean_architecture/core/ui/buttons/CardButton.kt
app/src/main/java/com/eup/heyjapan/clean_architecture/core/ui/buttons/TextButton.kt
app/src/main/java/com/eup/heyjapan/clean_architecture/core/ui/buttons/ImageButton.kt
app/src/main/java/com/eup/heyjapan/clean_architecture/core/ui/buttons/IconButton.kt
app/src/main/java/com/eup/heyjapan/clean_architecture/core/ui/buttons/ViewButton.kt
app/src/main/java/com/eup/heyjapan/clean_architecture/core/utils/ShapeUtils.kt
app/src/main/res/values/attrs.xml
app/src/main/res/values/text_styles.xml
app/src/main/res/values/themes.xml
docs/standard.md
```

Current HeyJapan strengths:

- Custom button views reduce shape XML duplication.
- `ShapeUtils` supports runtime rectangle/oval/gradient/stroke.
- Strong theme attribute convention.
- Strong text style convention.
- sdp/ssp layout convention is already documented.

Problems to fix:

- Some views may depend on old package, callbacks, or HeyJapan-specific attrs.
- Some naming is app-specific.
- Need a clean showcase screen in the new base.

Target:

```text
core/designsystem/
  attrs.xml
  colors.xml
  themes.xml
  text_styles.xml
  dimensions guide

core/ui/components/
  ButtonStyleDelegate
  FrameButton
  LinearButton
  ConstraintButton
  CardButton
  TextButton
  ImageButton
  IconButton
  ViewButton
  FlowLayout
  ShadowLayout
  CustomSwitch
  CustomToast
```

Decision:

- Port selectively.
- Rewrite package names and remove HeyJapan references.
- Keep XML attrs stable and documented.

Acceptance:

- `DesignSystemActivity` or sample screen shows every custom button, text style, shape, toast, switch, and loading/error state.
- No hardcoded hex colors in layouts except launcher assets.
- No literal `dp/sp` in project-owned XML layouts.

### 7. Core UI Base

Reference files:

```text
app/src/main/java/com/eup/heyjapan/activity/BaseActivity.java
app/src/main/java/com/eup/heyjapan/fragment/BaseFragment.java
app/src/main/java/com/eup/heyjapan/clean_architecture/core/ui/dialog/BaseCustomDialog.kt
app/src/main/java/com/eup/heyjapan/clean_architecture/core/extensions/ActivityExtensions.kt
app/src/main/java/com/eup/heyjapan/clean_architecture/core/extensions/ViewExtensions.kt
```

Target:

```text
core/ui/base/
  BaseActivity
  BaseFragment
  BaseBottomSheetDialogFragment
  BaseDialog
  ViewBinding delegates
  Insets helpers
  Debounced click helpers
  Loading/error render helpers
```

Decision:

- Prefer Kotlin.
- Keep activities/fragments thin.
- Provide common edge-to-edge and lifecycle-safe dialog patterns.

Acceptance:

- Sample XML screen uses ViewBinding.
- Base screen handles locale/responsive wrapping.
- One-shot effects are collected lifecycle-safely.

### 8. Analytics / Logging

Reference files:

```text
app/src/main/java/com/eup/heyjapan/clean_architecture/core/analytics/Analytics.kt
app/src/main/java/com/eup/heyjapan/clean_architecture/core/analytics/AnalyticsClient.kt
app/src/main/java/com/eup/heyjapan/clean_architecture/core/analytics/CompositeAnalyticsClient.kt
app/src/main/java/com/eup/heyjapan/clean_architecture/core/utils/LogUtils.kt
```

Target:

```text
core/analytics/
  Analytics
  AnalyticsClient
  CompositeAnalyticsClient
  NoOpAnalyticsClient

core/logging/
  AppLogger
  TimberAppLogger or AndroidLogLogger
```

Decision:

- Keep interface + composite pattern.
- Do not include Firebase/Facebook clients in base by default; provide optional adapters later.
- No product-specific event constants in core.

Acceptance:

- Base can run with no-op analytics.
- Debug logs disabled or limited in release.

## Phased Implementation Plan

### Phase 0 - Convert Starter To Android XML

Scope:

- Remove Compose plugin and Compose dependencies.
- Enable ViewBinding.
- Add `MainActivity` XML layout.
- Ensure app launches with a simple XML screen.
- Keep version catalog clean.

Files likely touched:

```text
gradle/libs.versions.toml
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/java/com/example/androidxmlbase/MainActivity.kt
app/src/main/res/layout/activity_main.xml
```

Acceptance:

- `./gradlew :app:assembleDebug` passes.
- No Compose dependency remains unless explicitly kept for future optional module.

### Phase 1 - Establish Package And Architecture Skeleton

Scope:

- Decide final package, likely:

```text
com.example.androidxmlbase
```

or later:

```text
com.<company>.androidxmlbase
```

- Create base package folders.
- Create `core/architecture` primitives.
- Create sample feature skeleton.
- Update `docs/ARCHITECTURE.md` from placeholder into actual architecture rules.

Acceptance:

- Empty sample feature compiles.
- Architecture doc matches real folders.

### Phase 2 - Storage Core

Scope:

- Add DataStore dependencies.
- Implement typed settings store.
- Add base keys only:

```text
language_code
theme_mode
first_open_at
open_count
debug_logging_enabled
```

- Add tests.

Acceptance:

- Unit tests pass.
- Sample screen can read/write a setting.

### Phase 3 - Network Core

Scope:

- Add Retrofit, OkHttp, logging interceptor, Gson or Kotlin serialization.
- Implement `ApiClient`, `ApiResult`, `ApiError`.
- Implement auth token provider abstraction.
- Implement connectivity checker.
- Add MockWebServer tests.

Acceptance:

- Repository sample calls a fake endpoint.
- Error states flow to ViewModel/UI.

### Phase 4 - Localization And Responsive Policy

Scope:

- Implement `LocaleManager`.
- Implement `ResponsiveContextWrapper`.
- Add base activity context wrapping.
- Add language selector sample if useful.

Acceptance:

- Language switch affects string resource.
- Responsive wrapper can be enabled/disabled via config.

### Phase 5 - Design System And Custom Views

Scope:

- Port theme attrs, text styles, color tokens.
- Port custom button views and `ShapeUtils`.
- Add sdp/ssp dependencies and conventions.
- Build `DesignSystemShowcase`.
- Update `docs/STANDARD.md`.

Acceptance:

- Showcase screen displays buttons/text/colors/shapes.
- Layouts use `sdp/ssp`.
- Custom views compile without HeyJapan dependencies.

### Phase 6 - Core UI Base

Scope:

- Add `BaseActivity`, `BaseFragment`, ViewBinding helpers.
- Add lifecycle-safe effect collector.
- Add debounced click extension.
- Add loading/error helpers.

Acceptance:

- Sample feature uses base UI primitives.
- No business logic in Activity/Fragment.

### Phase 7 - Sample Feature End To End

Create a small reference feature, for example `feature/settings` or `feature/demo`.

It should demonstrate:

- XML screen.
- ViewBinding.
- ViewModel state/event/effect.
- UseCase.
- Repository interface in domain.
- RepositoryImpl in data.
- DataStore usage.
- API call through network client.
- Localization.
- Custom button/style.

Acceptance:

- A new developer can copy this feature as a template.

### Phase 8 - Documentation And Agent Handoff

Update:

```text
docs/ARCHITECTURE.md
docs/STANDARD.md
docs/GIT_FLOW.md
AGENTS.md
```

Add if useful:

```text
docs/FEATURE_TEMPLATE.md
docs/CORE_MODULES.md
docs/DESIGN_SYSTEM.md
```

Acceptance:

- An agent can implement a new feature by following docs only.
- Docs include examples and anti-patterns.

## Do Not Port

Do not port these into the base unless explicitly requested:

- HeyJapan lesson domain.
- IAP/paywall/sale domain.
- Ads-specific logic.
- Japanese learning content models.
- DBFlow legacy setup.
- Firebase/Facebook analytics concrete clients.
- Legacy singleton `Preference`.
- Large legacy helpers with mixed responsibilities.
- Feature-specific remote config keys.

## Migration Strategy From HeyJapan Files

Use this rule for every source file:

```text
copy only if generic and already clean
rewrite if generic idea is strong but implementation is app-specific
drop if product-specific or legacy-only
```

Initial classification:

| Area | Source | Action |
|---|---|---|
| Base MVVM/Clean contracts | `feature/base_feature` | Rewrite |
| Preference helper | `core/helpers/storage/preference` | Rewrite to DataStore |
| Network client facade | `core/helpers/network/client` | Rewrite with same idea |
| Network response/error | `core/helpers/network` | Rewrite/clean |
| Locale wrapper | `AppContextWrapper`, `LocaleHelper` | Rewrite and split |
| sdp/ssp convention | `SdpExtensions`, `docs/standard.md` | Port and clean |
| Custom buttons | `core/ui/buttons` | Port selectively |
| Shape utils | `ShapeUtils.kt` | Port after cleanup |
| Text/theme tokens | `attrs.xml`, `text_styles.xml`, `themes.xml` | Port selectively |
| Analytics interfaces | `core/analytics` | Port interfaces only |
| Speech/Japanese parser | `core/speech`, `core/language` | Drop from base |

## Implementation Rules For Agents

Any agent continuing this work must follow these rules:

1. Read this file first.
2. Read target project `AGENTS.md`.
3. Do not copy large folders blindly from HeyJapan.
4. Implement one phase at a time.
5. After each phase, run build/tests relevant to that phase.
6. Keep package names consistent.
7. Update docs with every architectural decision.
8. Do not introduce Compose into the base unless the user changes the target.
9. Do not add Firebase, billing, ads, or product-specific dependencies to core.
10. Prefer typed APIs over `Any`, raw strings, and global singletons.

## Suggested First Agent Task

Use this prompt for the first implementation agent:

```text
Implement Phase 0 and Phase 1 from docs/BASE_PROJECT_PORT_PLAN.md.

Target project:
/Users/thanhng224/AndroidStudioProjects/AndroidXmlBase

Source reference project:
/Users/thanhng224/Dev/Kalapa/heyjapan-android-main

Requirements:
- Convert the Android Studio starter from Compose to XML + ViewBinding.
- Keep Kotlin DSL and version catalog.
- Add a simple XML MainActivity.
- Create initial core/architecture and feature/demo folder skeleton.
- Update docs/ARCHITECTURE.md to reflect the real target architecture.
- Do not port HeyJapan feature/product code yet.
- Run ./gradlew :app:assembleDebug.
```

## Open Decisions

Resolve before or during Phase 1:

- Final package name: keep `com.example.androidxmlbase` or replace with a company namespace.
- Single-module first or modular Gradle modules from the start.
  - Recommendation: start single-module with strict packages, then split into Gradle modules after core stabilizes.
- Serializer: Gson for familiarity or Kotlin serialization for stronger Kotlin-first modeling.
  - Recommendation: choose once network core starts.
- Minimum SDK: current target project uses minSdk 24. Keep unless product needs lower.
- Java target: current target project uses Java 11. Consider Java 17 only after confirming AGP/Kotlin/tooling compatibility in this project.

## Done Definition For The Base

The base project is ready when:

- Fresh clone builds with one command.
- It launches an XML sample screen.
- It has typed storage.
- It has network facade and tested error handling.
- It has localization and responsive resource policy.
- It has design system tokens and reusable custom views.
- It has a sample feature using MVVM + Clean Architecture end to end.
- Docs explain how to add a new feature.
- No HeyJapan product logic remains.
