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

## Phase 6 — Secure R2 Upload Contract

### Scope

Implement the trusted server boundary required by direct binary upload.

### Server responsibilities

- authenticate Supabase user/session;
- verify admin authorization;
- generate canonical `assetId` and R2 object key;
- validate allowed MIME/type/size metadata;
- return short-lived, scoped upload authorization;
- initiate multipart upload when required;
- finalize multipart upload when required;
- verify uploaded object server-side;
- persist/transition media asset status;
- expose safe finalize/cancel endpoints;
- reject expired/invalid sessions;
- never return permanent R2 credentials.

### Android responsibilities

- request upload session;
- transfer prepared file;
- persist resume metadata locally when required;
- report bytes uploaded / total bytes;
- retry recoverable part failures;
- cancel explicitly;
- call server finalization;
- transition local state to VERIFYING/READY/FAILED;
- never perform privileged signing locally.

### Upload strategy

Support two paths where the verified server contract allows it:

```text
small object
  -> single authorized PUT

large video
  -> multipart upload
  -> resumable/retryable parts
  -> complete
  -> server verification
```

The exact size threshold is a server/config decision rather than a magic constant spread through Android UI code.

### Exit gate

A prepared local video can be uploaded, resumed/retried as supported, verified by the server, and represented as a READY media asset without embedding permanent R2 credentials in Android.

---

## Phase 7 — Episode Video Integration

### Scope

Connect READY uploaded assets to real episode editing/publishing.

### Tasks

- preserve existing YouTube/Facebook editing;
- support R2 provider only after schema/server contract is verified;
- show source metadata and output metadata;
- preview READY replacement;
- attach READY asset to episode;
- replace video atomically;
- keep old production video active until replacement finalization succeeds;
- clean orphaned/replaced objects safely;
- disable publish when a required selected local video is not READY;
- ensure upload completion alone does not auto-publish an episode;
- expose Retry/Replace/Cancel states clearly.

### Exit gate

An episode can use a verified R2 video asset while maintaining its existing episode identity and preserving a previously valid video if replacement fails.

---

## Phase 8 — Analytics

### Scope

Read-only operational analytics.

### Tasks

- views/pageviews;
- unique visitors where supported;
- player events;
- top series/episodes/pages where existing backend supports them;
- time range selector;
- loading/empty/error states;
- use safe views/RPCs instead of downloading raw event history where possible.

### Exit gate

Admin can inspect supported Ceritaria metrics without Android receiving unnecessary raw analytics data.

---

## Phase 9 — Testing & Hardening

### Unit tests

- DTO/domain mapping;
- validators;
- provider parsing;
- streaming preset calculation;
- compatibility checker;
- ViewModel state transitions;
- Room video-job mapping/state transitions;
- upload state machine;
- replacement/publish guards.

### Integration tests

- Supabase mappings against a safe test/staging environment when available;
- RLS authorization cases;
- storage conventions;
- R2 upload-session contract;
- upload finalization;
- Room recovery.

### UI tests

Critical flows:

- login/session restore;
- series navigation;
- create/edit draft;
- publish confirmation;
- dirty-form exit;
- image upload states;
- local video selection;
- encode progress/failure/cancel;
- upload retry;
- replacement confirmation.

### Failure scenarios

Test explicitly:

- no network;
- network loss during transfer;
- expired auth session;
- unauthorized user;
- process death;
- application recreation;
- source URI unavailable;
- insufficient device storage;
- unsupported codec;
- encoder cancellation;
- multipart part failure;
- server finalization failure;
- upload succeeds but verification fails;
- previous production video remains valid after failed replacement.

### Quality checks

- no privileged secret in APK;
- no raw token logging;
- no file above hard limit unless generated;
- no God ViewModel/repository;
- no generic media manager combining unrelated responsibilities;
- no production schema field invented by Android;
- accessibility labels/focus/touch targets reviewed;
- loading/content/empty/error states present;
- release build compiles.

### Exit gate

All critical user flows and media failure paths are deterministic, recoverable where expected, and verified by tests appropriate to the layer.

---

## Phase 10 — Release

### Tasks

- release signing configuration;
- minification/shrinker verification where enabled;
- final environment review;
- final schema compatibility check;
- final RLS/security review;
- APK/AAB internal distribution;
- production smoke test with non-destructive operations first;
- controlled mutation test;
- controlled media upload test;
- rollback procedure documented.

### Exit gate

Internal production candidate can manage Ceritaria content without breaking existing web/PWA behavior and without privileged credentials being recoverable from the Android package.

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
