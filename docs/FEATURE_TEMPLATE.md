# FEATURE_TEMPLATE.md

A step-by-step guide to adding a new feature to this base project, using the real `feature/demo` package as the worked example. `feature/demo` is the most complete reference feature in this codebase — it is the only one that exercises `core/architecture`, `core/storage`, `core/network`, and `core/ui` together. `feature/designsystem` is a second, simpler worked example (no `data/`, no domain use cases beyond a synchronous `StateViewModel`) — see the callouts below for what it shows instead.

Read `docs/CORE_MODULES.md` alongside this doc for the API surface of everything a feature is built on, and `docs/DESIGN_SYSTEM.md` for the UI tokens/components a screen's layout should use.

## 1. Folder layout

A feature lives under `app/src/main/java/com/example/androidxmlbase/feature/<name>/` with up to three top-level packages. Not every feature needs all three — see section 3 for when `data/` earns its place.

```
feature/demo/
  domain/
    repository/
      DemoRepository.kt              # interface: fun observeCount(): Flow<Int>, suspend fun saveCount(count: Int), suspend fun fetchMessage(): ResultState<String>
    usecase/
      IncrementCounterUseCase.kt      # plain class, sync business rule, no I/O
      ObserveDemoCountUseCase.kt      # plain class, Flow-returning
      SaveDemoCountUseCase.kt         # implements UseCase<Int, Unit>
      FetchDemoMessageUseCase.kt      # implements UseCase<Unit, ResultState<String>>
  data/
    repository/
      DemoRepositoryImpl.kt           # implements DemoRepository, owns a feature-private SettingsKey
    datasource/
      DemoApiService.kt               # Retrofit interface
      DemoRemoteDataSource.kt         # interface + DemoRemoteDataSourceImpl, wraps ApiClient.execute
    dto/
      DemoMessageDto.kt                # @Serializable wire model
    mapper/
      DemoMessageMapper.kt             # ApiResult<DemoMessageDto>.toResultState(): ResultState<String>
  presentation/
    state/
      DemoUiState.kt, DemoUiEvent.kt, DemoUiEffect.kt
    viewmodel/
      DemoViewModel.kt, DemoViewModelFactory.kt
    ui/
      DemoActivity.kt
```

Notes on what's *not* here, on purpose:
- No `domain/entity` (or `domain/model`) package. `DemoRepository`'s methods return primitives (`Int`, `String` wrapped in `ResultState`) — there is no feature-specific entity type yet that a mapper produces. Add one only when a feature's domain model is genuinely richer than what the DTO/primitive already expresses; don't create an empty passthrough entity for its own sake.
- `feature/designsystem` has only `presentation/` — no `domain/`, no `data/`. It is a pure UI-state showcase (see `DesignSystemViewModel`): its `onEvent` sets state synchronously with no repository or use case involved at all. That's a valid shape for a feature that never reads or writes real data — don't force empty `domain/`/`data/` packages onto it just to match the folder template.

## 2. Wiring a screen end to end

The real call chain in `feature/demo`, read bottom-to-top from where a tap originates to where data comes back:

```
DemoActivity (extends BaseActivity<ActivityDemoBinding>)
  -> DemoViewModel (extends StateViewModel<DemoUiState, DemoUiEvent, DemoUiEffect>)
    -> IncrementCounterUseCase          (plain sync class — no repository)
    -> SaveDemoCountUseCase             (implements UseCase<Int, Unit>)
    -> FetchDemoMessageUseCase          (implements UseCase<Unit, ResultState<String>>)
    -> ObserveDemoCountUseCase          (plain class, returns Flow<Int>)
      -> DemoRepository (domain interface) / DemoRepositoryImpl (data)
        -> SettingsStore                (persistence, via core/storage)
        -> DemoRemoteDataSource / DemoApiService / ApiClient (network, via core/network)
```

Concretely, in `DemoActivity.onBindingReady`:
1. `binding.btnIncrement.setOnDebouncedClickListener { viewModel.onEvent(DemoUiEvent.IncrementClicked) }` — a `FrameButton` (see `docs/DESIGN_SYSTEM.md`), debounced via `core.ui.base.setOnDebouncedClickListener` since it's the control most likely to be rapid-tapped.
2. `viewModel.state.collectOnStarted { ... }` renders `count` and `message` into `tvCount`/`tvMessage`.
3. `viewModel.effect.collectOnStarted { ... }` handles the one-shot `DemoUiEffect.ShowToast`.

