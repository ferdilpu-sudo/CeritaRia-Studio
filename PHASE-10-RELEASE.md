# Phase 10 — Release

Status: **AUTOMATED RELEASE PACKAGING READY; MANUAL SIGNED CANDIDATE + PRODUCTION SMOKE PENDING**  
Date: **2026-09-28**

## Automated gate

The Android repository now has a manual `Android Release Candidate` workflow that:

1. accepts explicit versionCode/versionName;
2. requires the Supabase URL, publishable key, API base URL, and signing secrets;
3. requires HTTPS release endpoints;
4. decodes the keystore only into the temporary GitHub runner directory;
5. runs Android source/security guardrails;
6. runs lint and unit tests;
7. builds release APK and AAB;
8. verifies the APK signature with `apksigner`;
9. verifies the AAB signature with `jarsigner -strict`;
10. generates SHA-256 checksums;
11. publishes APK, AAB, and checksum as a 30-day artifact.

The workflow has `contents: read` token permission and never receives service-role or R2 access credentials.

Normal Android CI run `36364998006` passed after the release-workflow hardening commit.

## Signing boundary

Release signing values are read only from Gradle properties/environment variables.

The repository does not contain:

- keystore bytes;
- keystore password;
- signing key password;
- signing alias secret;
- Supabase service-role key;
- R2 access key;
- R2 secret key.

The Supabase publishable key is an allowed client value. Privileged authorization remains enforced by Supabase RLS/admin checks and the Ceritaria server.

## Shrinker decision

`minifyEnabled = false` is intentional for the first internal production candidate.

Therefore Phase 10 does not claim a ProGuard/R8 compatibility pass. If minification is enabled later, it becomes a new release gate with dedicated release-build and runtime smoke verification.

## Backend compatibility checkpoint

The current Ceritaria backend includes additive migrations through:

```text
005_episode_reorder_rpc.sql
006_video_upload_assets.sql
007_create_video_upload_records.sql
008_video_upload_state_transitions.sql
009_video_upload_part_size_bigint.sql
010_episode_r2_video.sql
```

The backend also exposes the server-mediated R2 upload session, multipart-part authorization, complete/finalize/cancel/status, READY preview, and episode attachment routes consumed by Studio.

The latest audited backend head is CI green. Phase 10 does not introduce another schema or competing media contract.

## Manual candidate procedure

1. configure the documented GitHub release secrets;
2. choose a new monotonically increasing versionCode and versionName;
3. run **Android Release Candidate** manually;
4. download APK, AAB, and `SHA256SUMS.txt`;
5. verify checksums before distributing;
6. install the signed APK on a controlled device;
7. execute the smoke sequence in `RELEASE-OPERATIONS.md`.

## Required smoke

The candidate is not production-verified until it passes, against the real environment:

- launch/login/session restore/admin gate;
- Series/Episode/Analytics reads;
- one controlled draft mutation;
- image upload/replace/remove;
- local video inspect/encode on representative hardware;
- real R2 single and multipart upload;
- interruption/retry;
- expired session behavior;
- READY preview;
- controlled episode attachment;
- public web/PWA playback/compatibility;
- unauthorized/non-admin rejection;
- process recreation during media work;
- accessibility/focus/touch targets.

## Rollback

Use `RELEASE-OPERATIONS.md` as the authoritative rollback procedure.

For a distributed rollback, rebuild the last-known-good source with a higher versionCode using the same signing identity. Do not assume Android will install a lower versionCode over the candidate.

Schema migrations are additive and already consumed by backend/client code, so an app rollback should not blindly down-migrate production.

## Exit gate

Automated release engineering: **PASS**.

Signed candidate and production/device validation: **PENDING**.

Do not label the Android app production-verified until the manual candidate checklist is completed.
