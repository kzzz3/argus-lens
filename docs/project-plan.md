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

## 12. Planning and Progress Source of Truth

This document is the long-form Lens product and architecture blueprint. Active checklist state, immediate tasks, verification gates, and the current risk register live in the repository-level `PLAN.md`.

When architecture changes, update this blueprint. When implementation status changes, update `PLAN.md` and only mirror durable architectural conclusions here.