In `DemoViewModel`:
- `init` launches two coroutines: one reads the persisted count once (`observeDemoCount().first()`), sets `isInitialCountLoaded = true`, then keeps collecting (`.drop(1)`) for future external changes; the other calls `fetchDemoMessage(Unit)` once and stores the `ResultState<String>` into `DemoUiState.message`.
- `onIncrementClicked()` guards on `isInitialCountLoaded` (see the comment in the real file — this exists specifically to avoid computing the next count from the constructor-default `0` while the real DataStore read is still in flight, which would clobber the persisted value with a stale increment), calls `incrementCounter(currentState.count)`, updates state, fires `saveDemoCount` async, and sends `ShowToast` if the result is capped.

`DemoViewModelFactory` is the composition root: it hand-builds `NetworkModule.createRetrofit(...)` -> `DemoApiService` -> `DemoRemoteDataSourceImpl` (+ `RetrofitApiClient`) -> `DemoRepositoryImpl` (also given the `SettingsStore` passed in from `DemoActivity`) -> the four use cases -> `DemoViewModel`. There is no DI framework in this codebase — every new feature wires its dependency graph by hand in its own `<Feature>ViewModelFactory`, following this exact pattern.

`DemoActivity` obtains its `ViewModel` via:
```kotlin
private val viewModel: DemoViewModel by lazy {
    val settingsStore = DataStoreSettingsStore(applicationContext.appSettingsDataStore)
    val factory = DemoViewModelFactory(applicationContext, settingsStore)
    ViewModelProvider(this, factory).get(DemoViewModel::class.java)
}
```

## 3. When to add a `data/` layer

**Rule: don't add `data/` (or a repository) until the feature actually reads or writes real data.**

This is not a stylistic preference — it's this codebase's own precedent. `feature/demo` shipped in Phase 1 with *no* `data/` package and no repository at all; its `IncrementCounterUseCase` was a plain synchronous class with nothing to persist or fetch. The Phase 1 plan states the reasoning directly:

> "`feature/demo` has no `data/` package. It does not read or write any real data, so a repository or data source would be an abstraction with nothing to abstract. That layer gets added once Phase 2 (storage) or Phase 3 (network) gives the feature something to fetch or persist."

`data/repository`, `data/datasource`, `data/dto`, and `data/mapper` were added incrementally, exactly when a real need appeared: Phase 2 added `data/repository/DemoRepositoryImpl.kt` the moment the counter needed to survive process death; Phase 3 added `data/dto`, `data/datasource`, `data/mapper` the moment there was a real (if fake-endpoint) network call to make. If your new feature's screen is purely local UI state with no persistence and no API call — like `feature/designsystem` — it should look like `feature/designsystem`: `presentation/` only, no `domain/`, no `data/`.

## 4. When a UseCase should implement `core.architecture.UseCase<in P, R>`

`UseCase<in P, R>` is a `suspend operator fun invoke(params: P): R` contract for **single-result, suspend operations**. Implement it only when that shape actually fits:

- **Implement it** — `SaveDemoCountUseCase : UseCase<Int, Unit>` and `FetchDemoMessageUseCase : UseCase<Unit, ResultState<String>>` both do exactly one suspend operation and return exactly one result.
- **Don't implement it, stay a plain class** —
  - `ObserveDemoCountUseCase` returns a `Flow<Int>` (an ongoing stream, not a single suspend result); it's a plain class with `operator fun invoke(): Flow<Int> = repository.observeCount()`.
  - `IncrementCounterUseCase` is a pure synchronous business rule with no I/O at all (`operator fun invoke(currentCount: Int): IncrementResult`); forcing it into a `suspend` interface would add ceremony with zero benefit.

Don't force every use case in a feature onto the same interface just for consistency — pick the shape that matches what the use case actually does.

## 5. Checklist for a new feature

