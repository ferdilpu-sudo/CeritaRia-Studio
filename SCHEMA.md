# Ceritaria Studio Android — Verified Data Contract

Status: **Phase 0 production audit + repository-verified R2 extension through migration 010**  
Audit date: **2026-09-27**  
Source repository: `ferdilpu-sudo/ceritaria` branch `main`  
Phase 0 audited tree SHA: `c7ea336224a5baaf37af3645ff375981051a8b31`  
R2 extension: migrations `006` through `010` committed; production application/smoke still pending.

This document preserves the Phase 0 production audit and also records the R2 extension now committed in the Ceritaria repository. Items marked **VERIFIED-MIGRATION** are implemented in migrations/server code but must not be assumed live in production until those migrations and environment values are applied and smoke-tested.

Do not invent fields outside the verified base contract or the committed migration contract.

---

## 1. Contract status legend

- **VERIFIED** — present in audited production migrations/code.
- **VERIFIED-BEHAVIOR** — behavior confirmed from current CMS/public code.
- **VERIFIED-MIGRATION** — committed migration/server contract; live production application still requires verification.
- **PROPOSED** — required by the Studio roadmap but not present in production yet.
- **UNSUPPORTED** — not implemented by the current production contract.

---

## 2. Entity map

```text
auth.users
    |
    | 1 : 0..1
    v
admin_users

series
    |
    | 1 : N
    v
episodes

series --------> series-media bucket
episodes ------> episode-media bucket

analytics_events
    ^
    |
track_analytics_event RPC

admin dashboard
    |
    v
get_analytics_dashboard RPC
```

At the Phase 0 audit there was no `video_assets` table. Migrations 006–010 now define the repository-verified R2 extension shown in section 15.

---

## 3. Admin authorization

Status: **VERIFIED**

### auth.users

Supabase Auth owns identity/session.

Android authenticates as a normal user. A service-role credential is never needed by the APK.

### public.admin_users

Exact schema:

```sql
user_id    uuid primary key references auth.users(id) on delete cascade
created_at timestamptz not null default now()
```

There is currently no admin role/status column.

### is_admin()

Production function:

```text
public.is_admin()
-> true when auth.uid() exists in public.admin_users
```

The function is executable by authenticated users.

### Android authorization flow

```text
Supabase sign-in
 -> valid user session
 -> query own admin_users membership
 -> RLS remains authoritative
 -> enter Studio only when membership exists
```

UI hiding is not authorization.

---

## 4. Series table

Status: **VERIFIED**

Table: `public.series`

| Field | Production type | Null | Notes |
|---|---|---:|---|
| id | uuid | no | PK, default `gen_random_uuid()` |
| slug | varchar(160) | no | unique |
| title | varchar(200) | no | |
| short_synopsis | varchar(320) | yes | |
| synopsis | text | yes | |
| genres | text[] | no | default empty array |
| cover_url | text | yes | currently stores public URL |
| hero_url | text | yes | currently stores public URL |
| is_featured | boolean | no | default false |
| is_published | boolean | no | default false |
| published_at | timestamptz | yes | required when published |
| seo_title | varchar(200) | yes | |
| seo_description | varchar(320) | yes | |
| created_at | timestamptz | no | default now |
| updated_at | timestamptz | no | trigger-maintained |
| deleted_at | timestamptz | yes | soft delete |

Constraint:

```text
is_published = false OR published_at IS NOT NULL
```

Indexes:

- public feed: `published_at desc` where published and not deleted;
- featured feed: `(is_featured, published_at desc)` where published and not deleted.

### Series validation

Status: **VERIFIED-BEHAVIOR**

Current CMS validation:

- slug: lowercase kebab-case, length 2..160;
- title: length 2..200;
- short synopsis: max 320;
- synopsis: max 8000;
- genre form input: max 400 chars, converted to max 12 items;
- SEO title: max 200;
- SEO description: max 320.

---

## 5. Episodes table

Status: **VERIFIED**

Table: `public.episodes`

| Field | Production type | Null | Notes |
|---|---|---:|---|
| id | uuid | no | PK, default `gen_random_uuid()` |
| series_id | uuid | no | FK -> series(id), cascade |
| episode_number | integer | no | > 0 |
| slug | varchar(160) | no | unique within series |
| title | varchar(200) | no | |
| short_synopsis | varchar(320) | yes | |
| recap | text | yes | |
| highlights | text[] | no | default empty array |
| video_provider | varchar(30) | no | default `youtube` |
| video_url | text | no | |
| thumbnail_url | text | yes | currently stores public URL |
| duration_seconds | integer | yes | > 0 when present |
| is_published | boolean | no | default false |
| published_at | timestamptz | yes | required when published |
| seo_title | varchar(200) | yes | |
| seo_description | varchar(320) | yes | |
| created_at | timestamptz | no | default now |
| updated_at | timestamptz | no | trigger-maintained |
| deleted_at | timestamptz | yes | soft delete |

