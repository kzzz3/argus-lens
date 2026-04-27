# Android Modernization Progress

This document tracks the Android modernization backlog by priority. Update it whenever a slice reaches a verified checkpoint. The long-term `:app` / `:core:*` / `:feature:*` Android architecture target and migration roadmap live in `docs/android-architecture-target.md`; this file remains the evidence ledger for completed or deliberately deferred checkpoints.

## P0 Token Boundary

Status: complete for the access/refresh token UI-state boundary.

- `accessToken` and `refreshToken` are owned by `data/session/SessionCredentials` and persisted through `SessionRepository` implementations.
- `AppSessionState` remains a Parcelable UI identity snapshot with only authentication status, account id, and display name.
- `rememberSaveable` is guarded by regression tests so access/refresh tokens cannot be reintroduced into saveable Compose state.
- Parcelable UI state in `feature` and `model` is guarded by regression tests so access/refresh tokens cannot be added there.
- Android backup is disabled and both full-backup and data-extraction rules exclude shared preferences, databases, and DataStore files.

Verification gate:

- `:app:testDebugUnitTest` must pass after changes to session state, token storage, manifest backup settings, or saveable UI state.

Remaining security review items are release-hardening work, not blockers for this P0 boundary: evaluate token rotation, device-lock policy, and whether to replace the custom Android Keystore AES/GCM wrapper with Jetpack Security.

## P0 AppRouteHost Decomposition

Status: complete for the first God Composable split milestone and the host API boundary slice.

- `AppRouteHost.kt` no longer owns route UI-state derivation; `AppRouteUiState.kt` builds the per-screen state bundle.
- `AppRouteHost.kt` no longer constructs every route/runtime inline; `AppRouteRuntimes.kt` centralizes runtime creation with Compose `remember` boundaries.
- `AppRouteHost.kt` no longer embeds the full `NavHost` route-to-screen graph; `AppRouteNavGraph.kt` owns route registration and screen binding.
- Host-only shell policy for `NewFriends` and missing-chat fallback moved into `AppRouteNavigationRuntime.resolveRouteShellDestination` with JVM regression coverage.
- `ArgusLensApp.kt` now passes app-shell state and callbacks through `AppRouteHostState` / `AppRouteHostCallbacks`, keeping route behavior unchanged while narrowing the `AppRouteHost` API.
- `ArgusLensAppState.kt` owns the root UI state model and pure session transition helpers so `ArgusLensAppViewModel.kt` can focus on state mutation and runtime scope ownership.
- `AppRouteActionBindings.kt` owns route request/callback/action adapters so `AppRouteHost.kt` no longer declares feature action factories inline.
- `feature/wallet/WalletActionHandler.kt` owns wallet action reduction/effect dispatch, replacing the app-owned wallet action runtime bridge.
- `feature/wallet/WalletRequestRunner.kt` and `WalletRequestGuard.kt` own wallet async request freshness and invalidation rules, replacing the app-owned wallet request runtime.
- `feature/wallet/WalletEffectHandler.kt` owns wallet effect dispatch and request launch rules; `app/WalletRouteRuntime.kt` remains only the app navigation adapter for wallet route changes.
- This boundary is the baseline for the future typed-navigation and feature ViewModel extraction roadmap in `docs/android-architecture-target.md`.

Verification gate:

- `:app:compileDebugKotlin` must pass after route host decomposition changes.
- `:app:testDebugUnitTest --tests "com.kzzz3.argus.lens.app.AppRouteNavigationRuntimeTest"` must pass after shell routing policy changes.
- `:app:testDebugUnitTest --tests "com.kzzz3.argus.lens.app.ArgusLensAppViewModelTest"` must pass after app-shell state boundary changes.
- Route action binding changes must also keep `EntryRouteRuntimeTest`, `ContactsRouteRuntimeTest`, `WalletRouteRuntimeTest`, `WalletActionHandlerTest`, `WalletRequestRunnerTest`, `WalletRequestGuardTest`, `WalletEffectHandlerTest`, and `RealtimeConnectionRuntimeTest` green.

Remaining lifecycle ownership work belongs to P1: long-running realtime/session refresh/call timer orchestration should continue moving out of the Composable layer.

## P1 ViewModel And Runtime Lifecycle Ownership

Status: complete for app-shell state collection and long-lived runtime scope ownership.

- `ArgusLensApp.kt` collects root app state with `collectAsStateWithLifecycle`.
- `ArgusLensAppViewModel` owns the root `StateFlow` and exposes `runtimeScope` backed by `viewModelScope`.
- `AppRouteHost.kt` no longer creates `rememberCoroutineScope`; route runtimes receive the ViewModel-owned scope through `rememberAppRouteRuntimes`.
- A JVM source-boundary regression test prevents reintroducing `rememberCoroutineScope` into `AppRouteHost.kt`.

Verification gate:

- `:app:testDebugUnitTest --tests "com.kzzz3.argus.lens.app.ArgusLensAppViewModelTest"` must pass after lifecycle/runtime ownership changes.

## P1 Navigation Compose

Status: complete for the current app graph baseline.

- `AppRouteNavGraph.kt` owns the Navigation Compose `NavHost` and all `composable(AppRoute.*.name)` registrations.
- `AppRouteNavigationRuntime` owns shell tab mapping and special shell policy for `NewFriends` and missing-chat fallback.
- JVM regression coverage verifies shell policy and that every declared `AppRoute` is registered in `AppRouteNavGraph`.

