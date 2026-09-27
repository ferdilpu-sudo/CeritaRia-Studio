# Phase 1 — Android Foundation

Status: **IMPLEMENTED / CI VERIFIED / PRODUCTION AUTH SMOKE PENDING**  
Implementation head verified: `4cbb292f065d051ce22156d9de304df310e37c59`  
CI run: `36294547068`

## Implemented

- single-module Android application;
- package `com.flyonz.ceritaria.studio`;
- minSdk 26;
- targetSdk 36;
- compileSdk 37;
- AGP 9.1.1 / Gradle 9.3.1 / JDK 17;
- Jetpack Compose + Material 3;
- Hilt dependency injection;
- Navigation Compose shell with Home, Series, Episodes, Analytics;
- Supabase Kotlin client behind `SupabaseClientProvider`;
- `AuthRepository` boundary;
- session restoration;
- email/password sign-in;
- sign-out;
- production-compatible `admin_users` membership gate;
- missing-configuration screen;
- unauthorized-account screen;
- app-level result/error taxonomy;
- login validation unit tests;
- GitHub Actions Android verification.

## Security

The repository contains no:

- Supabase service-role key;
- R2 access key/secret;
- signing credential;
- hardcoded admin password.

Only public client configuration is expected at build/runtime through:

```text
CERITARIA_SUPABASE_URL
CERITARIA_SUPABASE_PUBLISHABLE_KEY
```

They are accepted as Gradle properties or environment variables and default to empty so the project can compile safely without secrets.

## CI evidence

GitHub Actions run `36294547068` completed successfully.

Verified tasks:

```text
lintDebug
testDebugUnitTest
assembleDebug
```

The CI runner installs Android SDK Platform 37.0 and provides an `android-37` compatibility alias because the current command-line SDK repository publishes the platform directory as `android-37.0`.

## Pending production smoke gate

CI verifies compilation, lint, and unit behavior, but it cannot prove real Supabase authentication without the production public client configuration.

Before marking Phase 1 fully complete, run an end-to-end smoke test that confirms:

1. configured app opens login;
2. valid admin can sign in;
3. session restores after app restart;
4. own `admin_users` membership is readable through production RLS;
5. non-admin authenticated account is blocked;
6. sign-out returns to login.

No production credential should be committed just to satisfy this test.

## Next code phase

Phase 2 read-only catalog code may be prepared from the already verified schema, but production integration should retain the same explicit configuration gate.
