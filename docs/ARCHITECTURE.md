# ARCHITECTURE.md

## Purpose  
This document defines the architectural principles for Android projects using Clean Architecture and MVVM. It focuses on maintainability, scalability, modularity, and separation of concerns to ensure robust and adaptable applications.

## Architecture Goals  
- Maintainability  
- Scalability  
- Testability  
- Predictability  
- Low coupling  
- High cohesion  

## High Level Architecture  
The architecture is divided into three layers:  
**Presentation**  
↓  
**Domain**  
↓  
**Data**  

Dependencies always point inward, from Presentation to Domain to Data.

## Layer Responsibilities  

### Presentation  
Responsibilities:  
- Render UI  
- Observe state  
- Handle user interaction  
- Navigation  

Must not:  
- Access APIs  
- Access database  
- Contain business rules  

### Domain  
Responsibilities:  
- Business rules  
- UseCases  
- Entities  
- Repository interfaces  

Must not:  
- Depend on Android framework  
- Know networking/database details  

### Data  
Responsibilities:  
- Repository implementations  
- Remote data  
- Local data  
- Caching  
- Data synchronization  

Must not:  
- Contain UI logic  
- Leak API or database models to Presentation  

## Dependency Rule  
Dependencies always flow from Presentation → Domain → Data. Reverse dependencies are strictly prohibited to maintain clear separation and independence.

## Dependency Matrix  
| From / To         | Presentation | Domain | Data | Feature | Shared/Core |
|-------------------|--------------|--------|------|---------|-------------|
| Presentation      | -            | ✅     | ❌   | ❌      | ✅          |
| Domain            | ❌           | -      | ❌   | ❌      | ✅          |
| Data              | ❌           | ✅*    | -    | ❌      | ✅          |
| Feature           | ❌           | ❌     | ❌   | ❌      | ✅          |
| Shared/Core       | ❌           | ❌     | ❌   | ❌      | -           |

*Data → Domain dependency only allowed for Repository interface usage.

## Feature Architecture  
Each feature should own:  
- UI  
- ViewModel  
- UseCases  
- Repository interface usage  
- Data implementation when applicable  

Avoid coupling between features to promote modularity and independent development.

## Recommended Feature Structure  
Example folder structure for a feature module:  
```
feature/
    presentation/
        ui/
        viewmodel/
        state/
    domain/
        model/
        repository/
        usecase/
    data/
        datasource/
        repository/
        mapper/
```
The exact structure may vary depending on the project needs, but responsibilities should remain consistent to maintain clarity and separation of concerns.

## Repository Pattern  
Repositories coordinate multiple data sources to provide a unified interface. Repository interfaces belong to the Domain layer, while their implementations reside in the Data layer.

## Data Sources  
Data sources include:  
- RemoteDataSource  
- LocalDataSource  
- Optional CacheDataSource  

Repositories orchestrate these data sources to provide consistent data.

## UseCase Guidelines  
- One business capability per UseCase  
- Composable and reusable  
- Independent of framework and UI  
- Easily testable  

## Mapper Guidelines  
Mapping flow:  
- ApiModel → Entity → UiModel  
- DatabaseModel → Entity  

Mappers should be deterministic and maintain clear transformations between layers.

## MVVM Responsibilities  
**View:**  
- Render UI only  

**ViewModel:**  
- Own screen state  
- Coordinate UseCases  
- Contain no Android framework business logic  

## Data Flow  
Recommended request flow:  
User Action → View → ViewModel → UseCase → Repository → DataSource → API/Database.  
The response flows back in reverse order after appropriate mapping at each layer, ensuring data consistency and separation of concerns.

## UI State  
Use a clear separation of:  
- UiState (screen state)  
- UiEvent (user or system events)  
- UiEffect (one-time effects like navigation or messages)  

## Dependency Injection  
- Use constructor injection  
- Inject abstractions, not implementations  
- Keep modules independent and loosely coupled  

## Shared Modules  
- Move code into shared/core modules only after proven reuse  
- Avoid feature-specific logic in shared modules to maintain modularity  

## Shared Module Rules  
- Shared code must be framework-agnostic where possible  
- Never depend on feature modules  
- Avoid business logic tied to a single feature  
- Prefer moving code only after two or more proven reuse cases  

## Feature Communication  
- Communicate through public contracts/interfaces  
- Avoid direct dependencies between features  

## Error Propagation  
- Errors propagate from Data → Domain → Presentation  
- Convert technical errors into domain-friendly results  
- UI presents user-friendly error messages  

## Architecture Smells  
- Business logic in UI  
- ViewModel accessing APIs directly  
- Domain layer depending on Android framework  
- Circular dependencies between modules  
- God repositories handling too many responsibilities  
- Massive ViewModels  
- Duplicate business logic scattered across layers  
- Shared modules depending on feature-specific code  

