# Android Architecture Target

This document is the durable target architecture for Argus Lens. It describes where the Android app should converge over multiple verified slices; it does not claim the current codebase already has every target module or layer.

## Purpose and Scope

Argus Lens owns Android runtime behavior: Compose UI, lifecycle-aware state, permissions, local persistence, sync UX, messaging/payment screens, and future wearable/HUD capture flows. The target architecture keeps those responsibilities Android Studio-friendly while making feature ownership, session security, offline data, and verification boundaries explicit.

This file owns the long-term Android structure and migration roadmap. `PLAN.md` owns active work. `docs/android-modernization-progress.md` owns verified progress evidence. `docs/project-plan.md` owns product and stage architecture background.

## Current Baseline

The current project is a five-module modular baseline:

```text
argus-lens/
├── app/      app shell, Navigation Compose host, Hilt entry wiring, app tests
├── feature/  package-level auth, inbox, chat, contacts, wallet, call, realtime flows
├── data/     repositories, Room, DataStore, Retrofit/OkHttp, media/sync/session clients
├── model/    shared Parcelable/domain models
└── ui/       theme and reusable UI primitives
```

The app shell has already moved past a single monolithic route host milestone:

- `AppRouteHostState` and `AppRouteHostCallbacks` narrow the host API.
- `AppRouteUiState` builds route-facing UI state outside `AppRouteHost`.
- `AppRouteRuntimes` centralizes route runtime construction with ViewModel-owned coroutine scope.
- `AppRouteNavGraph` centralizes enum-backed Navigation Compose registration.
- `AppRouteActionBindings` centralizes route request/callback/action adapters outside the Compose host.
- `WalletActionHandler` keeps wallet action reduction/effect dispatch in the wallet feature package.
- `WalletRequestRunner` keeps wallet async request freshness and invalidation in the wallet feature package.
- `ArgusLensAppState` owns root UI state and pure session transition helpers.

That baseline is intentionally preserved while the project migrates toward the target shape below.

## Target Module Topology

The terminal topology is an Android Studio-compatible `:app` shell with shared `:core:*` modules and independently owned `:feature:*` modules.

```text
:app
  Composition root, root NavHost, process-wide Hilt entry points, WorkManager setup,
  release configuration, app-level smoke tests.

:core:model
  Shared immutable domain/UI models that need to cross module boundaries.

:core:designsystem
  Theme, typography, spacing, icons, tokens, previews, and reusable design primitives.

:core:ui
  Shared Compose widgets that are not product features and do not own business state.

:core:navigation
  Typed route contracts, nested graph contracts, and navigation argument helpers.

:core:data
  Repository contracts and implementations that coordinate local and remote data sources.

:core:network
  Retrofit/OkHttp clients, DTOs, service interfaces, auth interceptors, SSE adapters.

:core:database
  Room database, DAOs, entities, schema exports, migrations.

:core:datastore
  DataStore-backed preferences and non-secret durable app settings.

:core:session
  Session identity, credential boundary, token crypto adapters, restore/clear semantics.

:core:sync
  WorkManager request factories, sync workers, durable outbound queue policy.

:core:media
  Android media capture/upload abstractions that stay above Retina/JNI details.

:core:testing
  Fakes, fixtures, coroutine rules, repository doubles, navigation test helpers.

:feature:auth
:feature:inbox
:feature:chat
:feature:contacts
:feature:wallet
:feature:call
:feature:realtime
:feature:media
:feature:hud
  Feature-owned Route, Screen, ViewModel, reducer/state/effect, use cases, and tests.
```

### Dependency Direction

```text
:app
  -> :feature:*
  -> :core:*

:feature:*
  -> :core:model
  -> :core:ui / :core:designsystem
  -> :core:navigation
  -> :core:data or feature-specific use case contracts

:core:data
  -> :core:model
  -> :core:network
  -> :core:database
  -> :core:datastore
  -> :core:session

:core:ui / :core:designsystem / :core:model
  -> no app, data, network, database, or feature modules
```

