# Android Modernization Progress

This document tracks the Android modernization backlog by priority. Update it whenever a slice reaches a verified checkpoint. The long-term `:app` / `:core:*` / `:feature:*` Android architecture target and migration roadmap live in `docs/android-architecture-target.md`; this file remains the evidence ledger for completed or deliberately deferred checkpoints.

## P0 Token Boundary

Status: complete for the access/refresh token UI-state boundary.

- `accessToken` and `refreshToken` are owned by `core/session/SessionCredentials` and persisted through `SessionRepository` implementations.
- `LocalSessionStore` remains the concrete `SessionRepository` facade, but now delegates safe identity persistence to `LocalSessionStateStore` and encrypted token persistence to `LocalSessionCredentialsStore` so identity and credential responsibilities can evolve independently.
- `AppSessionState` remains a Parcelable UI identity snapshot with only authentication status, account id, and display name.
- Process-death entry restoration uses token-free `AppRestorableEntryContext` state only: account id, stable app route string, and selected conversation id. `SavedStateHandle` must not persist bearer tokens, refresh tokens, message bodies, media payloads, or conversation objects.
- `rememberSaveable` is guarded by regression tests so access/refresh tokens cannot be reintroduced into saveable Compose state.
- `SavedStateHandle` restore keys are guarded by regression tests so access/refresh tokens cannot be reintroduced into process-death state.
- Parcelable UI state in `:feature` and `:core:model` is guarded by regression tests so access/refresh tokens cannot be added there.
- Android backup is disabled and both full-backup and data-extraction rules exclude shared preferences, databases, and DataStore files.

Verification gate:

- `:app:testDebugUnitTest` must pass after changes to session state, token storage, manifest backup settings, or saveable UI state.
- `:app:testDebugUnitTest --tests "com.kzzz3.argus.lens.app.SessionBoundaryTest"` must pass after changing `SavedStateHandle`, Parcelable, or saveable state boundaries.
- The relevant `:core:datastore` session tests must pass after local session store responsibility split changes.

Remaining security review items are release-hardening work, not blockers for this P0 boundary: evaluate token rotation, device-lock policy, and whether to replace the custom Android Keystore AES/GCM wrapper with Jetpack Security.

## P0 AppShellHost Decomposition

Status: complete for the first God Composable split milestone and the host API boundary slice.

