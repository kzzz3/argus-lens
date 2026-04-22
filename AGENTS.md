# ARGUS LENS KNOWLEDGE BASE

## OVERVIEW
`argus-lens` is the Android host application. It owns Android runtime behavior, UI, device permissions/capture, local persistence, sync UX, and user-visible messaging/payment flows.

## STRUCTURE
```text
argus-lens/
├── settings.gradle.kts
├── build.gradle.kts
├── app/
│   ├── build.gradle.kts
│   └── src/
├── gradle/
└── docs/
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Project/module wiring | `settings.gradle.kts`, `build.gradle.kts` | Single app module: `:app` |
| Actual Android build config | `app/build.gradle.kts` | Build types, BuildConfig values, test deps |
| Launcher/entry | `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/kzzz3/argus/lens/MainActivity.kt` | Manifest + activity startup |
| App code | `app/src/main/java/com/kzzz3/argus/lens/` | Main subtrees: `app`, `data`, `feature`, `ui` |
| Local unit tests | `app/src/test/java/` | JVM tests |
| Instrumentation/UI tests | `app/src/androidTest/java/` | AndroidX / Compose test path |
| Project intent | `docs/project-plan.md`, `docs/wallet-payment-flow.md` | Product and UX boundaries |

## COMMANDS
```bash
./gradlew test
./gradlew connectedAndroidTest
./gradlew assembleDebug
```

## CONVENTIONS
- Keep the project Android Studio-centered. Project layout should remain compatible with standard Android Studio workflows.
- Use the Gradle wrapper, not arbitrary local Gradle installations.
- Repository resolution is centralized in `settings.gradle.kts` with `FAIL_ON_PROJECT_REPOS`; do not add ad hoc per-module repositories.
- `gradle/libs.versions.toml` is the version source of truth.
- `gradle.properties` explicitly sets `kotlin.code.style=official`.
- `app/build.gradle.kts` encodes debug/release backend URL behavior and `AUTH_MODE` / `CONVERSATION_MODE` BuildConfig boundaries.
- Room is wired through KSP; preserve that path instead of casually switching annotation processing style.

## ANTI-PATTERNS
- Do not put cloud routing, settlement/orchestration, or final authorization logic here.
- Do not call JNI directly from Compose UI; keep a repository/service boundary between UI and native code.
- Do not implement heavy media filtering or secret-bearing cryptographic logic directly in Kotlin/Compose/ViewModels.
- Do not edit `local.properties`; it is explicitly marked as disposable/generated local config.
- Do not treat `.gradle/`, `.idea/`, `.kotlin/`, or build outputs as source areas.

## TESTING
- Use `app/src/test/java` for local JVM tests and `app/src/androidTest/java` for device/emulator tests.
- Compose UI testing dependencies are present; use instrumentation tests when the behavior is UI/runtime-specific.
- Current tests often use handwritten fake repositories/services instead of a visible mocking framework.

## NOTES
- `docs/project-plan.md` says Lens owns Android permissions/lifecycle, CameraX integration, AudioRecord orchestration, Room queue behavior, WorkManager resync behavior, and user-visible status.
- `docs/project-plan.md` also says Lens does not own cloud session routing, heavy media filtering/signing internals, payment settlement logic, or model orchestration/final action authorization.
- The biggest code concentration currently sits in `feature/`, `data/`, and app wiring under `app/`; keep those boundaries sharp.
