# Argus Lens Active Plan

## Project Goal

Argus Lens provides the Android runtime for Argus by delivering a reliable local-first messaging/payment client that can evolve into an AI-glasses HUD simulator.

## Core Feature Breakdown

- [x] Compose host app baseline and Android Studio-compatible module layout.
- [x] Local-first auth/session shell with safe DataStore-backed session identity and encrypted token persistence behind the session store.
- [x] Inbox, contacts, chat, call, wallet, and media composer shells.
- [x] Room-backed conversation/message/draft persistence.
- [x] Remote auth, session restore, conversation/message sync, text send, recall, read receipts, SSE resume, and generic file upload/download.
- [x] Instrumentation smoke coverage and modernization regression gates.
- [x] P0 app shell state-boundary slice: introduce `AppRouteHostState` and `AppRouteHostCallbacks` to slim `AppRouteHost` / `ArgusLensApp` without changing navigation semantics.
- [x] P0 app shell action-binding slice: move route request/callback adapters into `AppRouteActionBindings` while preserving existing runtime and navigation behavior.
- [x] P0 app shell effect-boundary slice: move host `LaunchedEffect` / `DisposableEffect` orchestration into `AppRouteHostEffects` while preserving route/session/realtime behavior.
- [x] P0 wallet action-boundary slice: move wallet action reduction/effect dispatch into feature-owned `WalletActionHandler`.
- [x] P0 wallet request-boundary slice: move wallet async request freshness/invalidation into feature-owned `WalletRequestRunner` and request guard tests.
- [x] P0 wallet effect-boundary slice: move wallet effect dispatch/request launch rules into feature-owned `WalletEffectHandler` and remove the obsolete app wallet route runtime.
- [x] P0 wallet controller shell slice: introduce feature-owned `WalletFeatureController` so wallet action/effect composition is ready for ViewModel extraction while app keeps root state/session/navigation callbacks.
- [x] P0 wallet state-holder slice: move wallet screen state out of `ArgusLensAppUiState` into a ViewModel-owned feature `WalletStateHolder`, with app code retaining only route/session/navigation adaptation.
- [x] P0 auth state-holder slice: move login/register form state and reducer/submission orchestration out of `ArgusLensAppUiState` and the app-owned entry runtime into a ViewModel-owned feature `AuthStateHolder`, while app code retains route/session-success callbacks.
- [x] P0 inbox state-holder slice: move inbox UI-state derivation and route-agnostic inbox action dispatch into a ViewModel-owned feature `InboxStateHolder`, while app code retains conversation-open sequencing, route changes, sign-out, realtime, persistence, and chat ownership.
- [x] P0 chat state-holder slice: move selected-conversation chat state/UI derivation into a ViewModel-owned feature `ChatStateHolder`, while app code retains chat action effects, call routing, dispatch, recall/download, selected conversation, and shared thread ownership.
- [x] P1 typed-navigation compatibility slice: introduce an app-owned stable route contract around `AppRoute` so host navigation no longer depends on enum names, while deferring AndroidX `composable<T>` registration and route args until selected-conversation/process-death policy is explicit.
- [x] P1 app dependency effects-boundary slice: narrow `AppRouteHostEffects` to a four-field lifecycle/effects dependency object while leaving Hilt aggregate construction and feature ownership unchanged.
- [x] P2 LocalSessionStore delegation slice: keep `SessionRepository` behavior unchanged while splitting local session identity and credential persistence behind module-internal stores.
- [x] P2 role naming boundary slice: document and guard runtime-like role names before broad renames or ownership moves.
- [x] P2 focused UseCase slice: extract only the outgoing chat send/media-upload workflow into a feature-local `SendOutgoingChatMessageUseCase`, avoiding pass-through repository wrappers and new modules.
- [x] P2 module split readiness slice: document and guard future `:core:*` / `:feature:*` modules as planned targets, not current Gradle includes, while deferring `buildSrc` / `build-logic` to a dedicated convention-plugin slice.
- [x] P2 media datasource boundary slice: keep `MediaRepository` public while making `RemoteMediaRepository` internal and delegating Android download file persistence to a module-internal `MediaFileDataSource`.
- [x] P3 first core-module sync slice: move shared model/UI modules into `:core:model` and `:core:ui` while preserving package names and aggregate `:data` / `:feature` ownership.
- [x] P1 navigation graph split slice: normalize root navigation into `ArgusNavHost`, separate auth/main child graphs, and move feature leaf registrations into feature-owned navigation files while preserving `AppRoute` compatibility.
- [x] Long-term Android architecture target documented with `:app`, `:core:*`, `:feature:*`, typed navigation, session, data, test, and cleanup roadmaps.
- [ ] Process-death restoration decision for selected conversation/session entry context.
- [ ] Richer first-class remote media send paths beyond the generic-file baseline.
- [ ] Real RTC signaling integration and call lifecycle events.
- [ ] WorkManager-backed reconciliation decision for conversation/message sync.
- [ ] Stage 2 Retina JNI service boundary for local native media/security primitives.

