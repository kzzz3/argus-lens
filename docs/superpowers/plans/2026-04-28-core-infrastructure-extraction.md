# Core Infrastructure Extraction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract session, network, and database infrastructure from the aggregate `:data` module into focused `:core:session`, `:core:network`, and `:core:database` modules without changing runtime behavior.

**Architecture:** Move pure infrastructure first and keep product repository orchestration in `:data`. `:core:session` owns session contracts, DataStore identity persistence, token storage, and token cipher boundaries. `:core:network` owns BuildConfig-free Retrofit/OkHttp/Gson primitives. `:core:database` owns Room database, entities, DAOs, migrations, and schema tests; repository/domain mapping remains in `:data`.

**Tech Stack:** Kotlin, Android Gradle Plugin, KSP, Room, DataStore Preferences, Retrofit, OkHttp, Gson, Android Keystore, JUnit.

---

## File Structure

### Create modules

- Create: `core/session/build.gradle.kts`
- Create: `core/network/build.gradle.kts`
- Create: `core/database/build.gradle.kts`
- Modify: `settings.gradle.kts`
- Modify: `data/build.gradle.kts`
- Modify: `app/build.gradle.kts` only if app imports session contracts directly after package migration.

### Session files

- Move/split from: `data/src/main/java/com/kzzz3/argus/lens/data/session/SessionRepository.kt`
- Move/split from: `data/src/main/java/com/kzzz3/argus/lens/data/session/LocalSessionStore.kt`
- Create: `core/session/src/main/java/com/kzzz3/argus/lens/session/SessionRepository.kt`
- Create: `core/session/src/main/java/com/kzzz3/argus/lens/session/SessionCredentials.kt`
- Create: `core/session/src/main/java/com/kzzz3/argus/lens/session/SessionStateStore.kt`
- Create: `core/session/src/main/java/com/kzzz3/argus/lens/session/SecureTokenStore.kt`
- Create: `core/session/src/main/java/com/kzzz3/argus/lens/session/TokenCipher.kt`
- Create: `core/session/src/main/java/com/kzzz3/argus/lens/session/LocalSessionStore.kt`

### Network files

- Split from: `data/src/main/java/com/kzzz3/argus/lens/data/network/NetworkClientFactory.kt`
- Create: `core/network/src/main/java/com/kzzz3/argus/lens/network/RetrofitFactory.kt`
- Create: `core/network/src/main/java/com/kzzz3/argus/lens/network/AuthInterceptor.kt`
- Create: `core/network/src/main/java/com/kzzz3/argus/lens/network/ApiResult.kt`
- Keep adapter: `data/src/main/java/com/kzzz3/argus/lens/data/network/NetworkClientFactory.kt`
- Move/update test: `core/network/src/test/java/com/kzzz3/argus/lens/network/RetrofitFactoryTest.kt`

### Database files

- Move: `data/src/main/java/com/kzzz3/argus/lens/data/local/ArgusLensDatabase.kt`
- Move: `data/src/main/java/com/kzzz3/argus/lens/data/local/ArgusLensMigrations.kt`
- Move: `data/src/main/java/com/kzzz3/argus/lens/data/local/LocalConversationDao.kt`
- Move: `data/src/main/java/com/kzzz3/argus/lens/data/local/LocalConversationEntity.kt`
- Move: `data/src/main/java/com/kzzz3/argus/lens/data/local/LocalMessageEntity.kt`
- Move: `data/src/main/java/com/kzzz3/argus/lens/data/local/LocalDraftAttachmentEntity.kt`
- Move: `data/src/test/java/com/kzzz3/argus/lens/data/local/ArgusLensDatabaseMigrationTest.kt`
- Move schema: `data/schemas/com.kzzz3.argus.lens.data.local.ArgusLensDatabase/1.json`
- Keep in data: `data/src/main/java/com/kzzz3/argus/lens/data/local/LocalConversationStore.kt`
- Keep in data: `data/src/test/java/com/kzzz3/argus/lens/data/local/LocalConversationStoreTest.kt`