- `AppShellHost.kt` no longer owns route UI-state derivation; `AppShellUiState.kt` builds the per-screen state bundle.
- `AppShellHost.kt` receives ViewModel-owned feature state holders and constructs route handlers behind Compose `remember` boundaries.
- `AppShellHost.kt` no longer embeds the full `NavHost` route-to-screen graph; `navigation/ArgusNavHost.kt` owns the root graph and delegates leaf registration to auth/main child graph helpers.
- Root navigation now lives in `navigation/ArgusNavHost.kt`; `AuthGraphRoute` and `MainGraphRoute` separate login/register from the authenticated shell, and `TopLevelDestination.kt` models only main shell tabs.
- Login/register route registration lives in `feature/auth/navigation/AuthNavigation.kt`; main leaf registrations live in feature-owned navigation files for inbox/chat, contacts, call, wallet, and me.
- `AppShellHost.kt` no longer declares route lifecycle side effects inline; `AppShellEffects.kt` owns hydration, persistence, session boundary, realtime connection, route loading, disposal, and navigation-sync effects.
- Host-only shell policy for `NewFriends` and missing-chat fallback moved into `AppRouteNavigator.resolveRouteShellDestination` with JVM regression coverage.
- `ArgusLensApp.kt` now passes app-shell state and callbacks through `AppShellState` / `AppShellCallbacks`, while feature-state mutation callbacks are derived inside the host via `AppFeatureCallbacks`.
- `ArgusLensAppState.kt` owns the root UI state model and pure session transition helpers so `ArgusLensAppViewModel.kt` can focus on state mutation and ViewModel-scope ownership.
- `AppActionDispatcher.kt` owns route request/callback/action adapters so `AppShellHost.kt` no longer declares feature action factories inline.
- `feature/wallet/WalletActionHandler.kt` owns wallet action reduction/effect dispatch, replacing the former app-owned wallet action bridge.
- `feature/wallet/WalletRequestRunner.kt` and `WalletRequestGuard.kt` own wallet async request freshness and invalidation rules.
- `feature/wallet/WalletEffectHandler.kt` owns wallet effect dispatch and request launch rules.
- `feature/wallet/WalletFeatureController.kt` composes wallet action reduction with effect handling; app code supplies only root state, session freshness, and navigation callbacks.
- `feature/wallet/WalletStateHolder.kt` owns wallet screen `StateFlow` state, request invalidation, account binding, and wallet action dispatch; `ArgusLensAppViewModel` owns the holder lifetime so requests and state share the same ViewModel scope, while `ArgusLensAppUiState` no longer stores wallet feature state.
- `feature/auth/AuthStateHolder.kt` owns login/register form `StateFlow` state, reducer dispatch, and async auth submission; `ArgusLensAppViewModel` owns the holder lifetime, while app code keeps only route navigation and authenticated-session application callbacks.
- `feature/inbox/InboxStateHolder.kt` owns derived inbox `InboxUiState` and route-agnostic `InboxAction` dispatch; `ArgusLensAppViewModel` owns the holder lifetime, while app code still owns shared `ConversationThreadsState`, conversation-open sequencing, route changes, sign-out/session effects, realtime, persistence, and chat behavior.
- `feature/inbox/ChatStateHolder.kt` owns selected-conversation `ChatState` and `ChatUiState` derivation; `ArgusLensAppViewModel` owns the holder lifetime, while app code still owns chat action effects, call routing, outgoing dispatch, recall/download side effects, selected conversation routing, shared `ConversationThreadsState`, realtime, and persistence.
- `app/navigation/AppRouteContract.kt` owns stable route descriptors for every `AppRoute`; app host navigation now builds typed route targets through that contract instead of relying on enum names.
- `ArgusNavHost` / `mainGraph` now accept `ArgusNavRoutes` / `MainRoutes` route bundles and delegate leaf registration to feature `*Navigation.kt` files.
- `AppShellEffects.kt` now receives `AppShellEffectDependencies` instead of the full `AppDependencies` aggregate, narrowing lifecycle/effects access to initial session data, credentials, the credential store, and the realtime client.
- This boundary is the baseline for the current typed route-bundle navigation shape and the remaining feature ViewModel extraction roadmap in `docs/android-architecture-target.md`.

Verification gate:

- `:app:compileDebugKotlin` must pass after route host decomposition changes.
- `:app:testDebugUnitTest --tests "com.kzzz3.argus.lens.app.AppRouteNavigatorTest"` must pass after shell routing policy changes.
- `NavigationGraphBoundaryTest` must pass after auth/main graph or feature-owned navigation registration changes.
- `AppRouteNavigatorTest.appRouteDescriptorsExposeStableRouteStringsAndGraphMetadata` must pass after route contract changes.
- `:app:testDebugUnitTest --tests "com.kzzz3.argus.lens.app.ArgusLensAppViewModelTest"` must pass after app-shell state boundary changes.
- `ArgusLensAppViewModelTest.appShellHost_delegatesLifecycleEffects` must pass after moving host lifecycle/effect orchestration.
- `ArgusLensAppViewModelTest.appShellEffectsUseNarrowDependencyBoundary` must pass after changing host lifecycle/effects dependency inputs.
- Route action binding changes must also keep `AuthStateHolderTest`, `InboxStateHolderTest`, `ChatStateHolderTest`, `InboxRouteHandlerTest`, `ChatRouteHandlerTest`, `ContactsRouteHandlerTest`, `WalletActionHandlerTest`, `WalletRequestRunnerTest`, `WalletRequestGuardTest`, `WalletEffectHandlerTest`, `WalletFeatureControllerTest`, `WalletStateHolderTest`, and `RealtimeConnectionManagerTest` green.

Remaining lifecycle ownership work belongs to P1: the wallet, auth, inbox, and chat state holders are feature-owned precursors to AndroidX/Hilt feature ViewModels, and long-running realtime/session refresh/call timer orchestration should stay outside the Composable layer. AndroidX typed routes now build on the stable `AppRouteContract` seam rather than coupling to enum names.

## P1 ViewModel And Lifecycle Ownership