Rules:

- Feature modules never depend on other feature modules directly. Shared cross-feature state moves to `:core:*` only when two real owners need it.
- UI modules never depend on Retrofit services, Room DAOs, DataStore instances, or token crypto.
- `:app` may compose the graph, but feature behavior should live in feature routes/ViewModels/use cases.
- Module splitting is staged. Do not create a module only to move one file unless it removes a concrete ownership or build-time problem.

## Layering Contract

Each feature should converge on this vertical slice:

```text
FeatureRoute
  - navigation-aware composable
  - obtains ViewModel with Hilt
  - collects lifecycle-aware UI state
  - maps user callbacks to ViewModel actions
  - emits navigation callbacks upward

FeatureScreen
  - stateless Compose UI
  - receives immutable state and event lambdas
  - contains previews and UI-only branching

FeatureViewModel
  - @HiltViewModel constructor injection
  - exposes StateFlow<FeatureUiState>
  - consumes use cases/repositories
  - owns screen-level coroutine work and SavedStateHandle route arguments

UseCase
  - optional, added when logic is reused or composes multiple repositories
  - small class with one public operation, usually operator fun invoke(...)
  - no pass-through use cases that only call one repository method unchanged

Repository
  - public data-layer API
  - coordinates local/remote/session data sources
  - exposes streams for durable app data where useful

DataSource
  - talks to exactly one storage/network/session mechanism
  - examples: Room DAO adapter, Retrofit service adapter, DataStore adapter, token crypto adapter
```

State and events:

- Durable screen state belongs in immutable `UiState` data classes.
- One-off effects remain explicit sealed models until multiple independent app-wide producers require a shared visual queue.
- Compose screens do not receive `NavController`. They expose callbacks such as `onConversationClick(id)`.
- Token material never enters Parcelable, saveable Compose state, logs, previews, or screenshots.

## Navigation Target

Current state: `AppRouteNavGraph` centralizes enum-backed Navigation Compose registration.

Target state: type-safe Navigation Compose routes backed by Kotlin Serialization, nested feature graph registration, and route argument decoding in route/ViewModel boundaries.

Example target shape:

```kotlin
@Serializable
data object InboxRoute

@Serializable
data class ChatRoute(val conversationId: String)

fun NavGraphBuilder.chatGraph(
    onBack: () -> Unit,
) {
    composable<ChatRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<ChatRoute>()
        ChatRoute(conversationId = route.conversationId, onBack = onBack)
    }
}
```

Migration rule: keep the current enum graph covered while introducing typed route contracts one feature at a time. Do not remove the existing graph until every declared `AppRoute` has a typed equivalent and regression coverage.

## Session and Token Boundary Target

Current state: UI identity state and credential persistence are separated enough to keep access/refresh tokens out of saveable UI state.

Target state:

```text
SessionIdentityStore
  - account id, display name, authenticated/anonymous state
  - safe to expose as UI identity snapshot

SecureTokenStore
  - access/refresh token persistence boundary
  - never exposed to Compose state or Parcelable models

TokenCipher / SecureKeyProvider
  - Android Keystore-backed key management
  - encryption/decryption implementation detail

SessionRepository
  - restore, authenticate, refresh, clear
  - maps credentials to safe identity snapshots for UI
```

Rules:

- DataStore can persist small durable state, but it is not treated as a secure secret store by itself.
- Android Keystore-backed crypto remains isolated behind a small interface.
- Process-death restoration restores the selected safe entry context, not bearer tokens or sensitive payloads.

## Data, Offline, and Sync Target

The target data layer follows repository/data-source separation:

```text
ConversationRepository
  -> ConversationLocalDataSource / Room DAO
  -> ConversationRemoteDataSource / Retrofit service
  -> SyncCursorDataSource / DataStore or Room

MessageRepository
  -> MessageLocalDataSource / Room DAO
  -> MessageRemoteDataSource / Retrofit service
  -> OutboundMessageQueueDataSource / Room

MediaRepository
  -> MediaRemoteDataSource / upload-download service
  -> MediaFileDataSource / Android file access boundary
  -> MediaQueueDataSource / Room-backed retry state
```

