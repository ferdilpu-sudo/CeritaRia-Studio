# Ceritaria Studio Android

Native Android administration app for Ceritaria.

## Phase status

- Phase 0: Production Contract Audit ✅
- Phase 1: Android Foundation 🟡 implementation + CI complete; production auth smoke test pending configuration

## Toolchain

- JDK 17
- compileSdk 37
- minSdk 26
- targetSdk 36
- Android Gradle Plugin 9.1.1
- Gradle 9.3.1
- Jetpack Compose
- Hilt
- Supabase Kotlin

`compileSdk` is intentionally newer than `targetSdk`: current Compose artifacts require API 37 for compilation, while runtime target behavior remains API 36.

## Local configuration

Provide these values as Gradle properties or environment variables:

```text
CERITARIA_SUPABASE_URL=https://<project>.supabase.co
CERITARIA_SUPABASE_PUBLISHABLE_KEY=<publishable-or-anon-key>
```

Do not commit service-role keys, R2 secrets, or signing credentials.

## Build

With Gradle 9.3.1 available:

```bash
gradle lintDebug testDebugUnitTest assembleDebug
```

The repository CI installs Gradle 9.3.1 explicitly, so it does not depend on a machine-wide Gradle version.
