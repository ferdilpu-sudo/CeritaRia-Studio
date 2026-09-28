# Ceritaria Studio Android — Revised Implementation Plan

Status: **Implementation baseline — encode + R2 included in core delivery**  
Target: **Native Android administration application for Ceritaria**  
Source of truth: **existing Ceritaria web repository, Supabase project, and verified production migrations**

---

## 1. Product Direction

Ceritaria Studio is a native Android administration client for the same Ceritaria catalog used by the public web/PWA and the existing CMS.

Studio is **not** a second CMS database and must not duplicate production content. Existing Ceritaria production data remains the single source of truth.

The application extends the current operational model to Android with a mobile-first workflow for:

- series management;
- episode management;
- draft/publish workflow;
- artwork management;
- external video provider management;
- local video inspection and preparation;
- direct R2 video upload through a trusted server contract;
- read-only analytics.

The Android app must remain compatible with the existing Ceritaria web application throughout development and release.

---

## 2. Locked Product Decisions

These decisions are authoritative for this implementation plan.

### 2.1 Existing Ceritaria backend remains authoritative

- Supabase Auth, Postgres, Storage, RLS, existing RPCs/views, and current web behavior remain the production contract.
- Android does not invent replacement tables or fields because they would be convenient.
- Production schema must be audited before persistence code is finalized.

### 2.2 Direct video encoding + R2 upload is part of the core delivery

Direct video upload is **not** a future placeholder in this baseline.

However, the feature is gated by a secure server-side upload contract. Android may implement local inspection/encoding before the server contract is complete, but production R2 upload cannot ship until authorization and finalization are verified.

### 2.3 Privileged credentials never ship in the APK

Forbidden in Android resources, BuildConfig, DataStore, Room, source code, logs, or assets:

- Supabase service-role key;
- R2 account access key/secret;
- backend private signing credentials;
- hardcoded admin credentials.

Android authenticates as a normal user. Server/RLS policy determines authorization.

### 2.4 Room is required for operational video jobs

Room is used as a local operational job store for long-running video work.

It stores only local pipeline state such as:

- `VideoJob`;
- encode status/progress;
- upload status/progress;
- multipart/resume metadata where required;
- temporary media metadata;
- retry/recovery information.

Room is **not** a second database for series or episodes. Supabase remains source of truth for production content.

### 2.5 Encoding and upload are separate jobs and separate states

The app must never represent encoding as uploading or vice versa.

Typical pipeline:

```text
Selected
  -> Inspecting
  -> ReadyWithoutEncoding OR QueuedForEncoding
  -> Encoding
  -> EncodedReady
  -> QueuedForUpload
  -> Uploading
  -> Verifying
  -> Ready
```

Failure or cancellation at one stage must not silently corrupt another stage.

### 2.6 Existing video remains active until replacement is READY

A replacement video follows an atomic user-visible workflow:

1. prepare new local video;
2. upload new object;
3. server verifies object;
4. server marks media asset READY;
5. episode reference changes to the new READY asset;
6. old asset is cleaned only when safe.

A failed replacement must not destroy the currently valid episode video.

---

## 3. Primary Goals

1. View every existing series and episode from Android.
2. Search and filter catalog content efficiently.
3. Create and edit series.
4. Create and edit episodes.
5. Publish, unpublish, soft-delete, and restore where production supports it.
6. Reorder episodes safely.
7. Manage cover, hero, and thumbnail images.
8. Preview metadata, artwork, and video state before publishing.
9. Edit existing YouTube/Facebook provider references.
10. Select and inspect local video.
11. Skip re-encoding when source already meets the streaming contract.
12. Encode incompatible video using AndroidX Media3 Transformer.
13. Upload prepared video securely to R2 through server-mediated authorization.
14. Resume/retry recoverable video transfers without unnecessary re-encoding.
15. Persist long-running video-job state across navigation and process loss.
16. Inspect basic first-party Ceritaria analytics.
17. Keep all writes compatible with the public web/PWA and existing CMS.

---

## 4. Non-goals for the Initial Release

The following are intentionally outside the first production release:

- rebuilding the public Ceritaria viewer app;
- timeline-based video editing;
- transitions/effects editor;
- AI video generation;
- AI script generation;
- social-media cross-posting;
- replacing Supabase with a second backend;
- desktop-class analytics;
- advanced team/approval workflow unless already supported by production;
- custom CDN/player architecture beyond the verified Ceritaria delivery strategy;
- embedding privileged R2/Supabase credentials in Android;
- changing production schema without an explicit migration and compatibility review.

---

## 5. Technical Baseline

### Android

- Kotlin
- Jetpack Compose
- Material 3
- Single Activity
- Navigation Compose
- MVVM + unidirectional data flow
- Coroutines + Flow
- Hilt

### Remote data

- Supabase Kotlin client behind repository/data-source interfaces
- Existing Supabase Auth
- Existing Postgres/RLS contract
- Existing Supabase Storage for current image workflow
- Dedicated Ceritaria server endpoints where privileged R2 operations are required

### Local persistence

- DataStore for small preferences only
- Room for durable local video-job state

### Media

- Android system Photo Picker / document-media selection APIs as appropriate
- Persist URI access when required by long-running work
- Coil for images
- AndroidX Media3 Transformer for video transformation
- MediaCodec-backed paths where supported by the device

