# Phase 9 — Testing & Hardening

Status: **AUTOMATED CODE / CI PASS; ENVIRONMENT/DEVICE GATES PENDING**  
Date: **2026-09-28**  
Final hardened verification: **GitHub Actions 36364998006**

## What was hardened

Phase 9 focused on failure behavior and deterministic execution rather than adding new product features.

### Authentication and authorization behavior

Upload recovery now distinguishes remote-session failure from authentication failure. Server responses such as `UNAUTHENTICATED`, `FORBIDDEN`, and server lookup failures are propagated instead of being converted into a fresh upload session.

Automatic upload retry is disabled for auth/configuration/invalid-source failures and remains enabled for transient network/R2/server/session conditions.

### Duplicate mutation prevention

Busy state is set synchronously before coroutine launch for login, R2 attachment/preview, Series save, Episode save, and episode reorder actions. Regression tests cover rapid duplicate calls.

### Durable upload failure behavior

Tests verify that:

- finalize failure keeps prepared encoded output for retry;
- multipart failure after a completed part preserves that part and ETag;
- previous prepared media is not deleted until remote READY;
- stale or missing remote sessions can recover to a new session;
- auth failures do not trigger destructive session-reset behavior.

### Source availability

Missing or inaccessible local video sources are mapped to `SOURCE_NOT_READY`, making them non-retryable rather than repeatedly attempting an impossible transfer.

### CI security guardrails

CI rejects:

- privileged server credential identifiers in Android source;
- potential logging of access/refresh tokens, Authorization/Bearer values, or passwords;
- JWT-like literals in Android source;
- handwritten Kotlin files above the hard size limit.

### Room/KSP reproducibility

The previous raw KSP `room.schemaLocation` argument allowed debug and release schema-generation tasks to race on the same JSON output when Gradle parallelism was enabled. The build now uses the official Room Gradle Plugin and `room { schemaDirectory(...) }`, which gives schema generation proper variant-aware build inputs/outputs.

### CI maintenance

Workflow actions were upgraded to supported Node 24 releases and workflow concurrency now cancels superseded main-branch verification runs.

## Automated gate

The normal Android workflow verifies:

```text
lintDebug
testDebugUnitTest
assembleDebug
assembleRelease
```

plus source/security guardrails.

## Not proven by CI

The following still need release-candidate environment/device checks:

- real Supabase RLS behavior;
- real Storage and R2 credentials/contracts;
- network interruption on real transfer;
- real token expiry;
- hardware MediaCodec behavior;
- device accessibility/focus/touch targets;
- controlled web/PWA compatibility after production mutation.

These remain release gates rather than being silently declared complete from mocks.

## Final automated result

The hardened head, including the release-workflow permission/signature/checksum changes, passed the normal Android CI workflow in run `36364998006`:

```text
source/security guardrails
lintDebug
testDebugUnitTest
assembleDebug
assembleRelease
```

Phase 9 automated code/build hardening is therefore complete. The remaining list above moves forward as Phase 10 release-candidate smoke work.