Status: complete for app-shell state collection and long-lived ViewModel scope ownership.

- `ArgusLensApp.kt` collects root app state with `collectAsStateWithLifecycle`.
- `ArgusLensAppViewModel` owns the root `StateFlow` and exposes `appScope` backed by `viewModelScope`.
- `AppShellHost.kt` no longer creates `rememberCoroutineScope`; route handlers receive the ViewModel-owned scope from `ArgusLensAppViewModel`.
- A JVM source-boundary regression test prevents reintroducing `rememberCoroutineScope` into `AppShellHost.kt`.

Verification gate:

- `:app:testDebugUnitTest --tests "com.kzzz3.argus.lens.app.ArgusLensAppViewModelTest"` must pass after lifecycle/runtime ownership changes.

## P1 Navigation Compose

Status: complete for the current typed route-bundle app graph baseline.

- `navigation/ArgusNavHost.kt` owns the root Navigation Compose `NavHost`, with auth and main nested graphs beneath it.
- `ArgusNavHost` receives an `ArgusNavRoutes` bundle, `mainGraph` receives `MainRoutes`, and each feature navigation file receives its own `*Routes` contract instead of app-owned action/callback soup.
- `AppRouteNavigator` owns shell tab mapping and special shell policy for `NewFriends` and missing-chat fallback.
- JVM regression coverage verifies shell policy and that every declared `AppRoute` is registered in either the auth graph or a feature-owned main child navigation file.

Richer typed route arguments remain a future refinement; the current graph already uses stable route descriptors, auth/main nested graphs, and feature-owned route bundles for the app's declared route set. The target route-contract migration sequence is documented in `docs/android-architecture-target.md`.

## P2 Role Naming Boundary

Status: complete for the first role taxonomy and source-boundary guard.

- `docs/android-architecture-target.md` defines the role names `UseCase`, `Handler`, `Runner`, `Manager`, `Scheduler`, `Store`, `StateHolder`, and `Controller` before broad renames or ownership moves.
- Current names such as `AppRouteNavigator`, `PersistAppStateUseCase`, `RestoreAppSessionUseCase`, `CallSessionTimer`, `RealtimeConnectionManager`, `RealtimeReconnectScheduler`, `SessionCredentialsStore`, and `LocalSessionStore` are classified by role.
- `RoleNamingBoundaryTest` guards the taxonomy, keeps `AppRouteHandlers.kt` from constructing feature-owned state/handler/controller roles, and prevents feature state holders from taking navigation controller dependencies.

Verification gate:

- `:app:testDebugUnitTest --tests "com.kzzz3.argus.lens.app.RoleNamingBoundaryTest"` must pass after role naming or role taxonomy changes.

## P2 Focused UseCase Layer

Status: complete for the first non-pass-through use-case slice.

- `SendOutgoingChatMessageUseCase` owns outgoing chat send orchestration that composes draft media upload session creation, content upload, upload finalization, and conversation message send.
- `ChatUseCases` exposes outgoing send workflow through the use case while keeping chat state reduction, synchronization, recall, and attachment download roles feature-owned.
- The first slice deliberately avoids a broad domain module and avoids pass-through use cases for single repository calls such as wallet summary/history/receipt reads.

Verification gate:

- `:feature:testDebugUnitTest --tests "com.kzzz3.argus.lens.feature.inbox.SendOutgoingChatMessageUseCaseTest"` must pass after outgoing chat send/media workflow changes.

## P2 Repository And DataSource Boundaries

Status: in progress; complete for the first media download file-persistence seam.

- `MediaRepository` remains the public data-layer contract for media upload/download workflows.
- `RemoteMediaRepository` is now module-internal and keeps session token checks, Cortex API calls, response parsing, and repository result mapping.
- `MediaFileDataSource` owns Android download-directory/file-copy persistence for downloaded attachments, with `AndroidMediaFileDataSource` supplied by `createMediaRepository`.
- The first slice deliberately avoids a broad media repository rewrite, remote datasource extraction, durable media queue, or new Gradle module until more seams are proven.

Verification gate:

- The relevant `:core:data` media repository tests must pass after media repository/download datasource wiring changes.
- The relevant `:core:datastore` media filename tests must stay green after filename sanitization changes in the file datasource path.