### Background work

Image transfer and short durable work may use WorkManager.

Video processing and large user-initiated transfers must use the execution mechanism appropriate to the supported Android version and job type. The architecture must not assume one generic Worker is correct for every long-running media operation.

Encoding and transfer remain separate responsibilities regardless of execution mechanism.

---

## 6. Architecture Constraints

### 6.1 Dependency direction

```text
Compose UI
   -> ViewModel
      -> Use case / Repository interface
         -> Repository implementation
            -> Supabase data source / Ceritaria server API / local job store
```

The UI never talks directly to Supabase SDK classes, Room DAOs, R2, or Media3 implementation classes.

### 6.2 Feature-first structure

Use feature/domain-oriented packages rather than one giant technical-layer directory.

Recommended baseline:

```text
app/
  src/main/java/.../
    app/
    navigation/

    core/
      auth/
      designsystem/
      error/
      network/
      database/
      media/
      upload/
      storage/
      util/

    feature/
      login/
      home/
      series/
        list/
        detail/
        editor/
      episode/
        list/
        detail/
        editor/
        reorder/
      media/
      analytics/
      settings/
```

Start with one Gradle app module. Split Gradle modules only when build scale or team ownership provides a concrete reason.

### 6.3 File responsibility

One file has one clear responsibility.

Target limits for handwritten Kotlin:

- normal `.kt` file: target `<= 200`, hard `<= 300` lines;
- screen composable: target `<= 100`, hard `<= 150` lines;
- reusable composable: target `<= 80` lines;
- ViewModel: target `<= 180`, hard `<= 250` lines;
- Worker/service/job adapter: target `<= 180`, hard `<= 250` lines;
- function: target `<= 50`, hard `<= 80` lines unless declarative data justifies otherwise.

Generated files are exempt.

Never split a tangled responsibility into arbitrary `Part1`, `Part2`, or generic `Utils` files merely to satisfy a line count.

---

## 7. Video Pipeline Boundaries

The following responsibilities must remain separate.

```text
core/media/
  VideoMetadata.kt
  VideoInspector.kt
  VideoCompatibilityChecker.kt
  StreamingPreset.kt
  VideoEncoder.kt
  Media3VideoEncoder.kt
  EncodingProgress.kt
  TemporaryMediaStore.kt

core/upload/
  UploadSession.kt
  UploadProgress.kt
  VideoUploadRepository.kt
  R2UploadDataSource.kt
  MultipartUploadState.kt

core/database/videojob/
  VideoJobEntity.kt
  VideoJobDao.kt
  VideoJobMapper.kt
  VideoJobRepository.kt

feature/episode/editor/video/
  EpisodeVideoSection.kt
  EpisodeVideoUiState.kt
  EpisodeVideoController.kt
```

Do not create a `VideoManager`, `MediaHelper`, or `UploadAndEncodeService` that mixes inspection, transcoding, transfer, database mutation, and UI state.

---

## 8. Delivery Strategy

Development proceeds in vertical, verifiable phases.

Do not implement frontend, backend contract, media engine, and analytics simultaneously. Each phase must exit with a working build and its own acceptance gate.

---

## Phase 0 — Production Contract Audit ✅ COMPLETE

Audit completed on **2026-09-27** against `ferdilpu-sudo/ceritaria` branch `main`, tree SHA `c7ea336224a5baaf37af3645ff375981051a8b31`.

Detailed evidence and findings are recorded in `PHASE-0-AUDIT.md`. The verified Android-facing contract is now in `SCHEMA.md`.

### Verified outcomes

- exact `admin_users`, `series`, `episodes`, and `analytics_events` fields recorded;
- RLS/admin authorization confirmed;
- image buckets, MIME limits, size limits, and current object-path convention confirmed;
- CMS create/update/publish/delete behavior confirmed;
- publish-state mapping confirmed from `is_published`, `published_at`, and `deleted_at`;
- YouTube/Facebook provider constraint confirmed;
- analytics RPC contract confirmed;
- restore confirmed as unsupported by current CMS;
- no dedicated reorder RPC exists;
- no production R2/video asset schema exists;
- no R2 upload/finalization API routes exist.

### Required later backend work

R2 delivery requires a backward-compatible production extension because `episodes.video_provider` currently allows only `youtube|facebook` and no `video_assets` entity exists.

Drag reorder must not ship until a conflict-safe server/RPC strategy exists for the unique `(series_id, episode_number)` constraint.

### Exit gate

**PASS.** Phase 1/2 may use the verified production contract without inventing schema fields.

---

## Phase 1 — Android Foundation 🟡 IMPLEMENTED / PRODUCTION SMOKE PENDING

Implementation is committed and CI verified. GitHub Actions run `36294547068` passed `lintDebug`, `testDebugUnitTest`, and `assembleDebug` on the Android 37 compile toolchain.

The only remaining exit-gate item is a production Supabase authentication smoke test after `CERITARIA_SUPABASE_URL` and `CERITARIA_SUPABASE_PUBLISHABLE_KEY` are supplied through secure local/CI configuration. Those values are intentionally not committed to this public repository.

### Scope

Create the stable application shell before business features.

### Tasks