---

## Task 1: Add core module shells and boundary tests

**Files:**
- Modify: `settings.gradle.kts`
- Create: `core/session/build.gradle.kts`
- Create: `core/network/build.gradle.kts`
- Create: `core/database/build.gradle.kts`
- Modify: `data/build.gradle.kts`
- Test: `app/src/test/java/com/kzzz3/argus/lens/app/ReleaseAndModuleBoundaryTest.kt`

- [ ] **Step 1: Write failing module-boundary test**

Update `ReleaseAndModuleBoundaryTest.moduleGraphKeepsProductBoundariesExplicit` so it expects:

```kotlin
listOf(":app", ":data", ":feature", ":core:model", ":core:ui", ":core:session", ":core:network", ":core:database").forEach { moduleName ->
    assertTrue(settings.contains("include(\"$moduleName\")"))
}
```

Expected dependency assertions after all module shells exist:

```kotlin
listOf(":data", ":feature", ":core:model", ":core:ui", ":core:session", ":core:network", ":core:database").forEach { dependencyName ->
    assertTrue(appBuildFile.contains("implementation(project(\"$dependencyName\"))"))
}
listOf(":core:model", ":core:session", ":core:network", ":core:database").forEach { dependencyName ->
    assertTrue(dataBuildFile.contains("implementation(project(\"$dependencyName\"))"))
}
```

- [ ] **Step 2: Run red test**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.kzzz3.argus.lens.app.ReleaseAndModuleBoundaryTest"
```

Expected: FAIL because `settings.gradle.kts` does not include `:core:session`, `:core:network`, or `:core:database`.

- [ ] **Step 3: Add module shells**

Append to `settings.gradle.kts`:

```kotlin
include(":core:session")
include(":core:network")
include(":core:database")
```

Create `core/session/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.kzzz3.argus.lens.session"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
}
```

Create `core/network/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.kzzz3.argus.lens.network"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.okhttp.sse)
    implementation(libs.gson)
    testImplementation(libs.junit)
}
```

Create `core/database/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.kzzz3.argus.lens.database"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
}
```

Add to `data/build.gradle.kts` dependencies:

```kotlin
implementation(project(":core:session"))
implementation(project(":core:network"))
implementation(project(":core:database"))
```

- [ ] **Step 4: Run green test**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.kzzz3.argus.lens.app.ReleaseAndModuleBoundaryTest"
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```powershell
$env:GIT_MASTER='1'; git add settings.gradle.kts core/session/build.gradle.kts core/network/build.gradle.kts core/database/build.gradle.kts data/build.gradle.kts app/src/test/java/com/kzzz3/argus/lens/app/ReleaseAndModuleBoundaryTest.kt
$env:GIT_MASTER='1'; git commit -m "Add Lens core infrastructure modules"
```

---

## Task 2: Extract core session contract and storage seams

**Files:**
- Move/split: `data/src/main/java/com/kzzz3/argus/lens/data/session/SessionRepository.kt`
- Split: `data/src/main/java/com/kzzz3/argus/lens/data/session/LocalSessionStore.kt`
- Create: `core/session/src/main/java/com/kzzz3/argus/lens/session/SessionRepository.kt`
- Create: `core/session/src/main/java/com/kzzz3/argus/lens/session/SessionCredentials.kt`
- Create: `core/session/src/main/java/com/kzzz3/argus/lens/session/SessionStateStore.kt`
- Create: `core/session/src/main/java/com/kzzz3/argus/lens/session/SecureTokenStore.kt`
- Create: `core/session/src/main/java/com/kzzz3/argus/lens/session/TokenCipher.kt`
- Modify imports across: `app/src/main/java`, `app/src/test/java`, `data/src/main/java`, `data/src/test/java`

- [ ] **Step 1: Write failing source-boundary test**

Extend `SessionBoundaryTest` with a source assertion:

```kotlin
@Test
fun sessionContractsLiveInCoreSessionModule() {
    val root = File("..").canonicalFile
    val coreSession = File(root, "core/session/src/main/java/com/kzzz3/argus/lens/session")
    assertTrue(File(coreSession, "SessionRepository.kt").exists())
    assertTrue(File(coreSession, "SessionCredentials.kt").exists())
    assertTrue(File(coreSession, "SessionStateStore.kt").exists())
    assertTrue(File(coreSession, "SecureTokenStore.kt").exists())
    assertTrue(File(coreSession, "TokenCipher.kt").exists())
}
```

- [ ] **Step 2: Run red test**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.kzzz3.argus.lens.app.SessionBoundaryTest"
```