## Technology Stack and Architecture Decisions

- **Kotlin + Jetpack Compose**: concise, state-driven Android UI with a path toward HUD-style surfaces.
- **Room + DataStore**: local-first durable app state and safe session identity persistence; token persistence stays behind the session boundary.
- **WorkManager**: constrained retry and background sync semantics.
- **Retrofit/OkHttp**: conventional Cortex HTTP/SSE integration.
- **Hilt**: dependency wiring through Android-supported lifecycle scopes.
- **Gradle version catalog + wrapper**: reproducible Android Studio-centered builds.

## Project Structure Plan

```text
argus-lens/
├── app/          app shell, navigation, Hilt, app-level tests
├── core/
│   ├── model/   shared domain/Parcelable models
│   └── ui/      reusable theme and UI primitives
├── data/         repositories, network clients, Room, DataStore
├── feature/      feature state/reducers/effects/screens
└── docs/         architecture background and modernization progress
```

Long-term target topology is documented in `docs/android-architecture-target.md`: `:app` remains the composition shell, shared infrastructure converges into `:core:*`, and independently owned flows converge into `:feature:*` modules after package-level boundaries are stable.

## Development Phases and Milestones

### Phase 1 — MVCV: Android Core
- [x] App launches through the Compose shell.
- [x] Auth and conversation shells compile and run.
- [x] Local state can persist enough session/conversation data to avoid demo-only behavior.

### Phase 2 — Stage 1 Messaging Baseline
- [x] Remote auth/session restore.
- [x] Remote conversation/message sync.
- [x] Remote text send, recall, receipts, SSE resume.
- [x] Generic media upload/download with sanitized filename handling.
- [ ] Rich media-specific send paths.

### Phase 3 — Stage 1 Runtime Maturity
- [x] Slim `AppRouteHost` by replacing scattered state/callback parameters with explicit host boundary objects.
- [x] Extract `AppRouteActionBindings` so `AppRouteHost` no longer declares route action/request/callback factories inline.
- [x] Extract `AppRouteHostEffects` so `AppRouteHost` no longer declares lifecycle effect blocks inline.
- [x] Split navigation into a normalized root host with auth and main child graphs while keeping login/register and main shell behavior stable.
- [x] Move auth/register form state and submit orchestration into a feature-owned state holder while keeping session application and route transitions app-owned.
- [x] Move inbox UI derivation and simple action dispatch into a feature-owned state holder while keeping conversation-open sequencing and shared thread mutation in app-owned runtime boundaries.
- [x] Move selected chat state/UI derivation into a feature-owned state holder while keeping chat action effects and shared thread mutation in app-owned runtime boundaries.
- [x] Replace app-side route enum-name navigation with a stable app-owned route contract helper while keeping feature leaf `composable("...")` registrations unchanged.
- [x] Narrow host lifecycle effects to `AppRouteHostEffectDependencies` so this effects layer no longer receives the full `AppDependencies` aggregate.
- [ ] RTC signaling integration.
- [ ] Stronger process-death restoration rules.
- [ ] Background reconciliation strategy for sync.