- create Android project;
- configure build variants/environment values;
- add Hilt;
- add Navigation Compose;
- create design tokens and reusable primitives;
- implement common `AppResult`/error taxonomy;
- configure Supabase client behind an injectable boundary;
- implement session restoration;
- implement admin authorization check;
- create authenticated navigation shell;
- add baseline logging without token/PII leakage.

### Implementation status

- [x] Android project and Gradle toolchain created;
- [x] build variants/environment input boundary created;
- [x] Hilt configured;
- [x] Navigation Compose shell created;
- [x] baseline Material 3 design system created;
- [x] common `AppResult` / `AppError` taxonomy created;
- [x] Supabase client isolated behind an injectable provider;
- [x] session restoration implemented;
- [x] `admin_users` authorization check implemented;
- [x] authenticated four-destination shell created;
- [x] missing-config and unauthorized blocking states created;
- [x] login validation unit tests added;
- [x] `lintDebug`, `testDebugUnitTest`, and `assembleDebug` pass in CI;
- [ ] production Supabase sign-in/session/admin-gate smoke test with configured public client values.

### Exit gate

Code/build gate: **PASS**. Production integration smoke gate: **PENDING CONFIGURATION**.

An authorized admin must still be smoke-tested against the real Supabase project to confirm session restoration and `admin_users` membership access end-to-end before Phase 1 receives a final green completion marker.

---

## Phase 2 — Read-only Catalog 🟡 IMPLEMENTED / PRODUCTION SMOKE PENDING

Implementation is committed and CI verified. GitHub Actions run `36297938559` passed `lintDebug`, `testDebugUnitTest`, and `assembleDebug`.

The remaining exit-gate item is an end-to-end read-only smoke test against the real Ceritaria Supabase project after the public client configuration is supplied securely. No production catalog credentials are committed to this public repository.

### Scope

Prove data compatibility before enabling mutations.

### Implementation status

- [x] series list from the verified production schema;
- [x] backend range pagination with one-row lookahead;
- [x] debounced series search;
- [x] published/draft/featured series filters;
- [x] series detail;
- [x] episode list;
- [x] episode list filtered by parent series;
- [x] backend episode pagination;
- [x] debounced episode search;
- [x] episode status/provider/series filters;
- [x] episode detail;
- [x] YouTube/Facebook provider labels;
- [x] unknown future provider values preserved safely;
- [x] draft/published/unpublished/deleted state mapping;
- [x] pull-to-refresh;
- [x] loading/content/empty/error states;
- [x] cover and thumbnail network artwork with fallback;
- [x] DTO -> mapper -> domain boundaries;
- [x] repository data-source interfaces for testability;
- [x] mapper/provider/publish-state/repository/ViewModel unit tests;
- [x] changed handwritten files remain within responsibility/line-count limits;
- [x] `lintDebug`, `testDebugUnitTest`, and `assembleDebug` pass in CI;
- [ ] production Supabase catalog/RLS smoke test with configured public client values.

### Exit gate

Code/build gate: **PASS**. Production integration smoke gate: **PENDING CONFIGURATION**.

Before Phase 2 receives a final green completion marker, an authorized admin must verify against the real production backend that series and episode lists, pagination, search/filter behavior, Series -> Episodes navigation, detail data, RLS access, and artwork URLs match the existing Ceritaria catalog without mutation.

---

## Phase 3 — Content Mutations 🟡 IMPLEMENTED / PRODUCTION MIGRATION + SMOKE PENDING

Android mutation code is implemented and CI verified. Series and episode create/edit/publish/unpublish/delete flows use the existing production contract. Episode reorder uses a new conflict-safe backend migration committed to the Ceritaria repository as `supabase/migrations/005_episode_reorder_rpc.sql`.

The code/build gate is complete. Production completion still requires applying migration 005 to the real Supabase project and smoke-testing mutations with an authorized admin account.

### Scope

Implement production-compatible editing and publishing.

### Implementation status

- [x] create/edit series;
- [x] create/edit episode;
- [x] draft save;
- [x] publish;
- [x] unpublish while preserving existing `published_at`;
- [x] featured toggle;
- [x] series soft delete through `soft_delete_series`;
- [x] episode soft delete through verified direct-update semantics;
- [x] restore intentionally omitted because production still does not support it;
- [x] conflict-safe episode reorder Android contract and UI;
- [x] backend reorder RPC migration authored and CI verified in `ferdilpu-sudo/ceritaria`;
- [x] validation aligned with current web CMS rules, including provider-specific YouTube/Facebook URL validation;
- [x] duplicate-save protection;
- [x] dirty-form protection;
- [x] success/failure feedback;
- [x] mutation/navigation files remain within responsibility and line-count rules;
- [x] Android `lintDebug`, `testDebugUnitTest`, and `assembleDebug` pass after mutation implementation;
- [ ] apply `005_episode_reorder_rpc.sql` to production Supabase;
- [ ] production admin smoke test for create/edit/publish/unpublish/delete/reorder;
- [ ] confirm resulting changes in existing Ceritaria web/PWA without manual synchronization.

### Reorder contract

The reorder RPC accepts the complete ordered list of active episode IDs for one series. It validates authorization, membership, count, and duplicates, temporarily moves active rows outside the existing number range, then reassigns the original set of active `episode_number` values in the requested order.

