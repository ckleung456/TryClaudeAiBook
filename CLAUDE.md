# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build everything
./gradlew :app:build

# Run feature-book unit tests (the only meaningful test suite)
./gradlew :features:feature-book:test

# Run a single test class
./gradlew :features:feature-book:testDebugUnitTest --tests "com.example.featureBook.usecase.LoadBooksUseCaseTest"

# Run a single test method
./gradlew :features:feature-book:testDebugUnitTest --tests "com.example.featureBook.ui.list.BooksListViewModelTest.toggleViewMode switches from LIST to GRID"

# Build feature module only
./gradlew :features:feature-book:build
```

**JDK requirement**: Gradle must use the Android Studio bundled JDK 21. This is already set in `gradle.properties` via `org.gradle.java.home`. Do not remove this line — the system JDK is too old for Hilt 2.57.2 / AGP 8.x.

**Annotation processors**: Hilt uses `kapt`; Room uses `ksp`. Both are already configured. When adding new Hilt-injectable classes, no extra wiring is needed beyond `@Inject constructor`.

## Architecture

Two Gradle modules:
- `:app` — `MainActivity` (entry point), `MyApplication` (`@HiltApplication`), and the Material3 theme. Contains no business logic.
- `:features:feature-book` — the complete books feature. All new feature work goes here.

### feature-book layer structure

```
model/
  network/   Book.kt              — network/JSON model (@Serializable)
  local/     BookEntity.kt        — Room entity (genres stored as comma-separated String)
  domain/    BookUiModel.kt       — UI model passed to composables
             SortOrder.kt         — ASCENDING / DESCENDING enum
             ViewMode.kt          — GRID / LIST enum
             MockBookData.kt      — hardcoded JSON (no real network)

module/
  network/   BooksRemoteRepository.kt  — @Singleton; decodes MockBookData JSON; simulates delay
  local/     BookDao.kt                — Room DAO (Flow<List<BookEntity>>, upsert, getById)
             BooksDatabase.kt          — Room database definition
             BooksCacheRepository.kt   — wraps BookDao; injected via @Inject constructor
  mapper/    BookMapper.kt             — Book↔BookEntity↔BookUiModel extension functions

usecase/
  base/FlowUseCase.kt      — shared UseCaseOutputWithStatus (Progress/Success/Failed) + FlowUseCase base class
  LoadBooksUseCase.kt      — extends FlowUseCase; emits cached list first, then refreshed list after remote fetch
  GetBookDetailUseCase.kt  — extends FlowUseCase; cache-first lookup, falls back to remote and saves result

di/
  BooksDatabaseModule.kt   — provides BooksDatabase and BookDao (SingletonComponent)

navigation/
  BooksNavRoutes.kt        — @Serializable route objects (BooksListRoute, BookDetailRoute)
  BooksNavGraph.kt         — wires BooksListRoot / BookDetailRoot to nav routes

ui/
  UiState.kt           — sealed interface UiState<T> (Loading / Success<T> / Error(UiText, errorData))
  UiText.kt            — sealed interface UiText (DynamicString / StringResource) + @Composable asString()
  UIStatefulContent.kt — @Composable that switches on UiState<T>
  ObserveAsEvents.kt   — LaunchedEffect helper for one-time event collection

  list/
    BooksListState.kt    — screen data inside UiState.Success
    BooksListAction.kt   — sealed interface of all user actions
    BooksListEvent.kt    — one-time side effect: NavigateToDetail
    BooksListViewModel.kt
    BooksListScreen.kt   — BooksListRoot (injects VM) + BooksListScreen (stateless)

  detail/
    BookDetailState.kt   — screen data inside UiState.Success
    BookDetailAction.kt  — OnRetry, OnBackClick
    BookDetailEvent.kt   — NavigateBack
    BookDetailViewModel.kt
    BookDetailScreen.kt  — BookDetailRoot (injects VM) + BookDetailScreen (stateless)