Typed route arguments and nested graphs remain future refinements; the current enum-backed graph is now centralized and covered for the app's declared route set. The target route-contract migration sequence is documented in `docs/android-architecture-target.md`.

## P2 Hilt Dependency Injection

Status: complete for the current module baseline.

- `ArgusLensApplication` is annotated with `@HiltAndroidApp` and supplies `HiltWorkerFactory` for WorkManager.
- `MainActivity` uses `@AndroidEntryPoint` and `ArgusLensAppViewModel` uses `@HiltViewModel`.
- `ArgusLensAppModule` wires repositories, coordinators, schedulers, and app dependencies from the singleton component.

Remaining refinement: feature-specific ViewModels can be introduced when feature modules need independent lifecycle/state ownership.

## P2 Room Schema And Migration Strategy

Status: complete for the v1 persistence baseline.

- `ArgusLensDatabase` uses `exportSchema = true` and `CURRENT_VERSION = 1`.
- `ArgusLensMigrations` is the central migration registry and tracks the database version.
- `ArgusLensDatabaseMigrationTest` verifies the exported current schema and current v1 tables.
- Historical migration tests become required before increasing `CURRENT_VERSION` beyond 1.

Verification gate:

- `:data:testDebugUnitTest --tests "com.kzzz3.argus.lens.data.local.ArgusLensDatabaseMigrationTest"` must pass after Room entity, schema, or migration changes.

## P2 WorkManager Background Sync

Status: complete for the current reliable one-time sync baseline.

- `BackgroundSyncWork` creates constrained network-only work with exponential backoff.
- `BackgroundSyncTask` maps transient authenticated sync exceptions to `BackgroundSyncResult.Retry` while preserving coroutine cancellation.
- `BackgroundSyncWorker` maps retry results to WorkManager `Result.retry()`.
- Existing unique work now uses `ExistingWorkPolicy.KEEP` so an in-flight retry/backoff chain is not replaced by duplicate enqueue requests.

Verification gate:

- `:app:testDebugUnitTest --tests "com.kzzz3.argus.lens.worker.BackgroundSyncWorkTest"` must pass after WorkManager enqueue policy changes.
- `:app:testDebugUnitTest --tests "com.kzzz3.argus.lens.worker.BackgroundSyncTaskTest"` and `BackgroundSyncWorkerTest` must pass after sync result changes.

Remaining future expansion: a durable outbound queue for media/message uploads should be built on top of a new Room schema version and migration, not hidden inside the v1 baseline.

## P3 Release Hardening

Status: complete for repository-enforced release baseline.

- Release builds require `ARGUS_RELEASE_BASE_URL` and reject localhost, emulator, debug-only fallback, non-HTTPS, or credential-bearing URLs.
- Release builds enable R8 minification, resource shrinking, and the optimized default ProGuard file.
- Backup and data extraction rules exclude session persistence areas.
- Source-level regression tests guard release shrink/base URL requirements.

## P3 Module Boundaries

Status: complete for current product scale.

- The Android project is split into `:app`, `:data`, `:feature`, `:model`, and `:ui`.
- `:app` depends on lower modules; `:data`, `:model`, and `:ui` remain explicit modules instead of hidden source sets.
- Further feature granularity such as `:feature:chat` or `:core:network` should be driven by future ownership pressure, not premature splitting.
- The durable extraction roadmap for `:core:*` and `:feature:*` modules lives in `docs/android-architecture-target.md`.

## P3 Event Model

Status: complete for the current reducer/effect architecture.

- Feature one-off work is represented by sealed `*Effect` models; feature handlers own feature-specific dispatch/request policy, while app route runtimes only adapt app-owned navigation where needed.
- Source-level regression tests ensure effect files stay sealed instead of becoming ad hoc strings or nullable status blobs.
- User-visible durable state remains in feature state objects; one-off effects remain explicit reducer outputs.

Future refinement: introduce a root snackbar `SharedFlow` only when multiple independent app-wide event producers need a single visual queue.

## P4 Test Structure

Status: complete for the modernization regression baseline.

- P0 token/session boundaries are guarded by `SessionBoundaryTest`.
- P0/P1 route and Navigation Compose policy are guarded by `AppRouteNavigationRuntimeTest` and app compilation.
- P1 ViewModel/runtime ownership is guarded by `ArgusLensAppViewModelTest`.
- P2 Room migration/schema work is guarded by `ArgusLensDatabaseMigrationTest`.
- P2 WorkManager behavior is guarded by `BackgroundSyncWorkTest`, `BackgroundSyncTaskTest`, and `BackgroundSyncWorkerTest`.
- P3 release/module/event boundaries are guarded by `ReleaseAndModuleBoundaryTest` and `EventModelBoundaryTest`.
- Media contract and download filename boundaries are guarded by `MediaApiModelsTest` and `MediaFileNameTest`.
- The instrumentation baseline now uses `ArgusLensLaunchInstrumentedTest` instead of Android template tests.
- `ModernizationCoverageTest` guards the presence of these regression gates and this progress document.

Final verification gate for this modernization pass:

- `:app:testDebugUnitTest`
- `:data:testDebugUnitTest`
- `testDebugUnitTest`
- `lint`
- `assembleDebug`
- `:app:assembleDebugAndroidTest`