Preserving the existing number set avoids collisions with soft-deleted rows that still retain historical episode numbers under the unique `(series_id, episode_number)` constraint.

### Exit gate

Code/build gate: **PASS after final reorder CI**. Production integration gate: **PENDING MIGRATION + CONFIGURATION**.

Changes made from Android must still be smoke-tested against the real Supabase project and confirmed in Ceritaria web/PWA before Phase 3 receives a final green completion marker.

---

## Phase 4 — Image Media 🟡 IMPLEMENTED / PRODUCTION SMOKE PENDING

Implementation is committed and CI verified. GitHub Actions run `36305633832` passed the repository Verify step after the final Phase 4 cleanup.

### Scope

Complete the current image workflow before the heavier video pipeline.

### Implementation status

- [x] system Photo Picker with image-only selection;
- [x] no broad gallery/storage permission;
- [x] best-effort persisted URI read access before durable work;
- [x] JPEG/PNG/WebP validation;
- [x] production 5 MiB size limit enforced from metadata and streamed copy;
- [x] source preparation without loading the complete image into memory;
- [x] Series cover upload to `series-media`;
- [x] Series hero upload to `series-media`;
- [x] Episode thumbnail upload to `episode-media`;
- [x] WorkManager-backed durable replace/remove work;
- [x] network constraint, retry, cancel, and progress;
- [x] byte-based Supabase Storage upload progress;
- [x] artwork preview;
- [x] safe replace/remove flow;
- [x] DB reference updated only after new upload succeeds;
- [x] newly uploaded object rollback attempted when DB update fails;
- [x] old owned object cleaned only after new DB reference succeeds;
- [x] cleanup restricted to configured Supabase host + expected bucket;
- [x] external legacy/manual URLs are not deleted;
- [x] cleanup failure cannot invalidate a correct new DB reference;
- [x] cancellation is propagated rather than converted into retry;
- [x] new content must be saved once before media upload is enabled;
- [x] media success synchronizes current + initial editor forms;
- [x] obsolete manual Series media-URL editor removed;
- [x] validator, cleanup, replacement-order, rollback, cancellation, and ViewModel tests;
- [x] handwritten files remain within responsibility/line-count limits;
- [x] final Phase 4 CI run `36305633832` succeeded;
- [ ] production admin smoke test for upload/replace/remove/retry/cancel;
- [ ] verify resulting artwork in existing Ceritaria web/PWA.

### Replacement safety contract

```text
select image
  -> validate/copy
  -> upload new object
  -> update one DB media reference
  -> best-effort cleanup old owned object
```

Upload failure leaves the previous DB reference untouched. DB-reference failure triggers best-effort rollback of the new object. Cleanup never happens before the new reference is valid.

### Exit gate

Code/build gate: **PASS**. Production integration smoke gate: **PENDING CONFIGURATION**.

Before Phase 4 receives a final green completion marker, an authorized admin must run the configured APK against the real Supabase project and verify upload/replace/remove behavior and resulting web/PWA artwork.

---

## Phase 5 — Local Video Pipeline 🟡 IMPLEMENTED / REAL-DEVICE SMOKE PENDING

The local preparation pipeline is implemented with Media3 1.11.1 and Room 2.8.5 and does not depend on the R2 server contract.

### Scope

Build video inspection and encoding without depending on a finished R2 production contract.

### Implementation status

- [x] system video picker and persisted source URI access;
- [x] Media3 Inspector metadata retrieval for duration, dimensions, rotation, FPS, codecs, audio metadata, and size;
- [x] source fingerprint + durable `VideoJob` in Room;
- [x] pure compatibility checker and streaming preset;
- [x] compatible MP4/H.264/AAC source skips lossy re-encode;
- [x] orientation-independent max short side 1080 / long side 1920;
- [x] landscape 1920×1080 and portrait 1080×1920 accepted unchanged;
- [x] oversized input downscaled without stretch;
- [x] Media3 Transformer H.264/AAC encoder with AAC 128 kbps target;
- [x] 30 fps cap, progress, cancellation, partial-output cleanup;
- [x] insufficient-storage guard;
- [x] unsupported codec distinguished from generic export failure;
- [x] encoded output stored in app-owned no-backup storage;
- [x] READY encoded output retained for future upload/retry with no blind age-only cleanup;
- [x] interrupted ENCODING state and missing READY output recover to QUEUED;
- [x] active live ENCODING state is not mistaken for stale process-death state;
- [x] Episode editor local-video UI with source metadata, prepare/progress/cancel/error states;
- [x] existing YouTube/Facebook editing remains separate;
- [x] new episode must have a stable ID before local video job creation;
- [ ] representative real-device MediaCodec smoke test.

### Local state flow

```text
Selected
  -> Inspecting
  -> READY_WITHOUT_ENCODING
     or
  -> READY_TO_ENCODE
  -> ENCODING
  -> ENCODED_READY
```

Encoding and upload remain separate. `ENCODED_READY` means only that a valid local file is available for Phase 6.

### Recovery and retention

Room is the durable operational source of truth. Process death while encoding resets the stale job to `QUEUED` and removes partial output. READY encoded output is retained until an explicit later lifecycle owner removes it after replacement/discard or successful upload/finalization.

