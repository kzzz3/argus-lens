# ARGUS LENS KNOWLEDGE BASE

## OVERVIEW
`argus-lens` is the Android host application. It owns Android runtime behavior, UI, device permissions/capture, local persistence, sync UX, and user-visible messaging/payment flows.

## STRUCTURE
```text
argus-lens/
├── README.md
├── PLAN.md
├── AGENTS.md
├── settings.gradle.kts
├── build.gradle.kts
├── app/
│   ├── build.gradle.kts
│   └── src/
├── data/
│   ├── build.gradle.kts
│   └── src/
├── feature/
│   ├── build.gradle.kts
│   └── src/
├── model/
│   ├── build.gradle.kts
│   └── src/
├── ui/
│   ├── build.gradle.kts
│   └── src/
├── gradle/
└── docs/
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Project/module wiring | `settings.gradle.kts`, `build.gradle.kts` | Android modules: `:app`, `:feature`, `:data`, `:model`, `:ui` |
| App assembly / entry build config | `app/build.gradle.kts` | App shell, Hilt, release shrink config |
| Data/network/persistence build config | `data/build.gradle.kts` | Room/KSP, Retrofit/OkHttp, DataStore, backend BuildConfig values |
| Launcher/entry | `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/kzzz3/argus/lens/MainActivity.kt` | Manifest + activity startup |
| App shell code | `app/src/main/java/com/kzzz3/argus/lens/app/` | Navigation host, dependency assembly, Hilt wiring |
| Feature code | `feature/src/main/java/com/kzzz3/argus/lens/feature/` | Auth, inbox, contacts, wallet, call, realtime UI/business flows |
| Data code | `data/src/main/java/com/kzzz3/argus/lens/data/` | Repositories, Room, network clients, session persistence |
| Shared models | `model/src/main/java/com/kzzz3/argus/lens/model/` | Parcelable/domain models shared across data/feature/app |
| Shared UI | `ui/src/main/java/com/kzzz3/argus/lens/ui/` | Theme, shell widgets, status UI primitives |
| Local unit tests | `*/src/test/java/` | JVM tests live with their owning module |
| Instrumentation/UI tests | `app/src/androidTest/java/` | AndroidX / Compose test path |
| Onboarding and active plan | `README.md`, `PLAN.md` | Setup, navigation, current checklist, verification gates |
| Project intent | `docs/project-plan.md`, `docs/wallet-payment-flow.md` | Product and UX boundaries |

## COMMANDS
```bash
./gradlew testDebugUnitTest
./gradlew lint
./gradlew assembleDebug
# Device/emulator only, when UI/runtime behavior needs it:
./gradlew connectedAndroidTest
```

## CONVENTIONS
- Keep the project Android Studio-centered. Project layout should remain compatible with standard Android Studio workflows.
- Use the Gradle wrapper, not arbitrary local Gradle installations.
- Run Gradle tasks serially in this workspace; avoid parallel Gradle invocations because they can contend for the daemon/cache and hang.
- Repository resolution is centralized in `settings.gradle.kts` with `FAIL_ON_PROJECT_REPOS`; do not add ad hoc per-module repositories.
- `gradle/libs.versions.toml` is the version source of truth.
- `gradle.properties` explicitly sets `kotlin.code.style=official`.
- `data/build.gradle.kts` encodes debug/release backend URL behavior and `AUTH_MODE` / `CONVERSATION_MODE` BuildConfig boundaries.
- `app` may depend on all lower modules; `feature` may depend on `data`, `model`, and `ui`; `data` may depend on `model`; `ui` and `model` should not depend on product modules.
- Room is wired through KSP; preserve that path instead of casually switching annotation processing style.

## DOCUMENTATION WORKFLOW
- `README.md` is the Lens onboarding guide for Android setup, module navigation, and routine commands.
- `PLAN.md` is the active Lens task plan, risk register, and verification ledger.
- `docs/project-plan.md` remains the long-form product/architecture blueprint; do not duplicate active checklist state there unless the blueprint itself changes.
- `docs/android-modernization-progress.md` remains the detailed modernization evidence ledger.
- For non-trivial work, update `PLAN.md` before editing and complete Modify -> Review/Evaluate -> Document -> Cleanup before marking a task done.
- Documentation changes should accompany code changes whenever UI behavior, runtime contracts, Gradle commands, or verification gates change.

## ANTI-PATTERNS
- Do not put cloud routing, settlement/orchestration, or final authorization logic here.
- Do not call JNI directly from Compose UI; keep a repository/service boundary between UI and native code.
- Do not implement heavy media filtering or secret-bearing cryptographic logic directly in Kotlin/Compose/ViewModels.
- Do not edit `local.properties`; it is explicitly marked as disposable/generated local config.
- Do not treat `.gradle/`, `.idea/`, `.kotlin/`, or build outputs as source areas.

## TESTING
- Use each module's `src/test/java` for local JVM tests and `app/src/androidTest/java` for device/emulator tests.
- For routine verification, prefer `testDebugUnitTest`, `lint`, and `assembleDebug`; reserve `connectedAndroidTest` for emulator/device-specific coverage.
- Compose UI testing dependencies are present; use instrumentation tests when the behavior is UI/runtime-specific.
- Current tests often use handwritten fake repositories/services instead of a visible mocking framework.

## NOTES
- `docs/project-plan.md` says Lens owns Android permissions/lifecycle, CameraX integration, AudioRecord orchestration, Room queue behavior, WorkManager resync behavior, and user-visible status.
- `docs/project-plan.md` also says Lens does not own cloud session routing, heavy media filtering/signing internals, payment settlement logic, or model orchestration/final action authorization.
- The biggest code concentration currently sits in `feature/`, `data/`, and app wiring under `app/`; keep those boundaries sharp.
