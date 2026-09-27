# Ceritaria Studio Android --- Engineering Rules

These rules are mandatory for coding agents and human contributors.

## 1. Prime directive

A file has one clear responsibility.

Do not create monster files. "It still compiles" is not an architectural
argument.

## 2. File size limits

### Kotlin

-   Target: \<= 200 lines per handwritten `.kt` file.
-   Soft warning threshold: \> 250 lines.
-   Hard limit: 300 lines.
-   Any handwritten file \> 300 lines must be split by responsibility before the task is considered complete.
-   Generated files are exempt.
-   Do not split into arbitrary `Part1`/`Part2` files merely to satisfy the number.
-   A temporary migration exception requires explicit approval and a documented follow-up split task.

### Compose functions

-   Target screen composable: \<= 120 lines.
-   Reusable composable: \<= 80 lines.
-   Function target: \<= 50 lines.
-   Function hard limit: 80 lines unless it is declarative data with a
    clear reason.

### Classes

-   One primary public class/interface per file.
-   Small tightly coupled private helpers may remain in the same file.
-   Do not place multiple ViewModels, repositories or screens in one
    file.

## 3. Split by responsibility, not arbitrary chunks

Bad:

``` text
EpisodeScreenPart1.kt
EpisodeScreenPart2.kt
Utils2.kt
Helpers.kt
```

Good:

``` text
EpisodeEditorScreen.kt
EpisodeEditorViewModel.kt
EpisodeEditorUiState.kt
EpisodeEditorActions.kt
EpisodeIdentitySection.kt
EpisodeStorySection.kt
EpisodeVideoSection.kt
EpisodeArtworkSection.kt
EpisodePublishingSection.kt
EpisodeValidator.kt
```

Never split solely to satisfy line count while preserving a tangled
responsibility.

## 4. Screen rules

A screen file may: - arrange feature components; - collect ViewModel
state; - forward callbacks.

A screen file must not: - call Supabase; - contain DTO mapping; -
contain validation engines; - upload files; - build SQL/query strings; -
implement repository logic.

## 5. ViewModel rules

ViewModel may: - own UI state; - respond to intents; - invoke use
cases/repositories; - coordinate screen-level async work.

ViewModel must not: - render UI; - instantiate SDK clients; - know
storage bucket URL construction details; - contain giant mapping
functions; - exceed 300 lines without decomposition.

If a ViewModel becomes large, extract: - validator; - use case; -
mapper; - form reducer/state holder where justified.

## 6. Repository rules

Repository interface describes domain operations.

Implementation: - coordinates data sources; - maps errors; - returns
domain models/results.

Do not make a single `CeritariaRepository` containing every operation in
the product.

Prefer: - `SeriesRepository` - `EpisodeRepository` - `MediaRepository` -
`AnalyticsRepository` - `AuthRepository`

## 7. Data source rules

Supabase-specific query syntax belongs in data-source/adapter files.

DTOs must not leak into Compose UI.

Database row -> DTO -> mapper -> domain model.

## 8. Model rules

Separate when needed: - database/transport DTO; - domain model; - editor
form/UI state.

Do not reuse a mutable editor form object as the production domain
entity merely to save typing.

## 9. Component rules

Extract a composable when: - reused; - visually/semantically
independent; - has its own interaction behavior; - makes parent screen
materially easier to understand.

Do not extract every `Text()` into its own file. Architecture can also
die from excessive ceremony.

## 10. Naming

Names describe responsibility: - `SeriesListScreen` -
`SeriesListViewModel` - `SeriesListUiState` - `SeriesRepository` -
`SupabaseSeriesDataSource` - `SeriesRowDto` - `SeriesMapper`

Forbidden vague dumping grounds: - `Utils.kt` - `Common.kt` -
`Helpers.kt` - `Manager.kt` - `Stuff.kt` unless the name is narrowed to
an actual domain responsibility, e.g. `SlugNormalizer.kt`.

## 11. Package boundaries

-   `feature/*` can depend on `core/*`.
-   `core/*` cannot depend on `feature/*`.
-   Feature A must not import Feature B implementation.
-   Shared UI moves to design system only after real reuse or clear
    cross-feature role.

## 12. State

-   UI state is immutable.
-   Prefer `StateFlow`.
-   One source of truth per screen.
-   Do not maintain the same publish state in three independent
    booleans.
-   Derived UI values should be derived, not separately synchronized.

## 13. Coroutines

-   No `GlobalScope`.
-   Use structured concurrency.
-   Inject dispatchers when testability matters.
-   Cancel stale search/query work.
-   Never swallow `CancellationException`.
-   Prevent duplicate concurrent save operations.

## 14. Error handling

-   No empty `catch`.
-   No raw exception message as user-facing copy.
-   Map infrastructure failures to app error types.
-   Log enough context without secrets/tokens/PII.
-   Retry only operations known to be safe.

## 15. Security

Forbidden: - Supabase service-role key in app; - R2 secret/access key in
app; - hardcoded admin password; - bypassing RLS because UI "only shows
it to admins"; - logging access/refresh tokens; - committing
`.env`/keystore credentials.