Unique constraints:

```text
(series_id, episode_number)
(series_id, slug)
```

Provider constraint after migration 002:

```text
video_provider IN ('youtube', 'facebook')
```

That constraint describes the Phase 0/pre-migration schema. Migration 010 extends the target schema to `youtube|facebook|r2` and adds `episodes.video_asset_id`; production must be migrated before Android uses the R2 path.

### Episode validation

Status: **VERIFIED-BEHAVIOR**

Current CMS validation:

- series id: UUID;
- episode number: positive integer, max 10000;
- slug: lowercase kebab-case, length 2..160;
- title: length 2..200;
- short synopsis: max 320;
- recap: max 20000;
- highlights form: max 4000 chars, converted to max 12 non-empty lines;
- pre-migration provider: `youtube` or `facebook`;
- post-migration provider: `youtube`, `facebook`, or `r2`;
- YouTube/Facebook video URL: required, max 2048, provider-specific validation;
- R2 source: URL is null and `video_asset_id` must reference a READY asset belonging to the episode;
- duration: positive integer, max 86400 seconds;
- SEO title: max 200;
- SEO description: max 320.

---

## 6. Android domain models

These models map directly to verified production fields.

```kotlin
data class Series(
    val id: String,
    val slug: String,
    val title: String,
    val shortSynopsis: String?,
    val synopsis: String?,
    val genres: List<String>,
    val coverUrl: String?,
    val heroUrl: String?,
    val isFeatured: Boolean,
    val publishStatus: PublishStatus,
    val publishedAt: Instant?,
    val seoTitle: String?,
    val seoDescription: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
)
```

```kotlin
data class Episode(
    val id: String,
    val seriesId: String,
    val episodeNumber: Int,
    val slug: String,
    val title: String,
    val shortSynopsis: String?,
    val recap: String?,
    val highlights: List<String>,
    val videoProvider: VideoProvider,
    val videoUrl: String,
    val thumbnailUrl: String?,
    val durationSeconds: Int?,
    val publishStatus: PublishStatus,
    val publishedAt: Instant?,
    val seoTitle: String?,
    val seoDescription: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
)
```

Android DTOs must preserve production names. Mapping into domain models is explicit.

---

## 7. Publish state

Status: **VERIFIED-BEHAVIOR**

The database stores `is_published`, `published_at`, and `deleted_at`.

Current CMS preserves an existing `published_at` timestamp when content is unpublished.

Android may safely map:

```text
DRAFT
  deleted_at == null
  is_published == false
  published_at == null

PUBLISHED
  deleted_at == null
  is_published == true
  published_at != null

UNPUBLISHED
  deleted_at == null
  is_published == false
  published_at != null

DELETED
  deleted_at != null
```

Public visibility additionally requires:

```text
is_published = true
published_at <= now()
deleted_at IS NULL
```

A published episode is only public when its parent series is also public.

---

## 8. Soft delete and restore

### Series delete

Status: **VERIFIED**

Use production RPC:

```text
soft_delete_series(target_id uuid)
```

It:

1. soft-deletes all active child episodes;
2. sets child `is_published = false`;
3. soft-deletes the series;
4. sets series `is_published = false`.

### Episode delete

Status: **VERIFIED-BEHAVIOR**

Current CMS directly updates:

```text
deleted_at = now()
is_published = false
```

### Restore

Status: **UNSUPPORTED**

The audited CMS has no restore action/RPC and admin list queries hide deleted rows.

Android must not expose Restore until a production restore contract is deliberately added.

---

## 9. Episode ordering

Baseline audit status: **PARTIALLY VERIFIED**  
Post-audit extension status: **IMPLEMENTED IN REPOSITORY / PRODUCTION APPLY PENDING**

Ordering uses `episode_number`. The public per-series episode reader orders by `episode_number ASC`.

The unique constraint:

```text
(series_id, episode_number)
```

prevents duplicate numbers, including collisions with rows that are already soft-deleted.

The Phase 0 audited tree did not contain a dedicated reorder RPC. After that audit, migration:

```text
supabase/migrations/005_episode_reorder_rpc.sql
```

was added to `ferdilpu-sudo/ceritaria`.

It defines:

```text
reorder_episodes(
  target_series_id uuid,
  ordered_episode_ids uuid[]
)
```