```

### MVVM pattern (Action / Event / UiState)

Every screen follows this contract:

- **`UiState<ScreenState>`** — ViewModel exposes `state: StateFlow<UiState<T>>`. Composables call `UIStatefulContent` to branch on Loading / Error / Success.
- **`Action`** — the only public mutation API on a ViewModel is `onAction(Action)`. No individual setter functions.
- **`Event`** — one-time side effects (navigation, etc.) flow through a `Channel<Event>` exposed as `events: Flow<Event>`. Root composables observe via `ObserveAsEvents(viewModel.events) { ... }`.
- **Composable split**: `*Root` composable injects `hiltViewModel()` and observes events; `*Screen` composable is stateless, receives `state` + `onAction`. Both live in the same file.
- **Navigation**: back navigation also routes through the event channel — `OnBackClick` action → `NavigateBack` event → root calls `navController.popBackStack()`. `BackHandler` in the screen composable calls `onAction(OnBackClick)`.

### Key patterns

**`FlowUseCase` base class** (`usecase/base/FlowUseCase.kt`): every use case extends `FlowUseCase<INPUT, INTERMEDIATE, RESULT>`, implementing `doWork(input): Flow<INTERMEDIATE>` and `onSucceedDataHandling(intermediate): UseCaseOutputWithStatus.Success<RESULT>`. `invoke(input)` always emits `Progress` first, then maps each `doWork` emission through `onSucceedDataHandling`, and converts any exception (from `doWork` or the mapping) into a terminal `Failed(error, failedResult)`. ViewModels collect this and switch on `Progress` / `Success` / `Failed`.

**Offline-first flow** (`LoadBooksUseCase.doWork`): sequential `flow { }` that (1) emits the current Room cache immediately, then (2) fetches from remote, saves to cache, and emits the refreshed cache. Do not change this to `channelFlow + launch` — it causes non-deterministic ordering in tests with `StandardTestDispatcher`. A remote failure after the cache emission surfaces as a `Failed` output (via `FlowUseCase`'s catch), not a thrown exception — the ViewModel still has the stale cached data until the next collect.

**`BooksListViewModel` state pipeline**: `_sortOrder.flatMapLatest { loadBooksUseCase.invoke(it) }` combined with `_viewMode`, `_searchQuery`, `_isSearchActive` flows via `combine`, producing `UiState<BooksListState>` via `stateIn(WhileSubscribed(5000))`. The `UseCaseOutputWithStatus` from `invoke()` is switched on inside the final `combine` block (`Progress` → `UiState.Loading`, `Success` → `UiState.Success`, `Failed` → `UiState.Error`). Sort order change re-triggers the use case.

**StateFlow conflation across multi-stage `combine` chains**: when a `FlowUseCase` emits several `UseCaseOutputWithStatus` values back-to-back with no real suspension between them (as happens with synchronous fakes in tests), intermediate values feeding a `combine().stateIn()` chain can be conflated away before a `Turbine` collector attaches — the collector may observe only a subset of `Progress`/`Success`/`Failed`, including possibly just the final one. Tests that assert on an intermediate stage (e.g. cache-first success before a remote failure) must loop-skip past whichever transient states arrive rather than assuming an exact emission count (see `BooksListViewModelTest.state shows error when use case emits failure`).

**`BookDetailViewModel`**: reads `bookId` directly from `SavedStateHandle["bookId"]` — do not use `toRoute<>()` (requires Navigation backstack context, unavailable in unit tests).

**Genres serialization**: `Book.genres: List<String>` → stored in Room as a comma-joined `String` in `BookEntity.genres` → split back on read in `BookMapper`. No TypeConverter is registered.

**Image loading**: Coil 3.x (`coil3:coil-compose`, `coil3:coil-network-okhttp`). Use `AsyncImage` with `rememberVectorPainter(Icons.AutoMirrored.Filled.MenuBook)` as placeholder/error painter.

**Navigation naming**: `BooksListRoute` / `BookDetailRoute` in `BooksNavRoutes.kt` are the `@Serializable` route objects used as type parameters in `composable<T>`. The root composables are named `BooksListRoot` / `BookDetailRoot` — these are different things and must stay distinct.

### Testing approach

Fakes over mocks — no Mockk usage in existing tests despite it being on the classpath:
- `FakeBookDao` — `MutableStateFlow`-backed; has `seed(List<BookEntity>)` to pre-populate; `getBookById`/`upsertBooks`/`clearAll` call `yield()` first (see gotcha below)
- `FakeBooksRemoteRepository` — extends `BooksRemoteRepository()`; exposes `var books` and `var shouldThrow`
- ViewModel tests construct the **real** `LoadBooksUseCase` / `GetBookDetailUseCase` wired to `FakeBooksRemoteRepository` + `FakeBookDao` (via `BooksCacheRepository`) rather than subclassing the use case — `FlowUseCase.invoke()` is a concrete, non-open function, so there is nothing to override; control test scenarios through the fakes' `books`/`shouldThrow`/`seed(...)` instead.

All ViewModel tests set `Dispatchers.setMain(UnconfinedTestDispatcher())` in `@Before` and reset in `@After`. Use Turbine's `Flow.test { }` for asserting emissions. Access state via `viewModel.state` (not `uiState`).

**StateFlow conflation gotcha**: if a fake `suspend fun` has no real suspension points, several state transitions (e.g. `Loading → Success`, or `Loading → Success → Error`) can happen in one tick and intermediate emissions are dropped by StateFlow conflation before a Turbine collector attaches — sometimes down to only the *final* value. `FakeBookDao`'s methods call `yield()` to give collectors a chance to observe transient states, but this doesn't guarantee every intermediate stage of a multi-stage `FlowUseCase` (Progress → cache Success → remote Failed) is individually observable. Prefer asserting the eventual/terminal state with a loop that skips past whichever transient states arrive, rather than asserting an exact emission count, for any test that races through more than two states.
