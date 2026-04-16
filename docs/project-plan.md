# Argus Lens Technical Plan

## 1. Repository Positioning

Argus Lens is the Android host app. In product terms it is the simulated AI glasses device. In engineering terms it is the only repository that should own Android runtime behavior, foreground UX, permissions, offline queueing, camera/audio/sensor capture, user-visible messaging flows, and HUD-style interaction.

This repository must stay centered on the **Android Studio** workflow. All run, debug, layout inspection, packaging, and future NDK integration should remain compatible with the project shape Android Studio expects.

## 2. Product Strategy Split

The app evolves in two clear stages:

### Stage 1 — WeChat-like Baseline
Lens behaves like a standard mobile IM and payment client:

- login and conversation entry
- contact list, New Friends entry, and direct-chat timeline
- text, image, voice, and video messages
- 1v1 audio/video call entry
- wallet pay/collect flows and peer transfer initiation
- offline recovery and message resync

### Stage 2 — AI Glasses Enhancement
Lens becomes the Android simulator of smart glasses:

- back camera becomes first-person visual input
- microphone becomes always-available audio trigger input
- sensors become head gesture intent signals
- Compose UI degrades into a lightweight monocular HUD
- multimodal segments are pre-filtered locally and streamed to Cortex

## 3. User Experience Responsibilities

### 3.1 Stage 1 baseline UX
- chat list, direct-chat session page, and media composer
- press-to-talk or hold-to-record voice interaction
- image/video picking and capture entry
- call launch and in-call controls
- contacts page with New Friends request handling, wallet page, QR pay scanner, collect QR, and transfer confirmation flows

### 3.2 Stage 2 wearable UX
- translucent HUD instead of full-screen phone-first UI
- minimal overlays: contact target, confirmation chip, payment status, VAD wave, recording state
- glanceable action confirmation via tap or head gesture fallback
- low-distraction recovery prompts when cloud intent confidence is low

## 4. Stage-Oriented App Architecture

## 4.1 Stage 1 Android Baseline Architecture

### Functional modules
- authentication and device enrollment
- contacts, friend requests, and direct conversations
- text/media composer
- voice capture and send
- RTC session entry and signaling client
- wallet home, pay scanner, collect QR, transfer history, and receipt screens
- local persistence and sync recovery

### Technical backbone
- **UI**: Jetpack Compose, state-driven screens, MVI-style state reducers
- **Networking**: HTTP/WebSocket/TCP client abstractions toward Cortex
- **Persistence**: Room for chats, cursors, drafts, send queue, and transaction history cache
- **Background work**: WorkManager for reconnect sync, retry upload, and deferred reconciliation
- **Media entry**: CameraX and Android media stack for capture / scan / attachment selection

### Stage 1 data flow
1. user acts through standard app screens
2. outbound events enter a local queue first
3. local state is persisted in Room
4. socket and background workers coordinate sync with Cortex
5. media assets upload separately from control messages
6. server acks reconcile local message and payment state

## 4.2 Stage 2 AI Glasses Enhancement Architecture

### Wearable simulation rules
- **rear camera** simulates the user’s eyes
- **microphone** simulates ears + voice input channel
- **SensorManager** simulates head gestures such as nod and shake
- **Compose HUD** simulates a monocular overlay rather than a phone dashboard

### Added local capabilities
- FPV camera stream sampling and scene tagging triggers
- always-available VAD-triggered audio segmentation
- gesture interpretation pipeline for confirm / reject flows
- local pre-check and throttling before cloud upload
- HUD state machine for low-friction confirmation

## 5. Key Boundaries

### 5.1 Lens owns
- Android permissions and lifecycle
- CameraX integration and preview/capture state
- AudioRecord orchestration and app-facing voice UX
- Room queue and WorkManager resync behavior
- wallet QR scanning / collect UI and transfer confirmation
- sensor capture and local gesture interpretation
- user-visible status for model or payment operations

### 5.2 Lens does not own
- long-lived cloud session routing rules
- heavy media filtering or secure signing internals that should be hidden inside Retina
- payment transaction settlement logic
- model orchestration and final action authorization

## 6. Lens Interfaces

### 6.1 Lens -> Cortex
- auth and device registration
- contact and conversation sync
- message send, ack, recall, read-state sync
- upload session creation and media metadata submission
- WebRTC signaling exchange
- wallet summary loading, QR transfer session creation, and receipt/history sync
- multimodal intent upload in Stage 2

### 6.2 Lens -> Retina
- local media preprocessing and VAD/image-quality filtering
- secure QR parse / transaction payload handoff
- local capability probing and version checks
- JNI access to native realtime and security primitives