The RPC:

1. requires `public.is_admin()`;
2. requires every active episode in the target series exactly once;
3. rejects duplicate or foreign/deleted episode IDs;
4. captures the existing ordered set of active `episode_number` values;
5. moves active rows to a temporary range above every current/historical number;
6. maps the captured numbers back onto episode IDs in the requested order;
7. performs the operation atomically inside one PostgreSQL function call.

The algorithm deliberately preserves the existing active number set instead of compacting to `1..N`. This avoids collisions with soft-deleted episodes that still own historical numbers under the unique constraint.

Android may use reorder only after migration 005 is applied to the target Supabase environment. A Git commit containing the migration is not proof that production has already executed it.

The existing web/PWA per-series reader will reflect the reordered values automatically because it already sorts by `episode_number`. The global admin episode list remains a recency-oriented list ordered by `created_at`, not a per-series reorder surface.

---

## 10. RLS policies

Status: **VERIFIED**

### admin_users

Authenticated users may select only their own membership row.

### series

Public/authorized readers may select only rows satisfying:

```text
is_published = true
published_at <= now()
deleted_at IS NULL
```

Authenticated admins have full series access through `public.is_admin()`.

### episodes

Public/authorized readers may select only rows satisfying:

```text
episode.is_published = true
episode.published_at <= now()
episode.deleted_at IS NULL
parent series is also published, visible, and not deleted
```

Authenticated admins have full episode access through `public.is_admin()`.

Android writes therefore remain protected by database policy even if a UI check fails.

---

## 11. Supabase Storage

Status: **VERIFIED**

Buckets:

```text
series-media
episode-media
```

Both buckets are public.

Limits:

```text
max size = 5 MiB
allowed MIME =
  image/jpeg
  image/png
  image/webp
```

Authenticated admin policies permit INSERT/UPDATE/DELETE in these buckets when `public.is_admin()` is true.

Current CMS upload path:

```text
{ownerId}/{randomUUID}.{extension}
```

Current DB fields store the generated public URL.

Android Phase 4 must match this production behavior unless the storage contract is deliberately migrated first.

---

## 12. Video providers

Status: **VERIFIED base + VERIFIED-MIGRATION R2 extension**

Phase 0 production values were:

```text
youtube
facebook
```

Migration 010 extends the target provider constraint to:

```text
youtube
facebook
r2
```

Source invariants after migration 010:

```text
youtube/facebook:
  video_url IS NOT NULL
  video_asset_id IS NULL

r2:
  video_url IS NULL
  video_asset_id IS NOT NULL
```

A trigger additionally requires an R2 asset to be `READY` and to belong to the same episode.

Studio does not manually invent R2 identifiers. The intended path is upload -> verify READY -> optional preview -> trusted attach endpoint/RPC. UNKNOWN remains a read-only forward-compatibility fallback and is never written without understanding it.

---

## 13. Analytics

Status: **VERIFIED**

Table: `public.analytics_events`

Core fields:

- `id uuid`
- `visitor_id uuid`
- `session_id uuid`
- `event_name varchar(80)`
- `path varchar(512)`
- `referrer_host varchar(255)`
- `device_type varchar(20)`
- `metadata jsonb`
- `created_at timestamptz`

Device constraint:

```text
mobile | tablet | desktop | unknown
```

Metadata serialized size is limited to 2048 bytes.

Direct public INSERT/UPDATE/DELETE is revoked.

### Event write RPC

```text
track_analytics_event(...)
```

Accepted events:

- `page_view`
- `episode_view`
- `play_intent`
- `next_episode_click`
- `previous_episode_click`
- `facebook_fallback_click`
- `youtube_fallback_click`

### Admin dashboard RPC

```text
get_analytics_dashboard(
  p_days integer default 7,
  p_timezone text default 'Asia/Jakarta'
)
```

The function clamps the requested period to 1..90 days and requires admin authorization.

Current web uses 7/30/90 day options.

Returned data includes:

- summary;
- 24-hour hourly pageviews/visitors;
- top pages;
- devices;
- referrers;
- event totals.

Realtime visitors are supplied separately by Supabase Realtime Presence.

Android analytics should consume the aggregate RPC instead of downloading raw event history.

---

## 14. Current server API surface relevant to Studio

Status: **VERIFIED**

The audited Next.js application currently exposes an API health route:

```text
GET /api/health
```

No R2/video upload API routes existed in the Phase 0 audited tree.

The current repository extension now exposes authenticated admin routes for:

