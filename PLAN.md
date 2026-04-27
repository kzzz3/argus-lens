# Argus Lens Active Plan

## Project Goal

Argus Lens provides the Android runtime for Argus by delivering a reliable local-first messaging/payment client that can evolve into an AI-glasses HUD simulator.

## Core Feature Breakdown

- [x] Compose host app baseline and Android Studio-compatible module layout.
- [x] Local-first auth/session shell with DataStore-backed credentials.
- [x] Inbox, contacts, chat, call, wallet, and media composer shells.
- [x] Room-backed conversation/message/draft persistence.
- [x] Remote auth, session restore, conversation/message sync, text send, recall, read receipts, SSE resume, and generic file upload/download.
- [x] Instrumentation smoke coverage and modernization regression gates.
- [ ] Process-death restoration decision for selected conversation/session entry context.
- [ ] Richer first-class remote media send paths beyond the generic-file baseline.
- [ ] Real RTC signaling integration and call lifecycle events.
- [ ] WorkManager-backed reconciliation decision for conversation/message sync.
- [ ] Stage 2 Retina JNI service boundary for local native media/security primitives.

## Technology Stack and Architecture Decisions

- **Kotlin + Jetpack Compose**: concise, state-driven Android UI with a path toward HUD-style surfaces.
- **Room + DataStore**: local-first durable state and credential/session persistence.
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
- [ ] RTC signaling integration.
- [ ] Stronger process-death restoration rules.
- [ ] Background reconciliation strategy for sync.

### Phase 4 — Stage 2 AI-Glasses Runtime
- [ ] Camera/microphone/sensor capture orchestration.
- [ ] HUD-style confirmation surfaces.
- [ ] Retina JNI service boundary and capability probing.

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
- Contract drift with Cortex media/message DTOs.
- Background sync becoming implicit foreground refresh instead of explicit durable reconciliation.
- Premature JNI coupling before Retina contracts stabilize.

## Documentation Rules

- `README.md` explains how to onboard and run Lens.
- `PLAN.md` tracks active Lens work and verification gates.
- `docs/project-plan.md` remains the long-form product/architecture blueprint.
- `docs/android-modernization-progress.md` remains the detailed modernization evidence ledger.

## Verification Log

| Date | Scope | Command / method | Result |
|---|---|---|---|
| 2026-04-27 | Documentation workflow refresh | Read `AGENTS.md`, `docs/project-plan.md`, `docs/android-modernization-progress.md`, and verified referenced paths exist | PASS for this documentation pass |