## P2 Module Split And Gradle Conventions

Status: complete for the current core data-layer split.

- Current included modules are `:app`, `:feature`, `:core:data`, `:core:network`, `:core:database`, `:core:datastore`, `:core:model`, `:core:session`, and `:core:ui`.
- `:core:session` owns the canonical `SessionRepository` and `SessionCredentials` contract under `com.kzzz3.argus.lens.session`; Android-backed `LocalSessionStore` persistence lives in `:core:datastore`.
- `:core:network` owns Retrofit/OkHttp/SSE/API DTOs and backend URL BuildConfig; `:core:database` owns Room/KSP/schema export; `:core:data` owns repository orchestration; `:core:datastore` owns DataStore/Keystore/cache/file persistence.
- Future modules such as `:feature:auth`, `:feature:inbox`, `:feature:chat`, and `:feature:wallet` remain target modules, not current Gradle includes.
- Build convention plugins are deferred to a dedicated slice; repository resolution remains centralized in `settings.gradle.kts` and versions remain in `gradle/libs.versions.toml`.

Verification gate:

- `:app:testDebugUnitTest --tests "com.kzzz3.argus.lens.app.ReleaseAndModuleBoundaryTest"` must pass after module-readiness, Gradle-convention, or module-include changes.
- `:core:session:compileDebugKotlin` and the relevant `:core:datastore` session tests must pass after session contract or local session persistence changes.

## P2 Hilt Dependency Injection

Status: complete for the current module baseline.

- `ArgusLensApplication` is annotated with `@HiltAndroidApp` and supplies `HiltWorkerFactory` for WorkManager.
- `MainActivity` uses `@AndroidEntryPoint` and `ArgusLensAppViewModel` uses `@HiltViewModel`.
- `ArgusLensAppModule` wires repositories, use cases, schedulers, managers, and app dependencies from the singleton component.

Remaining refinement: feature-specific ViewModels can be introduced when feature modules need independent lifecycle/state ownership.

## P2 Room Schema And Migration Strategy

Status: complete for the v1 persistence baseline.

- `ArgusLensDatabase` uses `exportSchema = true` and `CURRENT_VERSION = 1`.
- `ArgusLensMigrations` is the central migration registry and tracks the database version.
- `ArgusLensDatabaseMigrationTest` verifies the exported current schema and current v1 tables.
- Historical migration tests become required before increasing `CURRENT_VERSION` beyond 1.

Verification gate:

- `:core:database:testDebugUnitTest` and the relevant database migration/schema tests must pass after Room entity, schema, or migration changes.

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

Status: complete for the current physical core-module split.

- The Android project is split into `:app`, `:feature`, `:core:data`, `:core:network`, `:core:database`, `:core:datastore`, `:core:model`, `:core:session`, and `:core:ui`.
- `:core:model` and `:core:ui` preserve their Kotlin package names; `:core:session` owns session contracts; `:core:data`, `:core:network`, `:core:database`, and `:core:datastore` use explicit `com.kzzz3.argus.lens.core.*` packages.
- `:app` depends on lower modules; `:feature` depends on feature-facing core APIs; `:core:data` depends on network/database/datastore/model/session; leaf core modules do not depend on app or feature.
- Further feature granularity such as `:feature:chat` should be driven by future ownership pressure, not premature splitting.
- The durable extraction roadmap for `:core:*` and `:feature:*` modules lives in `docs/android-architecture-target.md`.

## P3 Event Model

Status: complete for the current reducer/effect architecture.

- One-off commands remain sealed feature `*Effect` models; feature handlers own feature-specific dispatch/request policy, while app route adapters only adapt app-owned navigation where needed.
- Source-level regression tests ensure effect files stay sealed instead of becoming ad hoc strings or nullable status blobs.
- Screen-scoped user-visible messages stay feature-owned in immutable UI state, using `UiStatusMessage` when a shared message primitive is useful and existing `statusMessage` / `isStatusError` pairs while transitional screens migrate.
- User-visible durable state remains in feature state objects; one-off effects remain explicit reducer outputs.
- Root snackbar rendering remains deferred until multiple independent app-wide producers need one visual queue; do not add `SnackbarHostState`, `SharedFlow`, `Channel`, or `Toast` as default event/message infrastructure before that threshold is met.