### Exit gate

Code/build gate: **PASS**. GitHub Actions run `36308774838` completed successfully. Real-device behavior gate: **PENDING**.

No R2 credential is required for this phase.

---

## Phase 6 — Secure R2 Upload Contract 🟡 CODE / CI PASS · PRODUCTION SMOKE PENDING

The trusted server upload boundary and Android transfer pipeline are implemented end-to-end without embedding permanent R2 credentials in the APK.

### Server implementation

- [x] additive `video_assets` and `video_upload_sessions` schema;
- [x] admin-only RLS;
- [x] canonical server-generated asset/session IDs and object keys;
- [x] bearer Supabase admin authentication for Android API routes;
- [x] server-only R2 account credentials;
- [x] server-configured size limit, single-upload threshold, part size, URL TTL, and session TTL;
- [x] single presigned PUT path;
- [x] multipart initiation, per-part authorization, completion, and abort;
- [x] server-side HEAD verification before READY;
- [x] invalid uploaded object fails instead of becoming READY;
- [x] recovery/status endpoint;
- [x] explicit cancel endpoint;
- [x] upload completion never mutates or publishes an episode;
- [x] backend CI `36318207100` passed typecheck, lint, and tests.

### Android implementation

- [x] authenticated Ceritaria upload API client using current Supabase access token;
- [x] direct byte transfer to short-lived presigned R2 URLs;
- [x] source range streaming without loading a whole video into memory;
- [x] single PUT upload;
- [x] multipart upload with persisted completed-part ETags;
- [x] resumable missing-part retry;
- [x] remote status recovery for expired/terminal/READY sessions;
- [x] Room upload-session and multipart state ownership;
- [x] persisted uploaded bytes and total bytes;
- [x] server finalize -> VERIFYING -> READY flow;
- [x] API 34+ User-Initiated Data Transfer Job scheduling;
- [x] pre-API-34 foreground WorkManager fallback with dataSync service type;
- [x] notification/progress updates;
- [x] explicit upload cancel;
- [x] Episode editor Upload / Retry / Cancel controls;
- [x] READY remote asset shown in editor;
- [x] encoded temporary output removed only after remote asset becomes READY;
- [x] Android CI `36318085202` passed the final Phase 6 code state.

### Runtime flow

```text
prepared local video
  -> schedule transfer
  -> request server upload session
  -> SINGLE PUT
     or
     MULTIPART missing-part transfer
  -> complete multipart when required
  -> server HEAD verification
  -> local VERIFYING
  -> remote asset READY
  -> local upload state READY
```

### Recovery contract

A retry does not automatically force re-encoding.

For multipart uploads, completed part ETags remain in Room and only missing parts are transferred again.

If the remote session is expired, terminal, or unusable, Android abandons the local session metadata and requests a fresh server session.

If the remote asset is already READY, Android adopts that READY state rather than re-uploading.

### Execution strategy

- Android 14 / API 34 and newer: user-initiated transfer uses `JobScheduler.setUserInitiated(true)`.
- Older supported Android versions: foreground WorkManager performs the transfer under a network constraint.
- Transfer progress is durable in Room; UI navigation is not the owner of the upload lifecycle.

### Security boundary

Android never stores or receives permanent R2 credentials. It receives only scoped, short-lived operation URLs from the trusted Ceritaria server.

The server owns:

- admin authorization;
- R2 request signing;
- canonical object keys;
- upload policy;
- object verification;
- final READY transition.

### Production gate still pending

Code/build gate: **PASS**.

Production completion requires applying/configuring the real infrastructure and running smoke tests:

1. apply Phase 6 Supabase migrations;
2. configure all R2 server variables and the production bucket;
3. verify unauthorized/non-admin requests are rejected;
4. run one SINGLE upload below the configured threshold;
5. run one MULTIPART upload above the threshold;
6. interrupt multipart transfer and verify missing-part resume;
7. cancel an active upload;
8. verify expired-session recovery;
9. verify object mismatch never reaches READY;
10. verify API 34+ UIDT and one pre-34 fallback device;
11. confirm no R2 access key/secret exists in the APK;
12. confirm the episode's current YouTube/Facebook video remains unchanged after upload READY.

A READY asset is intentionally **not attached to the episode in Phase 6**. That atomic production-video replacement belongs to Phase 7.

---

## Phase 7 — Episode Video Integration 🟡 CODE / CI PASS · PRODUCTION MIGRATION + SMOKE PENDING

Phase 7 now connects verified READY R2 assets to real episodes through an explicit, trusted attachment step. Upload READY and episode attachment remain separate states.

### Backend / web implementation

- [x] migration 010 adds `episodes.video_asset_id`;
- [x] migration 010 extends provider constraint to `youtube|facebook|r2`;
- [x] legacy YouTube/Facebook rows keep URL-backed behavior;
- [x] R2 rows require `video_url = null` and a READY asset reference;
- [x] trigger rejects non-READY or cross-episode R2 assets;
- [x] admin RPC atomically attaches a READY asset to the episode;
- [x] previous attached R2 asset is marked `REPLACED` only after the new reference succeeds;
- [x] old replaced R2 object cleanup is best-effort after the database swap;
- [x] upload completion/finalization does not attach or publish an episode;
- [x] admin-only short-lived GET preview URL for READY assets;
- [x] public R2 playback is supported for attached published episodes;
- [x] public asset RLS exposes only READY assets attached to published content;
- [x] backend preview contract CI run `36321253749` passed typecheck, lint, and tests.