Expected: FAIL because files do not exist under `core/session`.

- [ ] **Step 3: Move `SessionCredentials` and `SessionRepository`**

Create `core/session/src/main/java/com/kzzz3/argus/lens/session/SessionCredentials.kt`:

```kotlin
package com.kzzz3.argus.lens.session

data class SessionCredentials(
    val accessToken: String = "",
    val refreshToken: String = "",
) {
    val hasAccessToken: Boolean = accessToken.isNotBlank()
    val hasRefreshToken: Boolean = refreshToken.isNotBlank()
}
```

Create `core/session/src/main/java/com/kzzz3/argus/lens/session/SessionRepository.kt`:

```kotlin
package com.kzzz3.argus.lens.session

import com.kzzz3.argus.lens.model.session.AppSessionState

interface SessionRepository {
    suspend fun loadSession(): AppSessionState
    suspend fun loadCredentials(): SessionCredentials
    suspend fun saveSession(session: AppSessionState, credentials: SessionCredentials)
    suspend fun clearSession()
}
```

Remove duplicate definitions from the old data package and update imports from `com.kzzz3.argus.lens.data.session.*` to `com.kzzz3.argus.lens.session.*`.

- [ ] **Step 4: Split storage implementation**

Create `SessionStateStore`, `SecureTokenStore`, and `TokenCipher` by moving the corresponding private DataStore, SharedPreferences, and Android Keystore logic out of `LocalSessionStore.kt`. Preserve these names exactly:

```kotlin
private const val SESSION_DATASTORE_NAME = "argus-lens-session"
private const val SESSION_SNAPSHOT_PREFS = "argus-lens-session-snapshot"
private const val SECURE_SESSION_PREFS = "argus-lens-secure-session"
private const val KEYSTORE_ALIAS = "argus-lens-session-key"
```

`LocalSessionStore` in `core/session` should implement `SessionRepository` by delegating to the new stores. Keep helper factories:

```kotlin
fun createLocalSessionStore(context: Context): SessionRepository
fun createLocalSessionSnapshot(context: Context): AppSessionState
fun createLocalSessionCredentialsSnapshot(context: Context): SessionCredentials
```

- [ ] **Step 5: Run session tests**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.kzzz3.argus.lens.app.SessionBoundaryTest"
.\gradlew.bat :app:testDebugUnitTest --tests "com.kzzz3.argus.lens.app.AppSessionCoordinatorTest"
.\gradlew.bat :app:testDebugUnitTest --tests "com.kzzz3.argus.lens.worker.BackgroundSyncTaskTest"
.\gradlew.bat :data:testDebugUnitTest --tests "com.kzzz3.argus.lens.data.conversation.RemoteConversationRepositoryTest"
```

Expected: all commands report `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```powershell
$env:GIT_MASTER='1'; git add core/session data/src/main/java data/src/test/java app/src/main/java app/src/test/java app/src/test/java/com/kzzz3/argus/lens/worker
$env:GIT_MASTER='1'; git commit -m "Extract Lens core session boundary"
```

---

## Task 3: Extract BuildConfig-free network primitives

**Files:**
- Create: `core/network/src/main/java/com/kzzz3/argus/lens/network/RetrofitFactory.kt`
- Create: `core/network/src/main/java/com/kzzz3/argus/lens/network/AuthInterceptor.kt`
- Create: `core/network/src/main/java/com/kzzz3/argus/lens/network/ApiResult.kt`
- Modify: `data/src/main/java/com/kzzz3/argus/lens/data/network/NetworkClientFactory.kt`
- Move/update: `data/src/test/java/com/kzzz3/argus/lens/data/network/NetworkClientFactoryTest.kt` to `core/network/src/test/java/com/kzzz3/argus/lens/network/RetrofitFactoryTest.kt`