Rules:

- Local durable state is written before network-dependent reconciliation when a flow must survive process death or weak networks.
- WorkManager handles constrained retry/reconciliation; foreground refresh must not silently replace durable sync policy.
- Room schema changes require exported schema updates and migration tests before increasing `CURRENT_VERSION`.
- Filename sanitization, content capability checks, and media upload contracts stay in the data/media boundary, not in Compose UI.

## Hilt and Runtime Ownership Target

- `@HiltAndroidApp` remains in the application class.
- Activities and workers use Android-supported Hilt entry points.
- Feature ViewModels use `@HiltViewModel` and constructor injection.
- App-level singleton bindings remain in app or core DI modules only when the dependency is truly app-wide.
- Feature-owned use cases and repositories should move toward feature/core scoped modules instead of a growing `AppDependencies` aggregate.
- Long-running session, realtime, sync, and timer work is owned by ViewModels, workers, or repositories, not by Composable coroutine scopes.

## Test Architecture Target

Target pyramid:

```text
Many local JVM tests
  - reducers and state transitions
  - ViewModels with fake repositories/use cases
  - use cases
  - repository/data-source mapping
  - filename, DTO, token/session boundary tests

Some component/integration tests
  - Room DAO and migration tests
  - DataStore/session tests
  - feature route/screen tests where behavior is UI-specific
  - WorkManager task tests

Few device/emulator tests
  - app launch smoke
  - critical navigation flows
  - permission/capture/runtime behavior that needs Android framework execution
```

Testing rules:

- Prefer handwritten fakes and test doubles over mocking implementation details.
- Every module split must move or add tests with the code it owns.
- Boundary tests guard forbidden regressions: token material in UI state, direct Compose-to-JNI calls, release URL weakening, ad hoc route strings, and data-source access from screens.
- Gradle tasks run serially in this workspace.

## Migration Roadmap

### P0 — Shell and Security Boundaries

Goal: keep the app safe and navigable while shrinking the app shell.

- Preserve the token/session UI-state boundary.
- Continue slimming `AppRouteHost` and `ArgusLensApp` behind explicit state/callback/runtime objects.
- Move feature-owned state and actions out of the root ViewModel when a feature has an independent lifecycle boundary.
- Keep route behavior covered by JVM source-boundary and navigation policy tests.

Exit evidence:

- `:app:testDebugUnitTest --tests "com.kzzz3.argus.lens.app.ArgusLensAppViewModelTest"`
- `:app:testDebugUnitTest --tests "com.kzzz3.argus.lens.app.AppRouteNavigationRuntimeTest"`
- Source-boundary checks for saveable UI state and host API shape.

### P1 — Typed Navigation and Lifecycle Ownership

Goal: move from centralized enum registration to typed route contracts without losing coverage.

- Introduce typed route contracts in a small navigation boundary.
- Convert one feature route at a time to typed arguments.
- Replace Composable-owned long-running scopes with ViewModel/repository/worker ownership.
- Decide and document process-death restoration for selected conversation and session entry context.

Exit evidence:

- Route registration coverage for typed and legacy routes during transition.
- ViewModel/runtime ownership regression tests.
- Targeted process-death/session-entry tests where state is persisted.

### P2 — Data, Session, and Use Case Boundaries

Goal: make repositories and data sources explicit enough to support offline-first messaging/media/payment flows.

- Split `LocalSessionStore` into identity, token, and crypto responsibilities.
- Introduce use cases only around reused or multi-repository operations.
- Separate network, database, DataStore, session, and media data-source adapters within the current module before extracting Gradle modules.
- Add durable outbound queue decisions for message/media upload flows that must survive process death.

Exit evidence:

