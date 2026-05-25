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
             MockBookData.kt      — hardcoded JSON (no real network)

module/
  network/   BooksRemoteRepository.kt  — @Singleton; decodes MockBookData JSON; simulates delay
  local/     BookDao.kt                — Room DAO (Flow<List<BookEntity>>, upsert, getById)
             BooksDatabase.kt          — Room database definition
             BooksCacheRepository.kt   — wraps BookDao; injected via @Inject constructor
  mapper/    BookMapper.kt             — Book↔BookEntity↔BookUiModel extension functions

usecase/
  LoadBooksUseCase.kt      — emits cached list first, then refreshed list after remote fetch
  GetBookDetailUseCase.kt  — cache-first lookup; falls back to remote and saves result

di/
  BooksDatabaseModule.kt   — provides BooksDatabase and BookDao (SingletonComponent)

navigation/
  BooksNavRoutes.kt        — @Serializable route objects (BooksListRoute, BookDetailRoute)
  BooksNavGraph.kt         — wires BooksListRoot / BookDetailRoot to nav routes

ui/
  UiState.kt           — sealed interface UiState<T> (Loading / Success<T> / Error(UiText))
  UiText.kt            — sealed interface UiText + asString() extension
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

**Offline-first flow** (`LoadBooksUseCase`): sequential `flow { }` that (1) emits the current Room cache immediately, then (2) fetches from remote, saves to cache, and emits the refreshed cache. Do not change this to `channelFlow + launch` — it causes non-deterministic ordering in tests with `StandardTestDispatcher`.

**`BooksListViewModel` state pipeline**: `_sortOrder.flatMapLatest { loadBooksUseCase(it) }` combined with `_viewMode`, `_searchQuery`, `_isSearchActive` flows via `combine`, producing `UiState<BooksListState>` via `stateIn(WhileSubscribed(5000))`. Sort order change re-triggers the use case; the cache emits immediately so there is no Loading flash between sort changes.

**`BookDetailViewModel`**: reads `bookId` directly from `SavedStateHandle["bookId"]` — do not use `toRoute<>()` (requires Navigation backstack context, unavailable in unit tests).

**Genres serialization**: `Book.genres: List<String>` → stored in Room as a comma-joined `String` in `BookEntity.genres` → split back on read in `BookMapper`. No TypeConverter is registered.

**Image loading**: Coil 3.x (`coil3:coil-compose`, `coil3:coil-network-okhttp`). Use `AsyncImage` with `rememberVectorPainter(Icons.AutoMirrored.Filled.MenuBook)` as placeholder/error painter.

**Navigation naming**: `BooksListRoute` / `BookDetailRoute` in `BooksNavRoutes.kt` are the `@Serializable` route objects used as type parameters in `composable<T>`. The root composables are named `BooksListRoot` / `BookDetailRoot` — these are different things and must stay distinct.

### Testing approach

Fakes over mocks — no Mockk usage in existing tests despite it being on the classpath:
- `FakeBookDao` — `MutableStateFlow`-backed; has `seed(List<BookEntity>)` to pre-populate
- `FakeBooksRemoteRepository` — extends `BooksRemoteRepository()`; exposes `var books` and `var shouldThrow`
- `FakeLoadBooksUseCase` / `FakeGetBookDetailUseCase` — extend the real use case classes and override `operator fun invoke`

All ViewModel tests set `Dispatchers.setMain(UnconfinedTestDispatcher())` in `@Before` and reset in `@After`. Use Turbine's `Flow.test { }` for asserting emissions. Access state via `viewModel.state` (not `uiState`).

**StateFlow conflation gotcha**: if a fake `suspend fun` has no real suspension points, `Loading → Success` can happen in one tick and the intermediate `Loading` emission is dropped by StateFlow conflation. Add `yield()` inside the fake to give collectors a chance to observe transient states (see `FakeGetBookDetailUseCase`).
