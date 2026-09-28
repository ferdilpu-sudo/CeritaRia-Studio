# Ceritaria Studio Android

Native Android administration app for Ceritaria.

## Phase status

- Phase 0: Production Contract Audit ✅
- Phase 1: Android Foundation 🟡 code/CI pass; production auth smoke pending
- Phase 2: Read-only Catalog 🟡 code/CI pass; production catalog smoke pending
- Phase 3: Content Mutations 🟡 code/CI pass; production migration/mutation smoke pending
- Phase 4: Image Media 🟡 code/CI pass; production Storage smoke pending
- Phase 5: Local Video Pipeline 🟡 code/CI pass; real-device codec smoke pending
- Phase 6: Secure R2 Upload Contract 🟡 code/CI pass; real R2/environment smoke pending
- Phase 7: Episode Video Integration 🟡 code/CI pass; production web/PWA attachment smoke pending
- Phase 8: Analytics 🟡 code/CI pass; production analytics smoke pending
- Phase 9: Testing & Hardening ✅ automated code/CI pass
- Phase 10: Release 🟡 signed-candidate automation ready; manual candidate + device/environment smoke pending

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
CERITARIA_API_BASE_URL=https://<ceritaria-backend>
```

Do not commit service-role keys, R2 secrets, or signing credentials.

## Build

With Gradle 9.3.1 available:

```bash
gradle lintDebug testDebugUnitTest assembleDebug assembleRelease
```

The repository CI installs Gradle 9.3.1 explicitly, so it does not depend on a machine-wide Gradle version.

## Internal release candidate

A signed internal candidate is built manually through the **Android Release Candidate** GitHub Actions workflow. Required signing/environment values live in GitHub Secrets, not in the repository.

The workflow validates release configuration, builds APK/AAB, verifies their signatures, generates SHA-256 checksums, and publishes the files as a 30-day Actions artifact.

See `RELEASE-OPERATIONS.md` for the controlled smoke and rollback procedure.