Future refinement: introduce root snackbar rendering only after the producing features, lifecycle ownership, sign-out clearing behavior, and duplicate-delivery policy are explicit and covered by tests.

## P3 Process-Death Restoration

Status: complete for the safe selected-chat compatibility slice.

- `ArgusLensAppViewModel` owns process-death restore keys through `SavedStateHandle`, but stores only a token-free `AppRestorableEntryContext`: account id, stable `AppRoute` route string, and selected conversation id.
- The current route is not blindly replayed after process recreation. Initial authenticated startup still enters the main shell safely, then `RestoreAppSessionUseCase` loads conversation threads before restoring `AppRoute.Chat`.
- Restored `Chat` is allowed only when the initial session is authenticated, an access credential is present in the existing credential boundary, the saved account matches the current account, the saved route string is `Chat`, and the selected conversation id exists in the loaded thread snapshot.
- Missing credentials, stale conversations, route mismatch, or account mismatch clear the restorable entry context and fall back to the existing Inbox/Auth behavior.
- This slice deliberately defers broad route restoration, typed route args, Kotlin serialization route migration, and durable cross-launch “last opened conversation” preferences.

Verification gate:

- `:app:testDebugUnitTest --tests "com.kzzz3.argus.lens.app.RestoreAppSessionUseCaseTest"` must pass after hydration or selected-entry restore policy changes.
- `:app:testDebugUnitTest --tests "com.kzzz3.argus.lens.app.ArgusLensAppViewModelTest"` must pass after app state / host boundary wiring changes.
- `:app:testDebugUnitTest --tests "com.kzzz3.argus.lens.app.SessionBoundaryTest"` must pass after saved-state, Parcelable, or saveable session boundary changes.

## P4 Test Structure

Status: complete for the modernization regression baseline.

- P0 token/session boundaries are guarded by `SessionBoundaryTest`.
- P0/P1 route and Navigation Compose policy are guarded by `AppRouteNavigatorTest` and app compilation; P4 navigation mapping tests now directly cover `TopLevelDestination.fromRoute` and `fromShellDestination` for main shell tabs versus secondary/non-top-level routes.
- P4 app callback adapters are guarded by `AppRouteActionBindingsTest`, which executes auth entry callbacks, shell/wallet routing, inbox open/sync callbacks, realtime connection callbacks, and sign-out session-boundary delegation with handwritten fakes.
- P1 ViewModel and lifecycle ownership is guarded by `ArgusLensAppViewModelTest`; P4 direct app ViewModel tests now instantiate `ArgusLensAppViewModel` with handwritten app dependency fakes to verify `SavedStateHandle` restore keys, UI-state synchronization, realtime bookkeeping, and feature-state holder boundaries, not only source shape.
- P3 safe selected-chat process-death restoration is guarded by `RestoreAppSessionUseCaseTest`, `ArgusLensAppViewModelTest`, and `SessionBoundaryTest`.
- P2 Room migration/schema work is guarded by `ArgusLensDatabaseMigrationTest`.
- P2 WorkManager behavior is guarded by `BackgroundSyncWorkTest`, `BackgroundSyncTaskTest`, and `BackgroundSyncWorkerTest`.
- P3 release/module/event boundaries are guarded by `ReleaseAndModuleBoundaryTest` and `EventModelBoundaryTest`.
- P2 role taxonomy and naming boundaries are guarded by `RoleNamingBoundaryTest`.
- Media contract, download filename, and repository/data-source delegation boundaries are guarded by `MediaApiModelsTest`, `MediaFileNameTest`, and `RemoteMediaRepositoryTest`.
- The instrumentation baseline now uses `ArgusLensLaunchInstrumentedTest` instead of Android template tests.
- `ModernizationCoverageTest` guards the presence of these regression gates and this progress document.

Final verification gate for this modernization pass:

- `:app:testDebugUnitTest`
- `:core:data:testDebugUnitTest`
- `:core:network:testDebugUnitTest`
- `:core:database:testDebugUnitTest`
- `:core:datastore:testDebugUnitTest`
- `testDebugUnitTest`
- `lint`
- `assembleDebug`
- `:app:assembleDebugAndroidTest`