- [ ] **Step 1: Write failing boundary test**

Add to `ReleaseAndModuleBoundaryTest`:

```kotlin
@Test
fun coreNetworkDoesNotDependOnDataBuildConfig() {
    val retrofitFactory = File("../core/network/src/main/java/com/kzzz3/argus/lens/network/RetrofitFactory.kt")
    assertTrue(retrofitFactory.exists())
    assertFalse(retrofitFactory.readText().contains("BuildConfig"))
}
```

- [ ] **Step 2: Run red test**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.kzzz3.argus.lens.app.ReleaseAndModuleBoundaryTest.coreNetworkDoesNotDependOnDataBuildConfig"
```

Expected: FAIL because `RetrofitFactory.kt` does not exist.

- [ ] **Step 3: Create core network primitives**

Create `RetrofitFactory.kt` with explicit parameters:

```kotlin
package com.kzzz3.argus.lens.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

fun createNetworkGson(): Gson = GsonBuilder().create()

fun createNetworkHttpClient(enableVerboseHttpLogs: Boolean): OkHttpClient {
    return OkHttpClient.Builder()
        .apply {
            if (enableVerboseHttpLogs) {
                addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            }
        }
        .build()
}

fun createNetworkRetrofit(
    baseUrl: String,
    gson: Gson = createNetworkGson(),
    httpClient: OkHttpClient = createNetworkHttpClient(enableVerboseHttpLogs = false),
): Retrofit {
    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
}
```

Create `AuthInterceptor.kt`:

```kotlin
package com.kzzz3.argus.lens.network

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val accessTokenProvider: () -> String?,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = accessTokenProvider()?.takeIf { it.isNotBlank() }
        val request = if (token == null) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}
```

Create `ApiResult.kt`:

```kotlin
package com.kzzz3.argus.lens.network

sealed interface ApiResult<out T> {
    data class Success<T>(val body: T) : ApiResult<T>
    data class HttpError(val code: Int, val message: String, val errorBody: String?) : ApiResult<Nothing>
    data class NetworkError(val throwable: Throwable) : ApiResult<Nothing>
}
```

- [ ] **Step 4: Keep data adapter**

Update `data/network/NetworkClientFactory.kt` so `createAppRetrofit()` calls `createNetworkRetrofit(BuildConfig.AUTH_BASE_URL, ...)` and `createAppHttpClient()` calls `createNetworkHttpClient(BuildConfig.DEBUG)`.

- [ ] **Step 5: Run network tests**

Run:

```powershell
.\gradlew.bat :core:network:testDebugUnitTest
.\gradlew.bat :data:testDebugUnitTest --tests "com.kzzz3.argus.lens.data.auth.AuthServiceFactoryTest"
```

Expected: all commands report `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```powershell
$env:GIT_MASTER='1'; git add core/network data/src/main/java/com/kzzz3/argus/lens/data/network data/src/test/java app/src/test/java/com/kzzz3/argus/lens/app/ReleaseAndModuleBoundaryTest.kt
$env:GIT_MASTER='1'; git commit -m "Extract Lens core network primitives"
```

---

## Task 4: Extract Room database module

**Files:**
- Move: Room database, DAO, entities, migrations, schema, migration test.
- Modify: `data/src/main/java/com/kzzz3/argus/lens/data/local/LocalConversationStore.kt`
- Modify: `data/build.gradle.kts`

- [ ] **Step 1: Write failing database-boundary test**

Add to `ReleaseAndModuleBoundaryTest`:

```kotlin
@Test
fun coreDatabaseOwnsRoomDatabaseAndDataKeepsRepositoryStore() {
    val databaseFile = File("../core/database/src/main/java/com/kzzz3/argus/lens/database/ArgusLensDatabase.kt")
    val storeFile = File("../data/src/main/java/com/kzzz3/argus/lens/data/local/LocalConversationStore.kt")
    assertTrue(databaseFile.exists())
    assertTrue(storeFile.exists())
    assertFalse(storeFile.readText().contains("@Database"))
}
```

- [ ] **Step 2: Run red test**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.kzzz3.argus.lens.app.ReleaseAndModuleBoundaryTest.coreDatabaseOwnsRoomDatabaseAndDataKeepsRepositoryStore"
```

Expected: FAIL because `core/database/.../ArgusLensDatabase.kt` does not exist.

- [ ] **Step 3: Move pure Room files**

Move these files to package `com.kzzz3.argus.lens.database`:

```text
core/database/src/main/java/com/kzzz3/argus/lens/database/ArgusLensDatabase.kt
core/database/src/main/java/com/kzzz3/argus/lens/database/ArgusLensMigrations.kt
core/database/src/main/java/com/kzzz3/argus/lens/database/LocalConversationDao.kt
core/database/src/main/java/com/kzzz3/argus/lens/database/LocalConversationEntity.kt
core/database/src/main/java/com/kzzz3/argus/lens/database/LocalMessageEntity.kt
core/database/src/main/java/com/kzzz3/argus/lens/database/LocalDraftAttachmentEntity.kt
```

Keep class names unchanged for the first slice. Update imports in `LocalConversationStore.kt` from `com.kzzz3.argus.lens.data.local.*` to `com.kzzz3.argus.lens.database.*`.

- [ ] **Step 4: Move schema and migration test**

Move schema JSON to:

```text
core/database/schemas/com.kzzz3.argus.lens.database.ArgusLensDatabase/1.json
```

Move migration test to:

```text
core/database/src/test/java/com/kzzz3/argus/lens/database/ArgusLensDatabaseMigrationTest.kt
```

Update expected schema path and package names in the test.

- [ ] **Step 5: Remove Room from data build only when no annotations remain**

After imports compile, remove from `data/build.gradle.kts` only if no files under `data/src/main/java` use Room annotations:

```kotlin
alias(libs.plugins.ksp)
implementation(libs.androidx.room.runtime)
implementation(libs.androidx.room.ktx)
ksp(libs.androidx.room.compiler)
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
```

Keep them if any Room annotations remain in `:data`.

- [ ] **Step 6: Run database tests**

Run:

```powershell
.\gradlew.bat :core:database:testDebugUnitTest
.\gradlew.bat :data:testDebugUnitTest --tests "com.kzzz3.argus.lens.data.local.LocalConversationStoreTest"
.\gradlew.bat :app:testDebugUnitTest --tests "com.kzzz3.argus.lens.app.ReleaseAndModuleBoundaryTest"
```

Expected: all commands report `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```powershell
$env:GIT_MASTER='1'; git add core/database data/src/main/java/com/kzzz3/argus/lens/data/local data/build.gradle.kts app/src/test/java/com/kzzz3/argus/lens/app/ReleaseAndModuleBoundaryTest.kt
$env:GIT_MASTER='1'; git commit -m "Extract Lens core database module"
```

---

## Final Verification

Run Gradle serially:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lint
.\gradlew.bat assembleDebug
```

Expected: each command reports `BUILD SUCCESSFUL`.

Update:

- `PLAN.md` verification log.
- `docs/android-modernization-progress.md` P2/P3 module boundary sections.
- `docs/android-architecture-target.md` current baseline once each core module is physically extracted.
- `README.md` / `AGENTS.md` module navigation tables after module includes change.

---

## Self-Review

- Spec coverage: covers `core/session`, `core/network`, `core/database`, Gradle wiring, tests, docs, and serial verification.
- Placeholder scan: no incomplete-marker steps remain.
- Type consistency: module/package names are fixed as `com.kzzz3.argus.lens.session`, `com.kzzz3.argus.lens.network`, and `com.kzzz3.argus.lens.database`; existing class names stay compatible unless a task explicitly creates a new seam.