- Session/token boundary tests.
- Room migration/schema tests when schemas change.
- Repository/data-source tests with fakes.
- WorkManager retry and enqueue policy tests.

### P3 — Module Extraction and Release Boundaries

Goal: extract modules only after package boundaries are stable.

- Extract low-risk core modules first: `:core:model`, `:core:designsystem` or `:core:ui`, then `:core:navigation`.
- Extract data infrastructure modules after package-level data-source boundaries exist: `:core:network`, `:core:database`, `:core:datastore`, `:core:session`.
- Split aggregate `:feature` into `:feature:*` modules when feature ViewModels/routes/use cases are already independent.
- Move shared Gradle configuration toward convention plugins when repeated module boilerplate becomes a maintenance problem.
- Preserve release hardening: R8, shrink resources, non-production HTTPS base URL checks, backup exclusions.

Exit evidence:

- `settings.gradle.kts` includes only modules with clear ownership.
- Module boundary regression tests still pass.
- `lint` and `assembleDebug` pass after module wiring changes.

### P4 — Architecture Enforcement and Runtime Maturity

Goal: keep the final shape clean as Stage 2 wearable/HUD behavior grows.

- Expand architecture tests to enforce dependency direction and forbidden imports.
- Add `:core:testing` when fakes/fixtures are shared by multiple modules.
- Keep instrumentation smoke coverage for launch/navigation and add runtime-specific tests for permissions/capture/HUD flows.
- Keep Retina/JNI access behind repository/service boundaries with capability probing.
- Periodically remove stale packages, docs, dead tests, and transitional shims after each successful migration slice.

Exit evidence:

- Full serial verification: `testDebugUnitTest`, `lint`, `assembleDebug`, and instrumentation APK build when UI/runtime paths change.
- Documentation reflects current module names and no longer describes removed transitional APIs.

## Official Reference Baseline

The target follows current Android guidance and sample architecture patterns:

- Android app architecture: https://developer.android.com/topic/architecture
- UI layer and state holders: https://developer.android.com/topic/architecture/ui-layer
- Compose architecture and unidirectional data flow: https://developer.android.com/develop/ui/compose/architecture
- Domain layer guidance: https://developer.android.com/topic/architecture/domain-layer
- Data layer and offline-first guidance: https://developer.android.com/topic/architecture/data-layer
- Modularization guide: https://developer.android.com/topic/modularization
- Common modularization patterns: https://developer.android.com/topic/modularization/patterns
- Navigation type safety: https://developer.android.com/guide/navigation/design/type-safety
- Multi-module navigation: https://developer.android.com/guide/navigation/integrations/multi-module
- Hilt and Jetpack integration: https://developer.android.com/training/dependency-injection/hilt-jetpack
- DataStore: https://developer.android.com/topic/libraries/architecture/datastore
- Android Keystore: https://developer.android.com/privacy-and-security/keystore
- Android testing strategy: https://developer.android.com/training/testing/fundamentals/strategies
- Now in Android architecture notes: https://github.com/android/nowinandroid/blob/main/docs/ArchitectureLearningJourney.md
- Now in Android modularization notes: https://github.com/android/nowinandroid/blob/main/docs/ModularizationLearningJourney.md

## Non-Goals

- Lens does not own cloud routing, settlement, final authorization, or server-side policy.
- Lens does not implement heavy media filtering or secure signing internals directly in Kotlin UI/ViewModels.
- Compose UI does not call JNI/native primitives directly.
- The migration does not require premature module splitting before ownership pressure and package boundaries justify it.

## Cleanup Rules

Every migration slice completes only after cleanup:

1. Remove stale package docs, obsolete shims, unused imports, and dead tests introduced by the slice.
2. Update `README.md`, `PLAN.md`, and `docs/android-modernization-progress.md` when current commands, module names, verification gates, or risk status change.
3. Keep this document focused on target architecture and roadmap; keep verified status in the progress ledger.
4. Verify changed docs by reading them end-to-end and checking referenced paths/commands against the repository.