## 7. Two-Stage Feature Map

## 7.1 Stage 1 — WeChat Baseline Features

### IM baseline
- direct-chat timeline
- local draft and resend queue
- message status: sending / sent / delivered / failed / recalled
- unread cursor alignment with Sync-Key or timeline diff

### Media baseline
- voice note recording and send
- image send and receive
- short video send and receive
- 1v1 audio/video call entry and session screens

### Payment baseline
- wallet page with live wallet summary and balance
- Pay action opens QR scan for peer-to-peer wallet transfer
- Collect action shows the user's own QR code with optional amount / note request context
- transfer confirmation, result receipt, history list, and receipt detail views

## 7.2 Stage 2 — AI Glasses Features

### FPV capture and AI assist
- back-camera driven first-person capture
- ambient audio trigger detection and voice capture slicing
- upload only valuable segments selected by local filtering
- cloud-returned intent previews for send / call / pay actions

### Wearable interaction
- nod to confirm, shake to reject, tap as fallback
- HUD overlays for target person, message preview, amount, and confidence cues
- first-person stream source switching during calls
- remote annotation overlays received from peers in FPV sessions

## 8. Global Phase Mapping From Lens Perspective

### Pair A — Phase 1 / Phase 2
- **Phase 1 primary**: login, chat shell, local queue, text IM, reconnect sync UX
- **Phase 2 primary**: voice capture, image send, media upload queue, weak-network handling

### Pair B — Phase 3 / Phase 4
- **Phase 3 primary**: baseline 1v1 audio/video call UX and signaling client
- **Phase 4 primary**: rear-camera FPV source switching and HUD overlay for AR-like annotation

### Pair C — Phase 5 / Phase 6
- **Phase 5 support/primary**: capture raw media and sensor input, hand off filtering to Retina, manage upload gating UX
- **Phase 6 support/primary**: upload multimodal slices, display intent suggestions, collect confirm/reject interactions

### Pair D — Phase 7 / Phase 8
- **Phase 7 primary**: standard wallet pay / collect / transfer UI
- **Phase 8 primary**: gaze-like QR capture, head-gesture confirmation, Retina-backed secure signing handoff, success/failure HUD feedback

## 9. Technical Tracks

### UI/HUD track
- Compose navigation and baseline screens in Stage 1
- transition to HUD-first composables in Stage 2

### Local data track
- Room schema for conversations, messages, cursors, uploads, and payment drafts
- WorkManager-driven sync and retry workers

### Media track
- CameraX capture and QR scanning entry
- AudioRecord orchestration and codec handoff path
- RTC local media source management

### Sensor and intent track
- SensorManager-based nod/shake detection
- local debounce and threshold tuning
- deterministic event channel to UI and Cortex upload logic

### Native bridge track
- repository/service boundary around JNI
- no direct Compose-to-JNI calls
- stable capability probe before turning on Stage 2 features

## 10. Risks and Constraints

- Android lifecycle pressure around long-running capture or call sessions
- background restrictions and battery impact for reconnect and sync
- large media upload reliability under weak networks
- sensor false positives in gesture confirmation
- keeping Stage 2 HUD simple enough to remain usable

## 11. Non-Goals

- no cloud routing logic here
- no native cryptographic secret management in Kotlin layer
- no heavy media filtering logic implemented directly in Compose or ViewModels
- no backend transaction settlement logic here

## 12. Current Progress Checklist

### 12.1 Completed foundation and shell work
- [x] Android Studio-compatible Compose host app baseline is running
- [x] Local-first auth shell with explicit `AUTH_MODE` boundary (`LOCAL` / `REMOTE`)
- [x] Login and registration screens with reducer-driven validation and submission flow
- [x] Inbox shell, contacts shell, chat shell, and call shell are all in place
- [x] Single-chat shell and contact-driven direct-chat entry are in place
- [x] Chat composer supports text, image, video, and voice draft paths
- [x] Voice draft recording shell includes timer/cancel/finish behavior
- [x] Call shell includes local connecting/active/ended lifecycle and timer UX
- [x] Message state model includes sending / sent / delivered / failed / recalled
- [x] Failed messages can remain local and be retried instead of disappearing
- [x] Recall path exists in local UI and now has a remote path as well
- [x] Chat timeline auto-scrolls to the latest message