Admin authorization must be enforced by backend policy.

## 16. Database changes

-   Never change production schema casually from Android work.
-   Every schema change requires a migration.
-   Migration is additive/backward-compatible where practical.
-   Update `SCHEMA.md`.
-   Verify web CMS/public web compatibility.
-   Do not rename/drop a production field in the same release that
    introduces the Android dependency.

## 17. Media

-   Use system photo picker where possible.
-   Validate MIME/size.
-   Do not hold giant media files fully in memory.
-   Image transfer and short durable work may use WorkManager.
-   Video encoding and large user-initiated transfer use the execution mechanism appropriate to the job type and supported Android version; one generic Worker is not the default architecture.
-   Encoding execution and upload execution must remain separate.
-   Persist required source URI access before long-running media work.
-   Durable `VideoJob` state belongs in Room, not only in transient ViewModel/Worker memory.
-   Multipart/resume metadata must have a dedicated persistence owner when the server contract requires multipart upload.
-   New media is verified before old reference is removed.
-   Failed upload must not leave a published record pointing to a
    missing object.

## 18. Compose

-   No business logic inside composables.
-   Hoist state.
-   Pass minimal data/callbacks.
-   Stable keys for dynamic lists.
-   Avoid unnecessary recomposition.
-   Preview reusable components when useful.
-   User-visible strings go to resources.

## 19. Tests

Required for meaningful new logic: - validation; - mapping; - ViewModel
state transitions; - repository behavior; - destructive/publish
workflows when changed.

Bug fix rule: add a regression test when reasonably testable.

## 20. Agent task discipline

Before coding, agent must: 1. read `PLAN.md`, `PRD.md`,
`ARCHITECTURE.md`, `DESIGN.md`, `RULES.md`, `SCHEMA.md`; 2. inspect
existing files related to the task; 3. state files expected to change;
4. prefer modifying existing responsibility owner over duplicating
logic.

During coding: - implement one vertical slice at a time; - keep builds
green; - do not refactor unrelated areas; - do not invent backend
fields; - do not replace architecture without explicit approval.

After coding: 1. run format/lint; 2. compile; 3. run relevant tests; 4.
inspect changed file line counts; 5. split any file over hard limit; 6.
report schema/API assumptions.

## 21. Definition of architectural failure

A change is not acceptable if it: - creates a 1,000+ line handwritten
source file; - creates a God ViewModel/repository; - puts
network/database code in UI; - duplicates production source of truth; -
embeds privileged secrets; - bypasses existing RLS contract; - silently
changes publish semantics; - adds an abstraction whose only purpose is
to move the same tangled code elsewhere.

## 22. Video pipeline file rules

The encode/upload subsystem has stricter responsibility boundaries.

Forbidden:

``` text
VideoManager.kt
VideoUtils.kt
MediaHelper.kt
UploadAndEncodeService.kt
```

when those files combine inspection, encoding, upload, database mutation
and UI state.

Expected separation:

``` text
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
  VideoTransferExecutor.kt

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

Additional limits:
- `Media3VideoEncoder.kt` target \<= 180 lines, hard \<= 250.
- Background execution adapter/worker/service target \<= 180 lines, hard \<= 250.
- `EpisodeVideoController.kt` target \<= 180 lines, hard \<= 250.
- Codec/preset calculation belongs in pure classes, not an execution adapter.
- R2 request signing belongs on the trusted server, never Android.
- Low-level byte transfer never mutates episode publish state.
- Database/media finalization is owned by the trusted server/repository contract, not the byte-transfer class.
- Remote asset `READY` never implies episode attachment. Only the dedicated trusted attachment contract may switch an episode to R2.
- Preview URLs are short-lived operation data and must not be persisted as episode/catalog state.
- Temporary file cleanup has its own owner.
- Encoding progress state and upload progress state are different types.
- Room is required for durable `VideoJob` and multipart/recovery metadata.
- Do not introduce `UploadAndEncodeService`, `VideoManager`, or equivalent cross-responsibility God objects.

## 23. Video processing rules

-   Primary transformation API: AndroidX Media3 Transformer.
-   Prefer hardware MediaCodec paths supported by the device.
-   Do not bundle FFmpeg as the default encoder without explicit
    architectural approval.
-   Never perform blocking/manual codec or file-transcoding work on the main thread.
-   Media3 Transformer lifecycle/control calls must stay on one application looper as required by Media3; heavy codec work remains Media3-owned.
-   Never force-stretch source video to 1080×1920.
-   Respect source rotation/aspect ratio.
-   Skip lossy re-encode when compatibility checker approves the source.
-   A retryable upload failure must reuse the encoded file.
-   Cancellation must release codec/transformer resources.
-   Check available storage where practical before encoding.
-   Temporary-media cleanup must be lifecycle-owned. READY encoded output is retained for upload/retry and must not be deleted by blind age-only cleanup.
-   Do not delete the previous production video until replacement is
    READY and committed.
-   Do not mark an episode Published merely because upload completed;
    publishing remains an explicit content action unless the user
    selected Publish in the initiating workflow.
