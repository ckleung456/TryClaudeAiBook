# TryClaudeAiBook

An Android app for browsing a catalog of books, built as a multi-module MVVM sample with an offline-first data layer.

## Features

- Browse books in a switchable **list / grid** view
- Search and sort the book catalog
- View book details
- **Offline-first**: the local cache (Room) is shown immediately while fresh data is fetched in the background

## Tech stack

- **Kotlin** + **Jetpack Compose** (Material 3)
- **Hilt** for dependency injection
- **Room** for local persistence
- **Kotlinx Serialization** for JSON decoding
- **Coil 3** for image loading
- **Navigation Compose** with type-safe routes
- **Coroutines / Flow** for async and reactive state
- **JUnit + Turbine** for testing (fakes over mocks — no MockK usage despite it being on the classpath)

## Project structure

The project is split into four Gradle modules:

```
:app                     Entry point (MainActivity, MyApplication), Material3 theme. No business logic.
:core:domain             Pure Kotlin. Shared error types: Result<D, E>, EmptyResult<E>, DataError.
:core:presentation       Android + Compose. Shared UI plumbing: UiState<T>, UiText, UiStatefulContent, ObserveAsEvents.
:features:feature-book   The complete books feature (depends on both core modules).
```

Feature modules never depend on each other directly — anything shared between features belongs in `:core:domain` or `:core:presentation`.

### `feature-book` layer structure

```
model/     network / local / domain models (Book, BookEntity, BookUi, SortOrder, ViewMode)
module/    repositories (remote + local/Room) and mappers
usecase/   FlowUseCase-based use cases (LoadBooksUseCase, GetBookDetailUseCase)
di/        Hilt modules
navigation/ type-safe nav routes and graph wiring
ui/        list/ and detail/ screens, each following the Action / Event / UiState (MVVM) contract
```

Every screen follows the same pattern: a `ViewModel` exposes `state: StateFlow<UiState<T>>` and a single `onAction(Action)` entry point; one-time side effects (e.g. navigation) flow through an `events: Flow<Event>` channel observed by a stateless `*Root` composable, which delegates rendering to a stateless `*Screen` composable.

See [CLAUDE.md](CLAUDE.md) for a full architectural deep-dive, including the offline-first data flow, the `FlowUseCase` base class, and testing conventions.

## Requirements

- Android Studio with JDK 21 bundled (required — the system JDK is too old for Hilt 2.57.2 / AGP 8.x)
- `compileSdk` / `targetSdk` 36, `minSdk` 24

## Build & test

```bash
# Build everything
./gradlew :app:build

# Run the feature-book unit tests
./gradlew :features:feature-book:test

# Run a single test class
./gradlew :features:feature-book:testDebugUnitTest --tests "com.example.featureBook.usecase.LoadBooksUseCaseTest"
```

## Notes

- Book data is served from a hardcoded JSON mock (`MockBookData.kt`) rather than a real network call — there's a simulated delay to exercise loading states.