- Does this screen need a ViewModel, or is it static? If static, you may not need `presentation/viewmodel` at all — but every screen in this codebase so far has used `StateViewModel`, even `feature/designsystem`'s purely-synchronous one; only skip it if there's truly no state or events.
- Does it read or write data? If not, skip `data/` (and `domain/repository`) entirely — see section 3.
- Does it need a new setting? Define a feature-private `SettingsKey` inside the feature's own repository implementation (see `DemoRepositoryImpl`'s private `DEMO_COUNTER_COUNT` key), not in `core.storage.AppSettingsKeys` — that object is reserved for genuinely app-wide keys (language, theme, first-open timestamp, debug logging). See section 6, anti-pattern 2.
- Does a use case return a `Flow` or do pure sync work with no I/O? Keep it a plain callable class. Does it do exactly one suspend operation with one result? Implement `UseCase<in P, R>`. See section 4.
- New `Activity`? Extend `core.ui.base.BaseActivity<VB>`, implement `inflateBinding(inflater)`, and do all view/ViewModel wiring in `onBindingReady(savedInstanceState)` — `onCreate` is `final` in `BaseActivity`, so there is nothing to override there.
- Buttons: use `core.ui.components.FrameButton` with design tokens from `docs/DESIGN_SYSTEM.md`, not a plain `<Button>` or hardcoded colors. Debounce the control most likely to be rapid-tapped with `View.setOnDebouncedClickListener` (not necessarily every control — one real usage per screen has been this codebase's bar so far).
- Layout dimensions: use `@dimen/_<n>sdp` / `@dimen/_<n>ssp`, never a literal `16dp`/`14sp` (see `docs/DESIGN_SYSTEM.md` for the convention and its rationale).
- Loading/success/error UI: model it with `ResultState<T>` in your `UiState` and either `ResultState.fold(...)` (for choosing *text*) or `.toRenderState()` (for toggling *visibility*) — see `DemoActivity.toDisplayText()` and `DesignSystemActivity.render()` for both patterns in the same codebase.
- All user-facing strings go through `strings.xml` (and get a `values-vi/strings.xml` translation — see `feature/demo`'s `demo_title`/`increment`).

## 6. Anti-patterns (do not do these)

These are drawn from real decisions made across this project's phases — not generic advice.

1. **Adding a repository/data source before there's real data to fetch or persist.** `feature/demo` deliberately shipped with zero `data/` files in Phase 1 for exactly this reason (quoted in full in section 3). If you're writing a `FooRepository` interface whose only implementation returns hardcoded/in-memory values with no real backing store, you're speculatively building the abstraction Phase 1's plan explicitly avoided.

2. **Putting a feature-specific `SettingsKey` into `AppSettingsKeys`.** `core.storage.AppSettingsKeys` holds exactly 5 keys, and the Phase 2 plan is explicit: *"Do not add more base keys speculatively — feature-specific keys belong to the feature that owns them... not to `AppSettingsKeys`."* `DemoRepositoryImpl`'s counter key (`demo_counter_count`) is defined as a private constant inside the repository implementation itself, not in the shared keys object. Follow that pattern for any new per-feature persisted value.

3. **Forcing a `Flow`-returning use case to implement the suspend `UseCase<P,R>` contract.** `ObserveDemoCountUseCase` is a plain class precisely because `UseCase<in P, R>`'s `suspend operator fun invoke(params: P): R` shape doesn't fit an ongoing stream. Don't wrap a `Flow` in a suspend function just to satisfy an interface that doesn't apply.

4. **Overriding `attachBaseContext` or hand-rolling ViewBinding inflate in a new `Activity` instead of extending `BaseActivity`.** `BaseActivity<VB>` centralizes exactly this (locale wrap, then responsive clamp, then binding inflate) so every screen gets it automatically and consistently. Before `BaseActivity` existed (Phase 6), `DemoActivity`/`DesignSystemActivity` had *no* locale/responsive wrapping at all — duplicating `MainActivity`'s override in every new Activity was the exact duplication `BaseActivity` was built to remove. Re-introducing a hand-rolled override in a new feature undoes that.

5. **Adding a new custom button variant (`CardButton`, `LinearButton`, `ConstraintButton`, etc.) before a real screen needs that specific shape.** The Phase 5 plan explicitly scoped this codebase to `ButtonStyleDelegate` + one concrete variant (`FrameButton`) out of the reference project's 9, stating: *"Porting all 9 with zero real screens needing more than one shape would be speculative, unused code... Adding `LinearButton`/`CardButton`/etc. is a follow-up whenever a real screen needs that specific shape — do not add them speculatively now."* The same reasoning blocked a `BaseFragment` in Phase 6 (*"There are zero Fragments or Dialogs anywhere in this codebase; building those base classes now would be speculative, unused code... Add them when a real Fragment/Dialog exists."*) — apply it to any new base class or component you're tempted to add "for completeness."