### Phase 4 — Stage 2 AI-Glasses Runtime
- [ ] Camera/microphone/sensor capture orchestration.
- [ ] HUD-style confirmation surfaces.
- [ ] Retina JNI service boundary and capability probing.

### Architecture Roadmap — Target Android Organization
- [x] Document the durable target architecture and P0-P4 migration roadmap in `docs/android-architecture-target.md`.
- [ ] P0: continue app-shell slimming and feature ViewModel extraction without changing route semantics; wallet, auth, inbox, and chat now have feature-owned state holders, host lifecycle effects receive a narrow dependency object, and future AndroidX/Hilt feature ViewModel wrappers can land once lifecycle/module dependencies are explicit.
- [x] P1: establish the first typed route compatibility contract around existing `AppRoute` leaf strings without route args or Kotlin serialization.
- [ ] P1: build on the verified auth/main nested graph baseline with AndroidX typed route registrations, route args, and process-death restoration rules one feature at a time.
- [x] P2: continue splitting session/token/crypto responsibilities and make repository/data-source/use-case boundaries explicit before Gradle extraction; `LocalSessionStore` now delegates identity and credentials to separate stores, `RemoteMediaRepository` delegates download file persistence to `MediaFileDataSource`, but crypto/key operations remain in the credential store implementation; first UseCase work is limited to real multi-repository outgoing chat/media orchestration.
- [x] P2: define the initial runtime/coordinator/handler/runner/store/state-holder/controller naming taxonomy and guard current transitional exceptions.
- [x] P2/P3: document module split readiness gates and keep future target modules out of `settings.gradle.kts` until ownership, tests, and dependency direction are proven.
- [ ] P3: continue `:core:*` and `:feature:*` extraction after the verified `:core:model` / `:core:ui` baseline, keeping ownership and package boundaries stable.
- [ ] P4: enforce dependency direction, expand test fixtures, and remove transitional shims after each migration slice.

## Mandatory M-R-E-A Cycle

1. **Modify**: make the smallest Android-scope change.
2. **Review/Evaluate**: check UI state, lifecycle, persistence, security, and module boundaries.
3. **Document**: update this plan plus README/docs when behavior or commands change.
4. **Cleanup**: remove stale tests/files/imports and run the relevant serial Gradle checks.

## Verification Gates

| Change type | Required verification |
|---|---|
| Routine app/data/feature code | `./gradlew testDebugUnitTest` |
| UI/resources/build config | `./gradlew lint`, `./gradlew assembleDebug` |
| Instrumentation/runtime smoke | `./gradlew :app:assembleDebugAndroidTest`; `./gradlew connectedAndroidTest` when emulator/device behavior is required |
| Session/token/security boundaries | Relevant `:app:testDebugUnitTest` or source-boundary tests listed in `docs/android-modernization-progress.md` |
| Docs-only | Read changed docs and verify commands against Gradle files |

Run Gradle tasks serially.

## Risk Register

- Reintroducing token material into saveable UI state.
- Growing `ArgusLensApp.kt` or route host orchestration beyond readable lifecycle boundaries.
- Moving route or session behavior while extracting shell boundary objects; this pass must preserve enum-backed navigation and existing `rememberUpdatedState` patterns.
- Contract drift with Cortex media/message DTOs.
- Background sync becoming implicit foreground refresh instead of explicit durable reconciliation.
- Premature JNI coupling before Retina contracts stabilize.

## Documentation Rules

- `README.md` explains how to onboard and run Lens.
- `PLAN.md` tracks active Lens work and verification gates.
- `docs/android-architecture-target.md` owns the long-term Android architecture target and migration roadmap.
- `docs/project-plan.md` remains the long-form product/architecture blueprint.
- `docs/android-modernization-progress.md` remains the detailed modernization evidence ledger.

## Verification Log