### Android implementation

- [x] R2 provider and `videoAssetId` supported in episode read/write models;
- [x] YouTube/Facebook editing remains available for legacy/external rows;
- [x] remote upload READY remains distinct from attachment;
- [x] READY asset can be previewed through a short-lived admin URL;
- [x] READY asset can be explicitly attached from Episode editor;
- [x] attachment has separate `IDLE/ATTACHING/ATTACHED/FAILED` state;
- [x] failed attachment leaves the current production video untouched;
- [x] successful attachment synchronizes editor provider to R2 without clearing unrelated dirty edits;
- [x] episode detail/list render R2 provider safely;
- [x] retry/replace/cancel upload flows remain intact;
- [x] local encode/upload completion never auto-publishes;
- [x] Android preview/attachment Verify step passed in CI run `36321377376`.

### Runtime replacement flow

```text
existing episode video remains active
  -> select local replacement
  -> inspect / encode when required
  -> upload
  -> server verify
  -> remote asset READY
  -> optional admin preview
  -> explicit Attach to episode
  -> atomic DB swap to R2 asset
  -> previous R2 asset becomes REPLACED
  -> best-effort old-object cleanup
```

`UPLOAD_READY` means the remote object is verified. It does **not** mean the episode uses it.

### Publish behavior

A pending replacement does not invalidate an already-valid active video. Existing published content stays publishable while a new replacement is prepared because the old source remains authoritative until attachment succeeds.

The normal episode validator still requires the active form itself to contain a valid source. For R2, that means a valid attached asset ID; for YouTube/Facebook, that means a valid provider URL.

### Exit gate

Code/build gate: **PASS**.

Production completion remains pending:

1. apply migrations 006–010 to the real Supabase project in order;
2. configure production R2 upload credentials/policy values;
3. configure `R2_VIDEO_PUBLIC_BASE_URL` and preview TTL;
4. smoke-test unauthorized/non-admin rejection;
5. verify READY asset preview;
6. attach R2 over an existing YouTube/Facebook episode and confirm the old video stays active until attachment;
7. replace one attached R2 asset with another and verify atomic swap + safe cleanup;
8. verify a failed attachment leaves the previous video unchanged;
9. publish/view an attached R2 episode through the public web player;
10. verify unpublished/deleted content does not expose its R2 asset publicly;
11. verify Android list/detail/editor reload the attached R2 source correctly.

---

## Phase 8 — Analytics 🟡 CODE / CI PASS · PRODUCTION DATA SMOKE PENDING

Phase 8 now exposes the existing first-party Ceritaria analytics through the Android admin app without downloading raw event history.

### Implementation status

- [x] Android analytics domain/data/repository boundary;
- [x] one admin-only `get_analytics_dashboard` RPC call per report load;
- [x] timezone fixed to `Asia/Jakarta` to match the existing web admin contract;
- [x] 7 / 30 / 90 day range selector;
- [x] today pageviews and visitors;
- [x] period pageviews, visitors, sessions, and total events;
- [x] 24-hour aggregated traffic strip;
- [x] top pages with views and visitors;
- [x] device breakdown;
- [x] referrer breakdown;
- [x] player interaction events;
- [x] loading, empty, initial-error, and refresh-error states;
- [x] pull-to-refresh retains the last successful report if refresh fails;
- [x] stale range requests are cancelled before a newer range wins;
- [x] DTO/domain mapper coverage;
- [x] ViewModel state/range/refresh coverage;
- [x] no raw `analytics_events` history downloaded by Android;
- [x] final Android Verify run `36326340217` succeeded.

### Backend support boundary

The existing RPC returns:

```text
summary
hourly
topPages
devices
referrers
events
```

It does not currently return a dedicated top-series or top-episode ranking. Android therefore renders `topPages` rather than inventing an unsupported content ranking.

Realtime visitor presence also remains outside this Phase 8 Android scope; the existing web admin owns that separate Supabase Presence view.

### Error behavior

- first-load failure shows a retry action;
- pull-to-refresh failure keeps the previous report visible and shows a compact warning;
- changing range cancels the previous load job;
- infrastructure errors are mapped through the repository boundary rather than exposed as raw PostgREST text.

### Exit gate

Code/build gate: **PASS**.

Production completion remains pending a configured admin smoke test against the real Supabase project:

1. open Analytics as an authorized admin;
2. verify 7/30/90 day reports match the web admin report for the same range/timezone;
3. verify top pages/device/referrer/player events are populated when data exists;
4. verify empty range behavior where applicable;
5. verify unauthorized/non-admin access remains rejected by the RPC/RLS boundary.

---

## Phase 9 — Testing & Hardening ✅ AUTOMATED CODE / CI PASS

The final hardened Android head passed GitHub Actions run `36364998006` on **2026-09-28** after release-workflow hardening was added.

### Automated coverage and hardening completed

