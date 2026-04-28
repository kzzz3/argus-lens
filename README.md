# Argus Lens

![Android](https://img.shields.io/badge/android-Jetpack%20Compose-3ddc84)
![Kotlin](https://img.shields.io/badge/kotlin-official%20style-7f52ff)
![Gradle](https://img.shields.io/badge/build-Gradle-02303a)

Argus Lens is the Android host app for Argus. It owns user-visible runtime behavior: Compose UI, Android permissions, capture entry points, local persistence, sync UX, messaging, wallet screens, and wearable/HUD evolution.

## Architecture at a Glance

Lens is local-first and Android Studio-centered. UI state flows through Compose screens and feature-owned state holders/controllers, persisted state lives behind repository boundaries, and cloud behavior enters through Cortex-facing network clients. Shared infrastructure is split across focused `:core:*` modules while `:app` remains the composition shell.

```text
argus-lens/
├── app/          app shell, navigation host, Hilt wiring, instrumentation tests
├── core/
│   ├── data/    repository contracts, implementations, and data-layer factories
│   ├── network/ Retrofit/OkHttp/SSE clients, API services, DTOs, backend URL config
│   ├── database/ Room database, DAO, entities, migrations, schemas
│   ├── datastore/ DataStore/Keystore/session/local cache/file persistence
│   ├── model/   shared Parcelable/domain models
│   ├── session/ session repository and credential contract
│   └── ui/      theme and shared UI primitives
├── feature/      auth, inbox, chat, contacts, wallet, call, realtime feature flows
└── docs/         product plans, modernization progress, payment flow notes
```

## Getting Started

### Requirements

- Android Studio with a recent Android SDK.
- JDK compatible with the Android Gradle toolchain used by the wrapper.
- Use the checked-in Gradle wrapper; do not rely on arbitrary local Gradle installations.

### Build and Test

Run Gradle tasks serially in this workspace.

```bash
./gradlew testDebugUnitTest
./gradlew lint
./gradlew assembleDebug
```

Device/emulator-only verification:

```bash
./gradlew connectedAndroidTest
```

Instrumentation APK build:

```bash
./gradlew :app:assembleDebugAndroidTest
```

## Project Navigation

| Need | File / directory |
|---|---|
| Active work plan | `PLAN.md` |
| Local agent rules | `AGENTS.md` |
| Product/architecture plan | `docs/project-plan.md` |
| Modernization gates | `docs/android-modernization-progress.md` |
| Long-term Android architecture target | `docs/android-architecture-target.md` |
| Wallet/payment UX | `docs/wallet-payment-flow.md` |
| Entry activity | `app/src/main/java/com/kzzz3/argus/lens/MainActivity.kt` |
| App composition root | `app/src/main/java/com/kzzz3/argus/lens/app/` |
| Root navigation | `app/src/main/java/com/kzzz3/argus/lens/navigation/` |
| Repository/data layer | `core/data/src/main/java/com/kzzz3/argus/lens/core/data/` |
| Network clients and DTOs | `core/network/src/main/java/com/kzzz3/argus/lens/core/network/` |
| Room database | `core/database/src/main/java/com/kzzz3/argus/lens/core/database/` |
| Session/cache/file persistence | `core/datastore/src/main/java/com/kzzz3/argus/lens/core/datastore/` |
| Shared models | `core/model/src/main/java/com/kzzz3/argus/lens/model/` |
| Session contract | `core/session/src/main/java/com/kzzz3/argus/lens/session/` |
| Shared UI | `core/ui/src/main/java/com/kzzz3/argus/lens/ui/` |

## Technology Stack

- **Jetpack Compose** for state-driven UI and HUD evolution.
- **Room** for local conversation/message/draft persistence.
- **DataStore** for session persistence.
- **WorkManager** for constrained background sync/retry work.
- **Retrofit/OkHttp** for Cortex HTTP/SSE integration.
- **Hilt** for Android dependency injection.
- **KSP** for Room code generation.

These choices keep the app compatible with Android Studio and modern Android architecture recommendations.

## Contribution Rules

- Read `PLAN.md` before multi-step work and update it when scope or verification changes.
- Keep cloud orchestration, payment settlement, and policy logic out of Lens.
- Keep JNI/native calls behind repository/service boundaries; Compose UI must not call native primitives directly.
- Update docs in the same change when commands, architecture, contracts, or user-visible behavior change.
- Never commit `.gradle/`, `.idea/`, `.kotlin/`, `build/`, `local.properties`, or generated APK/output files.
