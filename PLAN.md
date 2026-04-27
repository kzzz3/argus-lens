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
- [x] P0 wallet action-boundary slice: move wallet action reduction/effect dispatch into feature-owned `WalletActionHandler`.
- [x] P0 wallet request-boundary slice: move wallet async request freshness/invalidation into feature-owned `WalletRequestRunner` and request guard tests.
- [x] P0 wallet effect-boundary slice: move wallet effect dispatch/request launch rules into feature-owned `WalletEffectHandler` while keeping app navigation adaptation in `WalletRouteRuntime`.
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
├── app/      app shell, navigation, Hilt, app-level tests
├── feature/  feature state/reducers/effects/screens
├── data/     repositories, network clients, Room, DataStore
├── model/    shared domain/Parcelable models
├── ui/       reusable theme and UI primitives
└── docs/     architecture background and modernization progress
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
- [ ] RTC signaling integration.
- [ ] Stronger process-death restoration rules.
- [ ] Background reconciliation strategy for sync.

### Phase 4 — Stage 2 AI-Glasses Runtime
- [ ] Camera/microphone/sensor capture orchestration.
- [ ] HUD-style confirmation surfaces.
- [ ] Retina JNI service boundary and capability probing.

### Architecture Roadmap — Target Android Organization
- [x] Document the durable target architecture and P0-P4 migration roadmap in `docs/android-architecture-target.md`.
- [ ] P0: continue app-shell slimming and feature ViewModel extraction without changing route semantics; next candidate is wallet ViewModel shell preparation.
- [ ] P1: introduce typed route contracts and process-death restoration rules one feature at a time.
- [ ] P2: split session/token/crypto responsibilities and make repository/data-source/use-case boundaries explicit before Gradle extraction.
- [ ] P3: extract `:core:*` and `:feature:*` modules only after ownership and package boundaries are stable.
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
| 2026-04-27 | P0 wallet effect-boundary TDD | Red `WalletEffectHandlerTest`; green `WalletEffectHandlerTest`; targeted `WalletRouteRuntimeTest`; `:feature:testDebugUnitTest`; `:app:testDebugUnitTest`; `testDebugUnitTest`; `lint`; `assembleDebug`; Kotlin LSP unavailable because `kotlin-lsp` is not installed | PASS |