| Date | Scope | Command / method | Result |
|---|---|---|---|
| 2026-04-27 | Documentation workflow refresh | Read `AGENTS.md`, `docs/project-plan.md`, `docs/android-modernization-progress.md`, and verified referenced paths exist | PASS for this documentation pass |
| 2026-04-27 | P0 app shell state-boundary planning | Explored `AppRouteHost`, `ArgusLensAppViewModel`, session store, official Android guidance, and Oracle sequencing advice | Planned first slice: state/callback boundary only; typed navigation, feature module split, LocalSessionStore split, and UseCase layer deferred |
| 2026-04-27 | P0 app shell state-boundary TDD | Red/green source-boundary tests for `AppRouteHostState` / `AppRouteHostCallbacks` and dedicated `ArgusLensAppState.kt` | Targeted tests passed; broader verification follows |
| 2026-04-27 | Android target architecture documentation | Read current Lens README, PLAN, project-plan, modernization progress, code-structure audit, official Android architecture/modularization guidance, and Oracle review findings | Target roadmap documented; Oracle-required wording fixes applied; docs verification follows before commit |
| 2026-04-27 | P0 app shell action-binding TDD | Red `ArgusLensAppViewModelTest`; green `ArgusLensAppViewModelTest`; related route runtime tests; `:app:testDebugUnitTest`; `testDebugUnitTest`; `lint`; `assembleDebug`; Kotlin LSP unavailable because `kotlin-lsp` is not installed | PASS |
| 2026-04-27 | P0 wallet action-boundary TDD | Red `WalletActionHandlerTest`; green `WalletActionHandlerTest`; targeted app wallet boundary tests; `:feature:testDebugUnitTest`; `:app:testDebugUnitTest`; `testDebugUnitTest`; `lint`; `assembleDebug`; Kotlin LSP unavailable because `kotlin-lsp` is not installed | PASS |
| 2026-04-27 | P0 wallet request-boundary TDD | Red `WalletRequestRunnerTest`; green `WalletRequestRunnerTest` and `WalletRequestGuardTest`; targeted app wallet route/support tests; `:feature:testDebugUnitTest`; `:app:testDebugUnitTest`; `testDebugUnitTest`; `lint`; `assembleDebug`; Kotlin LSP unavailable because `kotlin-lsp` is not installed | PASS |
| 2026-04-27 | P0 wallet effect-boundary TDD | Red `WalletEffectHandlerTest`; green `WalletEffectHandlerTest`; targeted app wallet adapter tests; `:feature:testDebugUnitTest`; `:app:testDebugUnitTest`; `testDebugUnitTest`; `lint`; `assembleDebug`; Kotlin LSP unavailable because `kotlin-lsp` is not installed | PASS |
| 2026-04-27 | P0 wallet controller shell TDD | Red `WalletFeatureControllerTest`; green `WalletFeatureControllerTest`; obsolete app wallet route adapter deleted after controller wiring; `:feature:testDebugUnitTest`; `:app:testDebugUnitTest`; `testDebugUnitTest`; `lint`; `assembleDebug`; Kotlin LSP unavailable because `kotlin-lsp` is not installed | PASS |
| 2026-04-27 | P0 wallet state-holder TDD | Red `WalletStateHolderTest`; green `WalletStateHolderTest`; red/green `ArgusLensAppViewModelTest.appViewModelOwnsWalletStateHolderLifetime` for ViewModel-owned holder/request lifetime; app navigation runtime test updated for holder account-open callback; root `uiState.walletState` / `updateWalletState` stale-reference scan clean; `:feature:testDebugUnitTest`; `:app:testDebugUnitTest`; `testDebugUnitTest`; `lint`; `assembleDebug`; Kotlin LSP unavailable because `kotlin-lsp` is not installed | PASS |
| 2026-04-27 | P0 app host effect-boundary TDD | Red/green `ArgusLensAppViewModelTest.appRouteHost_delegatesLifecycleEffects`; moved host `LaunchedEffect` / `DisposableEffect` blocks into `AppRouteHostEffects`; `:app:testDebugUnitTest`; `testDebugUnitTest`; `lint`; `assembleDebug`; Kotlin LSP unavailable because `kotlin-lsp` is not installed | PASS |
| 2026-04-27 | P3 core model/ui module sync | Red/green `ReleaseAndModuleBoundaryTest` and `SessionBoundaryTest` for `:core:model` / `:core:ui`; moved shared source/config from `model/` and `ui/` into `core/model/` and `core/ui/` without package renames; `:app:testDebugUnitTest`; `testDebugUnitTest`; `lint`; `assembleDebug`; Kotlin LSP unavailable because `kotlin-lsp` is not installed; Markdown LSP unavailable | PASS |
| 2026-04-27 | P1 auth/main navigation split TDD | Red/green `NavigationGraphBoundaryTest`; updated `AppRouteNavigationRuntimeTest.argusNavHostGraphs_registerEveryDeclaredAppRoute`; introduced root `ArgusNavHost`, `AuthGraphRoute`, `MainGraphRoute`, `TopLevelDestination`, and feature-owned child navigation registrations; `:app:testDebugUnitTest`; `testDebugUnitTest`; `lint`; `assembleDebug`; Kotlin LSP unavailable because `kotlin-lsp` is not installed; Markdown LSP unavailable | PASS |
| 2026-04-28 | P0 auth state-holder TDD | Red/green `AuthStateHolderTest`; red/green `ArgusLensAppViewModelTest.appViewModelOwnsAuthStateHolderLifetime` and `appUiStateDoesNotOwnAuthOrRegisterFormState`; removed obsolete app `EntryRouteRuntime`; `:app:testDebugUnitTest`; `testDebugUnitTest`; `lint`; `assembleDebug`; Kotlin LSP unavailable because `kotlin-lsp` is not installed; Markdown LSP unavailable | PASS |
| 2026-04-28 | P0 inbox state-holder TDD | Red/green `InboxStateHolderTest`; red/green `ArgusLensAppViewModelTest.appViewModelOwnsInboxStateHolderLifetime`; removed obsolete app `InboxActionRouteRuntime` while keeping app `InboxRouteRuntime`; `:app:testDebugUnitTest`; `testDebugUnitTest`; `lint`; `assembleDebug`; Kotlin LSP unavailable because `kotlin-lsp` is not installed; Markdown LSP unavailable | PASS |
| 2026-04-28 | P0 chat state-holder TDD | Red/green `ChatStateHolderTest`; red/green `ArgusLensAppViewModelTest.appViewModelOwnsChatStateHolderLifetime`; moved selected-conversation chat state/UI derivation out of `AppRouteUiState` while keeping app `ChatRouteRuntime`; `:app:testDebugUnitTest`; `testDebugUnitTest`; `lint`; `assembleDebug`; Kotlin LSP unavailable because `kotlin-lsp` is not installed; Markdown LSP unavailable | PASS |
| 2026-04-28 | P1 typed-navigation compatibility TDD | Red/green `AppRouteNavigationRuntimeTest` route contract checks; red/green `NavigationGraphBoundaryTest` host/start-destination source checks; introduced stable app-owned `AppRouteDescriptor` / `routeString` contract and replaced app-side enum-name navigation; `:app:testDebugUnitTest`; `testDebugUnitTest`; `lint`; `assembleDebug`; Kotlin LSP unavailable because `kotlin-lsp` is not installed | PASS |
| 2026-04-28 | P1 app dependency effects-boundary TDD | Red/green `ArgusLensAppViewModelTest.appRouteHostEffectsUseNarrowDependencyBoundary`; introduced `AppRouteHostEffectDependencies` for initial session, credentials, credential store, and realtime client while leaving aggregate construction intact; `:app:testDebugUnitTest`; `testDebugUnitTest`; `lint`; `assembleDebug`; Kotlin LSP unavailable because `kotlin-lsp` is not installed | PASS |
| 2026-04-28 | P2 LocalSessionStore delegation TDD | Red/green `LocalSessionStoreDelegationTest.sessionRepositoryDelegatesIdentityAndCredentialsOperationsToSeparateStores`; split `LocalSessionStore` into a facade over `LocalSessionStateStore` and `LocalSessionCredentialsStore` while preserving storage files, keys, Keystore alias, and public `SessionRepository` behavior; `:data:testDebugUnitTest`; `:app:testDebugUnitTest --tests "com.kzzz3.argus.lens.app.SessionBoundaryTest"`; `:app:testDebugUnitTest`; `testDebugUnitTest`; `lint`; `assembleDebug`; Kotlin LSP unavailable because `kotlin-lsp` is not installed; Markdown LSP unavailable | PASS |
| 2026-04-28 | P2 role naming boundary TDD | Red/green `RoleNamingBoundaryTest.roleNamingTaxonomyDocumentsEveryRuntimeLikeRole`; documented the runtime/coordinator/handler/runner/store/state-holder/controller taxonomy and classified transitional names before broad renames; `:app:testDebugUnitTest`; `testDebugUnitTest`; `lint`; `assembleDebug`; Kotlin LSP unavailable because `kotlin-lsp` is not installed; Markdown LSP unavailable | PASS |
| 2026-04-28 | P2 focused UseCase TDD | Red `SendOutgoingChatMessageUseCaseTest.invoke_uploadsDraftAttachmentThenSendsFinalizedMessage` failed on missing `SendOutgoingChatMessageUseCase`; green `SendOutgoingChatMessageUseCaseTest`; extracted feature-local outgoing chat/media send orchestration into `SendOutgoingChatMessageUseCase` and wired `ChatCoordinator` through it without adding pass-through use cases or new modules; `:feature:testDebugUnitTest`; `testDebugUnitTest`; `lint`; `assembleDebug`; Kotlin LSP unavailable because `kotlin-lsp` is not installed; Markdown LSP unavailable | PASS |
| 2026-04-28 | P2 module split readiness TDD | Red/green `ReleaseAndModuleBoundaryTest.moduleSplitReadinessKeepsFutureModulesPlannedButNotIncluded`; documented future `:core:*` / `:feature:*` readiness gates while keeping `settings.gradle.kts`, `buildSrc`, and `build-logic` unchanged; `:app:testDebugUnitTest --tests "com.kzzz3.argus.lens.app.ReleaseAndModuleBoundaryTest"`; `testDebugUnitTest`; `lint`; `assembleDebug`; Kotlin LSP unavailable because `kotlin-lsp` is not installed; Markdown LSP unavailable | PASS |
| 2026-04-28 | P2 media datasource boundary TDD | Red `RemoteMediaRepositoryTest.downloadAttachment_delegatesDownloadedContentToFileDataSource` failed on missing `MediaFileDataSource` seam; green after introducing module-internal `MediaFileDataSource` / `AndroidMediaFileDataSource` and keeping `createMediaRepository` public wiring unchanged; review-discovered filename fallback hardening added red/green `MediaFileNameTest.sanitizeMediaFileName_sanitizesAttachmentFallback`; `:data:testDebugUnitTest --tests "com.kzzz3.argus.lens.data.media.MediaFileNameTest" --tests "com.kzzz3.argus.lens.data.media.RemoteMediaRepositoryTest"`; `:data:testDebugUnitTest`; `testDebugUnitTest`; `lint`; `assembleDebug`; Oracle/review-work pass after fallback fix; Kotlin LSP unavailable because `kotlin-lsp` is not installed; Markdown LSP unavailable | PASS |
| 2026-04-28 | P3 event/message boundary TDD | Red `EventModelBoundaryTest.eventMessageMechanismDocumentsStateEffectAndRootDisplayBoundaries` failed until docs explicitly defined screen-scoped feature-owned messages, sealed feature effects, and deferred root snackbar/global bus criteria; green after updating `docs/android-modernization-progress.md` and `docs/android-architecture-target.md`; tightened source guard against premature `SharedFlow`, `Channel`, `SnackbarHostState`, `SnackbarHost`, and `Toast` event primitives; `:app:testDebugUnitTest --tests "com.kzzz3.argus.lens.app.EventModelBoundaryTest"`; `testDebugUnitTest`; `lint`; `assembleDebug`; Oracle review PASS; Kotlin LSP unavailable because `kotlin-lsp` is not installed; Markdown LSP unavailable | PASS |