## Refactoring Strategy  
Improve architecture incrementally by making small, safe refactors that preserve existing behavior. Avoid big-bang rewrites to reduce risk and maintain project stability. Prioritize continuous improvement and maintainability through gradual enhancements.

## Architecture Principles  
- **Separation of Concerns:** Each layer and module has a distinct responsibility to reduce complexity.  
- **Dependency Inversion:** High-level modules should not depend on low-level modules; both depend on abstractions.  
- **Single Responsibility:** Classes and modules should have one reason to change, focusing on a single task.  
- **Feature Isolation:** Features should be self-contained to enable independent development and testing.  
- **Composition over Inheritance:** Prefer composing behaviors over complex inheritance hierarchies for flexibility.  
- **Explicit Dependencies:** Dependencies should be clearly declared and injected to improve testability and clarity.  
- **Predictable Data Flow:** Data should flow in a clear, unidirectional manner to simplify reasoning and debugging.  
- **Stable Public Contracts:** Interfaces between modules should be stable and well-defined to minimize coupling.  

## Architecture Review Checklist  
- [ ] Are layer boundaries clearly defined and respected?  
- [ ] Do dependencies flow inward only (Presentation → Domain → Data)?  
- [ ] Is business logic contained exclusively in the Domain layer?  
- [ ] Are UI components free of business and data access logic?  
- [ ] Are repository interfaces defined in Domain and implementations in Data?  
- [ ] Are UseCases focused on a single business capability?  
- [ ] Are mappers deterministic and correctly transforming data between layers?  
- [ ] Is dependency injection used consistently with abstractions?  
- [ ] Are shared modules free from feature-specific logic?  
- [ ] Is feature communication handled via public contracts without tight coupling?  
- [ ] Is error handling consistent and user-friendly across layers?  
- [ ] Are ViewModels free from Android framework dependencies and business logic?  
- [ ] Is UI state management separated into UiState, UiEvent, and UiEffect?  
- [ ] Are data sources properly encapsulated and orchestrated by repositories?  
- [ ] Are modules designed for low coupling and high cohesion?  
- [ ] Are there no circular dependencies between modules or layers?  
- [ ] Is the architecture scalable and maintainable for future growth?  
- [ ] Are tests easily written for UseCases, repositories, and ViewModels?

## Current Package Layout (Phase 0-6)

Everything above this section describes the target architecture. The folders below are what actually exists in the codebase today; check here (or the source tree) before assuming a core module already exists.

See `docs/FEATURE_TEMPLATE.md`, `docs/CORE_MODULES.md`, and `docs/DESIGN_SYSTEM.md` for a full walkthrough of building a new feature on top of the layout below.

