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

## Phase 0 — Production Contract Audit

### Scope

Audit the real Ceritaria production contract before implementing persistence-heavy features.

### Tasks

- inspect current `supabase/migrations/*` in order;
- record exact `series` schema;
- record exact `episodes` schema;
- inspect `admin_users` authorization;
- inspect RLS policies;
- inspect storage buckets/policies;
- inspect existing CMS create/update/delete/publish actions;
- inspect TypeScript database types;
- inspect analytics tables/views/RPCs;
- verify current YouTube/Facebook provider representation;
- determine whether R2 assets require a production migration;
- reconcile all discoveries into `SCHEMA.md`.

### Deliverables

- verified Android-facing data contract;
- exact field names and types;
- confirmed authorization rules;
- list of required migrations, if any;
- list of server endpoints required for R2.

### Exit gate

No Android repository implementation may invent an unverified production field.

---

## Phase 1 — Android Foundation

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

### Exit gate

An authorized admin can launch the app, restore/sign in to a valid session, and reach an authenticated shell. Unauthorized accounts are blocked by real backend policy, not merely hidden UI.

---

## Phase 2 — Read-only Catalog

### Scope

Prove data compatibility before enabling mutations.

### Tasks

- series list;
- pagination;
- search/filter;
- series detail;
- episode list per series;
- episode detail;
- provider/status labels;
- draft/published/deleted indicators;
- pull-to-refresh;
- loading/content/empty/error states;
- safe handling for unknown future provider values.

### Exit gate

Studio can inspect the real production catalog without mutations and without requiring every record to be loaded into memory.

---

## Phase 3 — Content Mutations

### Scope

Implement production-compatible editing and publishing.

### Tasks

- create/edit series;
- create/edit episode;
- draft save;
- publish;
- unpublish where supported;
- featured toggle;
- soft delete;
- restore where production supports it;
- episode reorder;
- validation matching existing web behavior;
- duplicate-save protection;
- dirty-form protection;
- success/failure feedback.

### Exit gate

Changes made from Android appear correctly in Ceritaria web/PWA and existing CMS without manual synchronization.

---

## Phase 4 — Image Media

### Scope

Complete the current image workflow before the heavier video pipeline.

### Tasks

- system Photo Picker;
- MIME/size validation;
- image preparation/compression where appropriate;
- cover upload;
- hero upload;
- thumbnail upload;
- progress/cancel/retry;
- replace/remove media;
- preview;
- atomic DB-reference update;
- safe old-object cleanup.

### Exit gate

Image operations survive normal navigation/backgrounding and never leave a published record pointing to a failed/missing replacement object.

---

## Phase 5 — Local Video Pipeline

### Scope

Build video inspection and encoding without depending on a finished R2 production contract.

### Tasks

- local video selection;
- persist required URI permission/access;
- inspect duration, dimensions, rotation, FPS, codec, audio, and file size;
- create `VideoCompatibilityChecker`;
- define verified streaming preset;
- skip re-encode for compatible source;
- implement Media3 Transformer encoder;
- preserve aspect ratio and rotation;
- cap output according to verified streaming requirements;
- expose progress;
- support cancellation;
- detect unsupported codec;
- detect insufficient local storage where estimable;
- create temporary-media retention policy;
- persist `VideoJob` in Room;
- restore job UI after process recreation;
- reuse valid encoded output after recoverable upload failure.

### Exit gate

A selected local video can reliably reach either:

```text
READY_WITHOUT_ENCODING
```

or:

```text
ENCODED_READY
```

and that state survives normal navigation/process recreation where supported by the execution contract.

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

## 14. Remaining Risks / Decisions to Resolve in Phase 0

These are intentionally not guessed in this plan:

1. exact production `series` and `episodes` field names/types;
2. exact current publish-state representation;
3. exact RLS/admin authorization model;
4. existing storage bucket/path conventions;
5. whether production already has a generic media-assets table;
6. whether an R2-specific migration is required;
7. final video delivery/player URL strategy;
8. exact server endpoint naming and response envelope;
9. exact maximum accepted upload size;
10. exact multipart threshold/part-size policy;
11. staging/test backend availability;
12. minimum/target Android SDK after inspecting the intended device support matrix.

None of these should be invented by the Android agent merely to keep coding moving.

---

## 15. Final Build Sequence

```text
0. Production Contract Audit
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