- [x] DTO/domain mapper, validator, provider, compatibility, Room, encoding, upload, analytics, auth/session, editor, reorder, image-media, and R2 attachment unit coverage;
- [x] release APK assembly is part of normal CI;
- [x] handwritten Kotlin line guardrails enforced in CI;
- [x] privileged server credential identifiers rejected from Android source;
- [x] potential sensitive token/password logging and JWT-like literals rejected in CI;
- [x] Room schema generation uses the official Room Gradle Plugin with reproducible variant-aware outputs;
- [x] CI actions use supported Node 24-based releases;
- [x] superseded Android workflow runs cancel through workflow concurrency;
- [x] upload recovery preserves UNAUTHENTICATED/FORBIDDEN/server failures instead of silently replacing the remote session;
- [x] WorkManager and Android 14+ UIDT paths share retry classification;
- [x] unrecoverable auth/config/source errors stop automatic retry;
- [x] transient network/R2/server/session errors remain retryable;
- [x] failed finalize and multipart transfer retain prepared local video for retry;
- [x] completed multipart parts remain persisted after a later part fails;
- [x] duplicate login, attach, preview, Series save, Episode save, and reorder actions are blocked synchronously;
- [x] cancellation semantics remain explicit across image, encode, upload recovery, Worker, and UIDT execution;
- [x] previous valid production media remains untouched until replacement/finalization succeeds;
- [x] final hardened head passed `lintDebug testDebugUnitTest assembleDebug assembleRelease` plus source/security guardrails.

### Still requires environment/device verification

- [ ] production/staging Supabase RLS authorization matrix;
- [ ] production image Storage policy smoke;
- [ ] real R2 single and multipart transfer with network interruption;
- [ ] expired real Supabase session during transfer;
- [ ] real process death / application recreation during media work;
- [ ] representative hardware codec matrix;
- [ ] accessibility/focus/touch-target manual pass on representative phones/tablets;
- [ ] final production web/PWA compatibility smoke after controlled mutations.

### Exit gate

Automated code/build hardening: **PASS**.

Environment-specific and real-device items remain explicit Phase 10 release-candidate gates and are not claimed from unit CI.

---

## Phase 10 — Release 🟡 AUTOMATED RELEASE PACKAGING READY / MANUAL CANDIDATE PENDING

Release engineering is implemented for an internal signed candidate, while production/device smoke remains intentionally manual.

### Automated release preparation

- [x] release signing configuration reads keystore/password/alias values only from Gradle properties or environment variables;
- [x] signing secrets are required by the manual `Android Release Candidate` workflow;
- [x] Supabase URL, publishable key, and API base URL are supplied at build time rather than committed;
- [x] privileged Supabase service-role and R2 credentials remain forbidden from Android source/workflows;
- [x] release workflow validates a positive integer `version_code` and non-empty `version_name`;
- [x] release Supabase/API endpoints must use HTTPS;
- [x] release workflow runs source/security guardrails, lint, unit tests, APK build, and AAB build;
- [x] APK signature is verified with `apksigner`;
- [x] AAB JAR signature is verified with `jarsigner -strict`;
- [x] SHA-256 checksums are generated for APK/AAB and uploaded with the artifacts;
- [x] APK + AAB + checksum artifact retention is 30 days;
- [x] workflow token permissions are restricted to `contents: read`;
- [x] rollback and controlled-smoke order are documented in `RELEASE-OPERATIONS.md`;
- [x] backend contract currently exposes migrations through `010_episode_r2_video.sql` and the required R2 upload/preview/attach routes;
- [x] Ceritaria backend latest audited head is CI green;
- [x] Android release-workflow hardening head passed normal Android CI run `36364998006`;
- [x] minification is intentionally disabled for the first internal candidate and therefore has no shrinker gate yet.

### Manual release-candidate gates

- [ ] configure GitHub release secrets;
- [ ] manually run `Android Release Candidate` with a new monotonically increasing versionCode;
- [ ] install the signed APK on representative Android hardware;
- [ ] verify login/session/admin authorization against the real environment;
- [ ] verify read-only Series/Episode/Analytics;
- [ ] perform one controlled draft mutation;
- [ ] perform controlled image upload/replace/remove;
- [ ] inspect/encode representative local video on hardware;
- [ ] verify real R2 single and multipart upload, retry, interruption, preview, and finalize;
- [ ] attach a READY R2 asset only after preview succeeds;
- [ ] verify the same controlled content in the existing Ceritaria web/PWA;
- [ ] verify unauthorized/non-admin operations remain rejected;
- [ ] complete accessibility/focus/touch-target manual pass;
- [ ] retain the previous known-good source/artifact before broader distribution.

### Exit gate

Automated release engineering: **PASS**.

Production/internal candidate: **PENDING MANUAL SIGNED WORKFLOW + ENVIRONMENT/DEVICE SMOKE**.

The app must not be described as production-verified until the signed candidate completes the release checklist against the real environment.

---

## 9. Backend/API Contract Required for Video

Exact endpoint names may be adapted to the existing Ceritaria server conventions after Phase 0, but responsibilities must remain explicit.

Conceptual contract:

```text
POST /video-uploads
  create upload session

POST /video-uploads/{id}/parts
  create/authorize multipart part operation where required

POST /video-uploads/{id}/complete
  complete transfer

POST /video-uploads/{id}/finalize
  verify object and mark asset READY

POST /video-uploads/{id}/cancel
  abort incomplete upload / cleanup
```