app/src/main/java/com/example/androidxmlbase/
  MainActivity.kt                            # launcher screen, XML + ViewBinding; extends BaseActivity, no attachBaseContext override of its own
  core/
    architecture/
      UiState.kt
      UiEvent.kt
      UiEffect.kt
      AppDispatchers.kt
      UseCase.kt
      StateViewModel.kt
      result/
        ResultState.kt
        DomainResult.kt                      # also declares AppError & map helper
    storage/
      settings/
        SettingsKey.kt                       # typed key sealed class (String/Int/Long/Boolean/Float)
        SettingsStore.kt                     # observe/get/set/remove contract
        DataStoreSettingsStore.kt            # DataStore<Preferences>-backed implementation
        AppDataStore.kt                      # Context.appSettingsDataStore delegate
        AppSettingsKeys.kt                   # app-wide keys only; feature keys stay feature-private
      secure/
        SecureStore.kt                       # + SecureStoreKey, SecureStoreKeys
        EncryptedSecureStore.kt
    network/
      ApiResult.kt                           # Success/HttpError/NetworkError/ParseError/EmptyBody
      ApiConfig.kt
      ApiClient.kt
      RetrofitApiClient.kt                   # classifies Retrofit calls into ApiResult
      NetworkModule.kt                       # OkHttp timeouts + Retrofit composition root
      auth/
        AuthTokenProvider.kt                 # + NoOpAuthTokenProvider
        SecureStoreAuthTokenProvider.kt
        AuthTokenInterceptor.kt
      connectivity/
        ConnectivityChecker.kt               # + AndroidConnectivityChecker
        ConnectivityInterceptor.kt
      transfer/
        FileTransferClient.kt                # + OkHttpFileTransferClient
        TransferResult.kt
        ProgressRequestBody.kt
    localization/
      LocaleTagMapper.kt                     # regional tag overrides (vi->vi-VN, ko->ko-KR, zh-TW->zh-TW), passthrough otherwise
      LocaleManager.kt                       # AppLocaleApplier interface + AppCompatLocaleApplier (real) + LocaleManager (injected applier, unit-testable)
      LanguageOption.kt                      # LanguageOption + SUPPORTED_LANGUAGES sample data (en, vi)
    ui/
      base/
        BaseActivity.kt                      # ViewBinding inflate + responsive attachBaseContext + collectOnStarted + immersive window cutout support
        BaseFragment.kt
        BaseDialogFragment.kt
        BaseBottomSheetDialogFragment.kt
        Debouncer.kt                          # pure shouldAllow(nowMs) rate limiter + View.setOnDebouncedClickListener glue
        ResultRenderState.kt                  # ResultState<T>.toRenderState() (loading/content/error visibility + error message) + View.applyVisibilityTo glue
      responsive/
        ResponsiveConfig.kt                  # enabled + min/max smallestScreenWidthDp
        ResponsiveContextWrapper.kt          # clamps Configuration.smallestScreenWidthDp into the configured range
      util/
        ShapeUtils.kt                        # builds runtime GradientDrawables (RECTANGLE/OVAL) shared by the components below
      components/
        ButtonStyleDelegate.kt               # shared shape/ripple background logic, framework-attribute-agnostic
        FrameButton.kt                       # FrameLayout-based button, the one ButtonStyleDelegate consumer ported so far
        ShadowLayout.kt                      # FrameLayout drawing an elevation+outline shadow (caller sets android:elevation)
        CustomSwitch.kt                      # MaterialSwitch wrapper tinted from the color tokens
        CustomToast.kt                       # Snackbar-based Toast replacement styled with the color tokens
        FullScreenLoaderView.kt              # Custom full-screen loading spinner overlay
        PromptDialogFragment.kt              # Status prompt dialog fragment (success, fail, info) with action callbacks
    navigation/
      ActivityDestination.kt
      ActivityNavigator.kt                   # transition support (SLIDE_HORIZONTAL, FADE, etc.)
      NavigationOptions.kt
    time/
      ElapsedRealtimeClock.kt                # Monotonic elapsed time utility (clock)
    di/
      AppCoreModule.kt, NetworkDiModule.kt  # Hilt app/core wiring
  feature/
    demo/
      domain/
        repository/DemoRepository.kt
        usecase/IncrementCounterUseCase.kt, ObserveDemoCountUseCase.kt, SaveDemoCountUseCase.kt,
          FetchDemoMessageUseCase.kt
      data/
        repository/DemoRepositoryImpl.kt     # SettingsStore- and remote-data-source-backed
        dto/DemoMessageDto.kt
        datasource/DemoApiService.kt, DemoRemoteDataSource.kt (+ DemoRemoteDataSourceImpl)
        mapper/DemoMessageMapper.kt          # ApiResult<DemoMessageDto> -> DomainResult<String>
      presentation/
        state/DemoUiState.kt, DemoUiEvent.kt, DemoUiEffect.kt, DemoMessageState.kt
        viewmodel/DemoViewModel.kt
        ui/DemoActivity.kt
      di/DemoModule.kt
    designsystem/
      presentation/
        state/DesignSystemUiState.kt, DesignSystemUiEvent.kt
        viewmodel/DesignSystemViewModel.kt   # StateViewModel<DesignSystemUiState, DesignSystemUiEvent, UiEffect>, synchronous setState, no data/domain layers
        ui/DesignSystemActivity.kt           # showcases FrameButton, ShadowLayout, CustomSwitch, CustomToast, and the ResultState demo

`feature/demo` now has a `data` package and Hilt bindings: its counter persists through `DemoRepositoryImpl`, backed by the real `DataStoreSettingsStore` provided by Hilt. It also performs a real (fake-endpoint) network call through `DemoRemoteDataSourceImpl` -> `DemoRepositoryImpl.fetchMessage()` -> `FetchDemoMessageUseCase`, returning `DomainResult<String>` from data/domain and mapping that into `DemoMessageState` in presentation. Hilt wires the full chain through `core/di` and `feature/demo/di/DemoModule`; feature-specific Retrofit service providers stay in the feature module, while `core/di` only provides reusable Retrofit/OkHttp infrastructure. `SecureStore` handles auth/refresh tokens separately from normal settings, and backup/data-extraction rules exclude the secure store shared-preferences file. `MainActivity`'s two EN/VI buttons drive `LocaleManager.setLanguage(...)` through AppCompat per-app locales; the manifest declares `@xml/locales_config` and opts into AppCompat `autoStoreLocales`, so no Activity blocks on DataStore during `attachBaseContext`.

`MainActivity`, `DemoActivity`, and `DesignSystemActivity` all extend `core/ui/base/BaseActivity`, which owns responsive `attachBaseContext` wrapping and ViewBinding inflation; subclasses implement `inflateBinding` and do their view/ViewModel wiring in `onBindingReady` instead of `onCreate`. Both feature Activities use `BaseActivity.collectOnStarted` for lifecycle-safe `Flow` collection. `DesignSystemActivity.render()` consumes `ResultState<T>.toRenderState()`, while `DemoActivity` resolves `DemoMessageState` into localized string resources. `DemoActivity`'s increment button uses `View.setOnDebouncedClickListener`, the button most likely to be rapid-tapped.