### 12.2 Completed local persistence work
- [x] Saveable state was migrated from handwritten `Saver` helpers to `@Parcelize`
- [x] Session persistence moved to DataStore
- [x] Local conversation persistence moved from saveable-only state to Room-backed storage
- [x] Room now has structured tables for conversations, messages, and draft attachments
- [x] Legacy snapshot storage is still bridged for migration compatibility
- [x] Conversation persistence is account-scoped, not global

### 12.3 Completed architecture cleanup
- [x] `SessionRepository` boundary exists
- [x] `ConversationRepository` boundary exists
- [x] `AppShellCoordinator` now owns hydration / signed-in / signed-out coordination logic
- [x] `ArgusLensApp.kt` is thinner than before and relies more on repository/coordinator boundaries
- [x] Conversation mode boundary exists (`LOCAL` / `REMOTE`)
- [x] app-level dependency creation now lives in a dedicated composition root instead of being hand-built inline inside the main app composable
- [x] shared remote network factories now own Retrofit/OkHttp creation instead of repeating setup per feature module
- [x] local empty-state bootstrap no longer seeds sample inbox threads; the current development baseline starts from real persisted or remotely synced state only

### 12.4 Completed real chain integration
- [x] Remote auth login/registration path is enabled
- [x] Remote session restore is enabled during app startup hydration
- [x] Offline startup keeps previously authenticated users inside the app when the network is unavailable
- [x] Remote conversation list sync is enabled
- [x] Remote message list sync is enabled when opening conversations
- [x] Remote text-message send path is enabled
- [x] Remote message recall path is enabled
- [x] Remote delivery receipt and read-state sync are enabled
- [x] Opening an active chat now clears local unread state and auto-applies visible read receipts during realtime refresh
- [x] Conversation summary hydration now preserves richer local thread history and drafts instead of flattening everything to preview-only state
- [x] Generic file-message upload/finalize/send path is enabled for image/video-as-file behavior
- [x] Attachment download/save-as path is enabled with in-chat success/failure feedback
- [x] SSE realtime conversation updates are enabled
- [x] Realtime client now keeps a visible connection status and auto-recovers with reconnect backoff
- [x] SSE resume now reuses Last-Event-ID so reconnects can replay missed conversation events
- [x] Message-level realtime updates now refresh inbox summaries instead of only the open chat state
- [x] Remote conversation/message fetch now uses recent-window semantics rather than pretending to be full-history sync
- [x] Cursor-backed reconnect sync now continues across multiple pages instead of stopping after a single recent-window pull
- [x] Contacts now prefer stable direct-conversation ids instead of depending on display-name/title matching
- [x] File messages now use structured attachment metadata as the latest contract baseline
- [x] local Room storage now treats the current schema as the single supported development baseline; legacy snapshot bridging and incremental migration steps were removed
- [x] local conversation startup no longer injects demo/sample timeline data into empty accounts

## 13. Next-Phase Checklist

### 13.1 Near-term Stage 1 priorities
- [x] add explicit sync cursor / next-window semantics instead of simple recent-window pulls
- [ ] persist and restore selected conversation/session-entry context across process death if we want chat re-entry to survive full app recreation
- [x] broaden receipt reconciliation beyond foreground chat so background/inbox refresh paths stay fully monotonic under delayed events
- [x] harden file-message metadata presentation and download management beyond the current basic save-as UX
- [ ] connect remote send for richer message types beyond the current text-first plus generic-file path

### 13.2 RTC and realtime follow-up
- [ ] replace local call shell with real signaling contract integration
- [ ] add call invite / accept / reject / reconnect server events
- [ ] separate RTC state handling from `ArgusLensApp.kt` into a stronger coordinator/state holder

### 13.3 Payment and scanning follow-up
- [x] add wallet page with pay / collect entry points
- [x] add QR transfer session persistence and confirmation flow

### 13.4 Best-practice cleanup still worth doing
- [ ] continue shrinking `ArgusLensApp.kt` by extracting remaining chat/call orchestration slices
- [ ] keep expanding repository/coordinator tests around hydration, offline fallback, receipt reconciliation, and sync transitions
- [ ] decide whether conversation/message sync should move to WorkManager-driven background reconciliation

## 14. Immediate Next Design Tasks

1. define sync cursor / recent-window evolution so remote history acts as a bounded sync source instead of a full-history truth source
2. extend delivery/read receipt reconciliation from the active-chat path into stronger background and restore-time convergence rules
3. refine generic file-message contracts so metadata, retention, and download UX remain predictable under retries
4. define WebRTC signaling schema and session lifecycle events
5. define the Stage 2 JNI-facing service boundary for Retina integration without regressing the Stage 1 local-first IM model