The final API must use the existing project's naming/auth/error conventions rather than introducing a second style solely for Android.

---

## 10. Local Video Job Contract

Room persists operational state similar to:

```text
VideoJob
- jobId
- episodeId?
- sourceUri
- sourceFingerprint
- sourceMetadata
- needsEncoding
- encodedLocalUri?
- encodingStatus
- encodingProgress
- uploadStatus
- uploadedBytes
- totalBytes
- uploadSessionId?
- multipartState?
- remoteAssetId?
- lastErrorCode?
- createdAt
- updatedAt
```

Encoding and upload states remain separate.

Encoding state:

```text
NOT_REQUIRED
QUEUED
ENCODING
READY
FAILED
CANCELLED
```

Upload state:

```text
NOT_STARTED
QUEUED
UPLOADING
VERIFYING
READY
FAILED
CANCELLED
```

`encodingStatus = READY` does not imply upload success.  
`uploadStatus = READY` does not imply the episode is published.

---

## 11. Implementation Order Inside Each Phase

For every feature slice:

1. inspect existing contract/files;
2. state files expected to change;
3. define domain/state contract;
4. implement UI with controlled/mock state where practical;
5. implement repository/data-source boundary;
6. connect real data;
7. handle loading/empty/error/cancel states;
8. add relevant tests;
9. run format/lint;
10. compile;
11. run relevant tests;
12. inspect changed-file line counts;
13. verify no unrelated stable code was modified;
14. report assumptions or unverified backend behavior.

Do not quietly refactor unrelated stable areas while implementing a feature.

---

## 12. Definition of Done

A feature is complete only when all relevant conditions are satisfied:

- loading state exists;
- populated state exists;
- empty state exists where applicable;
- error state exists;
- validation exists;
- success/failure feedback exists;
- duplicate mutation is prevented where necessary;
- navigation/cancellation is safe;
- process recreation is considered for durable work;
- relevant tests pass;
- accessibility is reasonable;
- no privileged secret is embedded;
- production schema compatibility is verified;
- file-size and responsibility rules pass;
- existing valid media is not destroyed by failed replacement;
- web/PWA compatibility is verified for production mutations.

---

## 13. Documentation Consistency Status

The specification set has been reconciled to this implementation baseline.

### `ARCHITECTURE.md`

Aligned:

- direct R2 video is an active core-delivery architecture;
- Room is the required operational video-job store;
- encoding execution and upload execution are separate;
- multipart/resume state has explicit ownership;
- URI persistence and process-recovery ownership are documented.

### `RULES.md`

Aligned:

- background execution is job-type specific rather than one generic Worker rule;
- handwritten Kotlin target is `<= 200` lines with hard limit `<= 300`;
- Media3 encoder/controller/background adapters use tighter limits;
- Room owns durable `VideoJob` state;
- multipart uploader responsibilities are separated from encoding and episode mutation.

### `SCHEMA.md`

Aligned:

- direct-upload video is part of the core delivery baseline;
- R2 asset representation remains migration-gated until Phase 0 verifies production;
- local `VideoJob` includes upload-session and multipart/recovery state;
- trusted server upload-session responsibilities are documented;
- transient Android operational state remains outside production `episodes`.

### `PRD.md`

Aligned:

- FR-11 video encoding and FR-12 R2 upload are explicit implementation-baseline capabilities;
- direct binary upload remains gated by the secure server-mediated contract;
- Room-backed recovery and job-type-specific background execution are required.

### `DESIGN.md`

Aligned:

- Encoding, Uploading, Verifying, and READY are visually distinct;
- upload completion does not imply verification;
- existing production video remains active during replacement;
- retry/resume behavior preserves valid prepared output where possible.

These documents now form one specification set and should be changed together when a future architectural decision affects more than one contract.

---

## 14. Post–Phase 0 Open Decisions

Phase 0 resolved the production schema, publish model, RLS/admin model, image storage convention, analytics contract, and current media-provider constraints.

The remaining decisions belong to later implementation phases:

1. final R2 video delivery/player URL strategy;
2. final `video_assets` migration design;
3. exact R2 server endpoint names and response envelope;
4. maximum accepted direct-upload video size;
5. multipart threshold and part-size policy;
6. production application and smoke verification for reorder migration `005_episode_reorder_rpc.sql`;
7. whether a restore workflow should be added later;
8. staging/test backend availability;
9. minimum/target Android SDK and device support matrix.

These are not blockers for Phase 1 foundation or Phase 2 read-only catalog. They must not be guessed when their owning phase begins.

---

## 15. Final Build Sequence

```text
0. Production Contract Audit ✅
        ↓
1. Android Foundation
        ↓
2. Read-only Catalog
        ↓
3. Content Mutations
        ↓
4. Image Media
        ↓
5. Local Video Pipeline
        ↓
6. Secure R2 Upload Contract
        ↓
7. Episode Video Integration
        ↓
8. Analytics
        ↓
9. Testing & Hardening
        ↓
10. Release
```

This sequence is the implementation baseline. A later phase may depend on verified outputs from an earlier phase, but unrelated phases should not be collapsed into one giant agent task.