```text
POST /api/video-uploads
GET  /api/video-uploads/{sessionId}
POST /api/video-uploads/{sessionId}/parts
POST /api/video-uploads/{sessionId}/complete
POST /api/video-uploads/{sessionId}/finalize
POST /api/video-uploads/{sessionId}/cancel

GET  /api/video-assets/{assetId}/preview
POST /api/episodes/{episodeId}/video-assets/{assetId}/attach
```

The upload API never publishes or attaches an episode merely because an object became READY. Preview is admin-only and short-lived. Attachment is a separate explicit mutation.

Content CRUD in the web CMS otherwise continues to use authenticated Supabase/server-action boundaries.

---

## 15. R2 repository extension

Status: **VERIFIED-MIGRATION — migrations 006–010 committed; production apply/smoke pending**

### video_assets

Migration 006 defines:

```text
id uuid PK
episode_id uuid -> episodes(id)
status PENDING|UPLOADING|UPLOADED|VERIFYING|READY|FAILED|CANCELLED|REPLACED
object_key text unique
mime_type video/mp4
expected_size_bytes bigint
actual_size_bytes bigint?
etag text?
checksum_sha256 char(64)?
created_by uuid -> auth.users(id)
ready_at timestamptz?
created_at timestamptz
updated_at timestamptz
```

Admin RLS controls mutation. Migration 010 additionally allows public SELECT only for a READY asset that is attached to a currently published, non-deleted episode whose series is also published and non-deleted.

### video_upload_sessions

Migrations 006–009 define:

```text
id uuid PK
asset_id uuid unique -> video_assets(id)
mode SINGLE|MULTIPART
status CREATED|UPLOADING|COMPLETING|UPLOADED|VERIFYING|READY|CANCELLED|FAILED|EXPIRED
r2_upload_id text?
part_size_bytes bigint?
part_count int?
expires_at timestamptz
created_by uuid -> auth.users(id)
created_at timestamptz
updated_at timestamptz
```

Multipart rows require upload ID, part size and part count. The final part-size type is `bigint` after migration 009.

### Episode attachment after migration 010

Migration 010 adds:

```text
episodes.video_asset_id uuid? -> video_assets(id)
episodes.video_url becomes nullable
video_provider IN ('youtube','facebook','r2')
```

The source check ensures legacy providers remain URL-backed while R2 rows are asset-backed.

`attach_ready_video_asset(episodeId, assetId)` is the atomic attachment RPC. It locks/validates the READY target, swaps the episode source, marks a previous R2 asset `REPLACED`, and returns the previous asset ID. Server code performs old-object cleanup only after the database swap succeeds.

### Server state transition RPCs

Committed migrations include explicit RPCs for:

```text
create upload records
mark transfer uploaded
finalize verified READY
fail upload
cancel upload
attach READY asset
```

Permanent R2 credentials and request signing remain server-side. Android receives only short-lived operation URLs.

### Production warning

Repository verification does not prove migrations 006–010 are already applied to the live Supabase project. Production use of R2 must remain gated until migration order, RLS, endpoints, R2 environment values, preview, attachment and public playback have been smoke-tested.

---

## 16. Local Android VideoJob

Status: **ANDROID-LOCAL CONTRACT**

This belongs in Room, not in production catalog tables.

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

Encoding states:

```text
NOT_REQUIRED
QUEUED
ENCODING
READY
FAILED
CANCELLED
```

Upload states:

```text
NOT_STARTED
QUEUED
UPLOADING
VERIFYING
READY
FAILED
CANCELLED
```

Encoding READY does not imply upload READY.

Upload READY does not imply episode PUBLISHED.

---

## 17. Phase 0 verification checklist

- [x] read all current `supabase/migrations/*` in order;
- [x] record exact `series` schema;
- [x] record exact `episodes` schema;
- [x] record exact `admin_users` schema;
- [x] verify publish behavior from CMS actions;
- [x] verify soft-delete behavior;
- [x] inspect RLS policies;
- [x] inspect storage buckets/policies;
- [x] inspect CMS create/update/delete actions;
- [x] inspect TypeScript database types;
- [x] verify YouTube/Facebook provider representation;
- [x] inspect analytics table/RPCs;
- [x] inspect current API route surface;
- [x] determine R2 migration requirement;
- [x] reconcile Android-facing schema documentation.

Phase 0 schema exit gate: **PASS**.

---

## 18. Compatibility rule

If Android needs a production field or behavior not documented as VERIFIED:

1. do not invent it client-side;
2. propose the backend/schema change;
3. verify existing web compatibility;
4. deploy the backend/migration first;
5. update shared documentation/types;
6. only then ship the Android dependency.

This is less exciting than debugging two conflicting realities in production, which is precisely the point.
