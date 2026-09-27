# Ceritaria Studio Android --- Architecture

## 1. Architectural principles

1.  Existing Ceritaria production data is the source of truth.
2.  UI never talks directly to Supabase SDK classes.
3.  Domain/UI models are separated from transport/database DTOs.
4.  Every feature owns its UI state and behavior.
5.  Cross-feature code belongs in `core` only when genuinely shared.
6.  Security is enforced server-side with Supabase Auth/RLS or dedicated
    endpoints.
7.  Background work is durable when the operation must survive process
    loss.
8.  Keep the architecture boring. Boring systems are wonderfully
    difficult to accidentally destroy.

## 2. High-level topology

``` text
Ceritaria Studio Android
        |
        | Supabase user session
        v
Repository interfaces
        |
        +------------------+------------------+
        |                  |                  |
Supabase adapters     Ceritaria server API   Local job store
        |                  |                  |
        v                  v                  v
Auth / Postgres /     scoped R2 upload       Room VideoJob /
Storage / RPC         session + verify       multipart state
        |                  |
        +---------+--------+
                  v
Existing Ceritaria production data
        |
        +--> Ceritaria Web/PWA
        +--> Existing Web CMS
```

## 3. Layers

### Presentation

Compose screens, components, ViewModels, immutable UI state, UI events.

Responsibilities: - render; - collect state; - send intents; -
navigation requests; - local presentation formatting.

Must not: - call Supabase directly; - contain SQL; - perform storage
upload; - own business rules duplicated from repositories/use cases.

### Domain

Models, repository contracts and use cases when behavior is non-trivial.

Examples: - `PublishEpisodeUseCase` - `ReorderEpisodesUseCase` -
`ReplaceArtworkUseCase`

A one-line repository call does not automatically deserve a ceremonial
UseCase class.

### Data

Repository implementations, Supabase data sources, DTOs, mappers.

Responsibilities: - query/mutation; - pagination; - error translation; -
transport mapping; - storage paths; - transaction/RPC calls.

### Background execution

Background execution is job-type specific rather than one generic Worker architecture.

- Image transfer and short durable work may use WorkManager.
- Video encoding has its own execution adapter around Media3 and must expose cancellation/progress without UI ownership.
- Transformer lifecycle/control calls remain on one Media3 application looper; codec work stays inside Media3 rather than custom blocking UI-thread work.
- Large user-initiated video transfer uses the execution mechanism appropriate to the supported Android version and verified transfer contract.
- Execution adapters call domain/data abstractions and never contain Compose/UI logic.
- Durable media state is persisted in Room so execution can be reconstructed after process recreation.

## 4. State model

Each screen exposes one immutable state object.

``` text
SeriesListUiState
- isLoading
- items
- query
- filter
- nextPage
- refreshing
- error
```

One-off UI effects such as navigation or snackbar may use a dedicated
effect stream. Persistent facts belong in state, not events.

## 5. Data flow

``` text
User action
  -> ViewModel intent
  -> use case/repository
  -> data source
  -> Supabase/API
  -> mapped domain result
  -> ViewModel state
  -> Compose
```

No circular dependencies.

## 6. Authentication

-   Supabase user session is stored using the supported client
    mechanism.
-   App verifies admin authorization after authentication.
-   `admin_users`/current production authorization contract determines
    access.
-   UI authorization is convenience only; RLS is authoritative.
-   Service-role key is forbidden in APK/build config/resources.
-   Token logging is forbidden.

## 7. Data compatibility

Android must map to verified production columns. `SCHEMA.md`
distinguishes: - VERIFIED: confirmed from production migrations/docs; -
CONCEPTUAL: required by product but exact column not yet verified; -
PROPOSED: future migration.

When a mismatch exists, update schema documentation before changing
Android code.

## 8. Pagination

Use key/range pagination supported by the backend. Repository APIs
expose page size/cursor rather than making screens know PostgREST
details.

Lists must not request every episode in production.

## 9. Media upload architecture

### Existing images

``` text
Photo Picker
 -> validation
 -> optional image preparation
 -> WorkManager
 -> authorized Supabase Storage upload
 -> repository metadata update
 -> success
```

Replacement should prefer: 1. upload new object; 2. verify success; 3.
update DB reference; 4. clean old object when safe.

This avoids broken records if upload fails.

### Direct R2 video — core delivery

``` text
Photo Picker / document media selection
 -> persist required URI access
 -> inspect source
 -> compatibility check
 -> encode only when required
 -> persist VideoJob in Room
 -> request upload session
 -> backend validates admin + metadata
 -> backend returns short-lived scoped upload authorization
 -> Android performs single PUT or multipart transfer
 -> local Room state records progress/resume metadata
 -> backend completes/verifies object
 -> media asset becomes READY
 -> episode reference changes atomically
 -> old asset is cleaned only when safe
```

Encoding and transfer are separate jobs and separate state machines.

Room stores only operational video-job state; it does not duplicate the production series/episode catalog.

Multipart upload state has a dedicated owner and is persisted when resume/recovery requires it.

Never ship R2 account secret/access key in Android.


## 10. Process recovery and URI ownership

- Persist source URI permission/access when long-running work may outlive the selecting screen.
- `VideoJobRepository` is the source of truth for operational encode/upload state.
- ViewModels observe durable state; they do not own the only copy of progress/retry metadata.
- Encoded output is retained after recoverable upload failure so retry does not force re-encoding.
- READY encoded output has no blind age-only eviction; deletion belongs to explicit replace/discard/finalization lifecycle ownership.
- Partial output is removed on failed, cancelled, or interrupted preparation recovery.
- Temporary media cleanup never deletes the active production asset.
- Process recreation must reconstruct UI state from persisted job state rather than assuming the old in-memory worker/controller still exists.

## 11. Error taxonomy

Create app-level errors: - `AuthError` - `AuthorizationError` -
`NetworkError` - `ValidationError` - `ConflictError` - `NotFoundError` -
`StorageError` - `UnknownError`

Screens should not parse arbitrary exception strings.

## 12. Concurrency

-   A record save has one active mutation at a time.
-   ViewModel cancels stale searches.
-   Reordering is serialized.
-   Media upload gets a stable operation ID.
-   Avoid optimistic updates for publish/delete unless rollback is
    explicit.
-   Refresh after critical mutation when backend triggers may modify
    data.

## 13. Dependency injection

Hilt scopes: - singleton: Supabase client, repositories, Room database, dispatchers; -
ViewModel: feature state; - background execution adapters: injected dependencies.

Do not create SDK clients inside Composables/ViewModels.

## 14. Testing

### Unit

-   ViewModels;
-   mappers;
-   validation;
-   use cases;
-   repository behavior with fakes.

### Integration

-   Supabase adapter against safe test/staging project where available;
-   schema mapping;
-   RLS authorization cases.

### UI

Critical flows: - login; - series navigation; - create/edit draft; -
publish confirmation; - media progress/error; - dirty-form exit.

## 15. Build configuration

At minimum: - debug; - release.

Prefer separate staging/production endpoints before destructive
integration testing.

Secrets: - public Supabase URL/publishable key may be configured as
intended by Supabase client model; - privileged server secrets are never
packaged.

## 16. Package ownership

``` text
feature/series/list      only series-list behavior
feature/series/detail    only series-detail behavior
feature/series/editor    only series editing
feature/episode/editor   only episode editing
core/designsystem        reusable visual primitives/tokens
core/data                shared data infrastructure
core/model               truly shared domain models
```

A feature may depend on core. Core must not depend on features. Sibling
features do not reach into each other's implementation packages.
