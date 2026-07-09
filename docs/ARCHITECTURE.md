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

## Current Package Layout (Phase 0-5)

Everything above this section describes the target architecture. The folders below are what actually exists in the codebase today; check here (or the source tree) before assuming a core module already exists.

app/src/main/java/com/example/androidxmlbase/
  MainActivity.kt                            # launcher screen, XML + ViewBinding; overrides attachBaseContext (locale, then responsive clamp)
  core/
    architecture/
      UiState.kt
      UiEvent.kt
      UiEffect.kt
      ResultState.kt
      AppDispatchers.kt
      UseCase.kt
      StateViewModel.kt
    storage/
      SettingsKey.kt                         # typed key sealed class (String/Int/Long/Boolean/Float)
      SettingsStore.kt                       # observe/get/set/remove contract
      DataStoreSettingsStore.kt              # DataStore<Preferences>-backed implementation
      AppDataStore.kt                        # Context.appSettingsDataStore delegate
      AppSettingsKeys.kt                     # the 5 base app-wide keys
    network/
      ApiResult.kt                           # Success/HttpError/NetworkError/ParseError/EmptyBody
      ApiConfig.kt
      AuthTokenProvider.kt                   # + NoOpAuthTokenProvider
      ConnectivityChecker.kt                 # + AndroidConnectivityChecker
      ApiClient.kt
      RetrofitApiClient.kt                   # classifies Retrofit calls into ApiResult
      NetworkModule.kt                       # hand-wired Retrofit/OkHttp composition root
      interceptor/
        AuthTokenInterceptor.kt
        ConnectivityInterceptor.kt
    localization/
      LocaleTagMapper.kt                     # regional tag overrides (vi->vi-VN, ko->ko-KR, zh-TW->zh-TW), passthrough otherwise
      LocaleStore.kt                         # LocaleStore interface + SettingsStoreLocaleStore (backed by SettingsStore/AppSettingsKeys.LANGUAGE_CODE)
      LocaleManager.kt                       # AppLocaleApplier interface + AppCompatLocaleApplier (real) + LocaleManager (injected applier, unit-testable)
      LocaleContextWrapper.kt                # wraps a Context with a per-app Configuration locale, for attachBaseContext
      LanguageOption.kt                      # LanguageOption + SUPPORTED_LANGUAGES sample data (en, vi)
    ui/
      responsive/
        ResponsiveConfig.kt                  # enabled + min/max smallestScreenWidthDp
        ResponsiveContextWrapper.kt          # clamps Configuration.smallestScreenWidthDp into the configured range
      util/
        ShapeUtils.kt                        # builds runtime GradientDrawables (RECTANGLE/OVAL) shared by the components below
      components/
        ButtonStyleDelegate.kt               # shared shape/ripple background logic, framework-attribute-agnostic
        FrameButton.kt                       # FrameLayout-based button, the one ButtonStyleDelegate consumer ported so far
        ShadowLayout.kt                      # FrameLayout drawing an elevation+outline shadow (caller sets android:elevation)
        CustomSwitch.kt                      # SwitchCompat wrapper tinted from the color tokens
        CustomToast.kt                       # Toast.makeText drop-in replacement styled with the color tokens
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
        mapper/DemoMessageMapper.kt          # ApiResult<DemoMessageDto> -> ResultState<String>
      presentation/
        state/DemoUiState.kt, DemoUiEvent.kt, DemoUiEffect.kt
        viewmodel/DemoViewModel.kt, DemoViewModelFactory.kt
        ui/DemoActivity.kt
    designsystem/
      presentation/
        state/DesignSystemUiState.kt, DesignSystemUiEvent.kt
        viewmodel/DesignSystemViewModel.kt   # StateViewModel<DesignSystemUiState, DesignSystemUiEvent, UiEffect>, synchronous setState, no data/domain layers
        ui/DesignSystemActivity.kt           # showcases FrameButton, ShadowLayout, CustomSwitch, CustomToast, and the ResultState demo

`feature/demo` now has a `data/` package: its counter persists through `DemoRepositoryImpl`, backed by the real `DataStoreSettingsStore` wired in `DemoActivity`. It also performs a real (fake-endpoint) network call through `DemoRemoteDataSourceImpl` -> `DemoRepositoryImpl.fetchMessage()` -> `FetchDemoMessageUseCase`, rendered via `ResultState<String>` in `DemoViewModel`/`DemoActivity`. `DemoViewModelFactory` wires the full chain by hand using `NetworkModule.createRetrofit(...)` against a placeholder base URL. `core/storage`'s 5 base keys (`AppSettingsKeys`) now have their first real consumer: `LocaleStore` reads/writes `AppSettingsKeys.LANGUAGE_CODE` (default changed from `"en"` to `""`, meaning "no override, use system default"). `MainActivity` wraps its base `Context` with `LocaleContextWrapper` then `ResponsiveContextWrapper` in `attachBaseContext`, and its two EN/VI buttons drive `LocaleManager.setLanguage(...)` to prove the per-app language switch works end to end. `THEME_MODE` remains reserved for a future logging/theming core.
