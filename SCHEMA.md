# Ceritaria Studio Android --- Data Contract & Schema Plan

## 1. Purpose

This document is the Android-facing data contract.

Important: exact production table/column definitions must be verified
against the current Ceritaria Supabase migrations before implementation.
The public repository confirms that Ceritaria uses Supabase
Postgres/Auth/Storage/RLS, soft delete, series/episodes, admin users,
YouTube-first video provider support, and first-party analytics. This
document intentionally does not fabricate exact column names that have
not been verified.

Legend: - **VERIFIED-CONCEPT**: confirmed as a capability/entity by
current repository documentation. - **VERIFY-FIELD**: exact production
column/type must be read from migration. - **PROPOSED**: future schema,
not safe to assume exists.

## 2. Existing entity map

``` text
auth.users
    |
    | 1:0..1 authorization mapping
    v
admin_users

series
    |
    | 1:N
    v
episodes

series/episodes
    |
    +--> image references --> Supabase Storage

analytics events/pageviews/visitors
    ^
    |
public Ceritaria web
```

## 3. Admin authorization

### `auth.users`

Status: VERIFIED-CONCEPT

Supabase Auth identity. Android authenticates as a normal user session.

### `admin_users`

Status: VERIFIED-CONCEPT

Purpose: identifies users allowed to operate the CMS/admin surface.

Android rule: - authentication alone is insufficient; - authorization
must match production policy; - RLS remains authoritative.

Fields to verify: - primary/user UUID; - role/status if any; -
timestamps.

## 4. Series

Status: VERIFIED-CONCEPT

Required domain representation:

``` kotlin
data class Series(
    val id: String,
    val title: String,
    val slug: String,
    val synopsis: String?,
    val coverUrl: String?,
    val heroUrl: String?,
    val status: PublishStatus,
    val isFeatured: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val publishedAt: Instant?,
    val deletedAt: Instant?
)
```

This is a domain target, not proof of production field names.

Verify from migration: - ID type; - title/slug fields; -
description/synopsis naming; - cover/hero storage fields; -
draft/publish representation; - featured field; - SEO fields; -
ordering; - created/updated/published timestamps; - soft-delete field.

Constraints Android must respect: - slug uniqueness if enforced; -
required title; - publish requirements; - soft delete semantics.

## 5. Episodes

Status: VERIFIED-CONCEPT

Required domain representation:

``` kotlin
data class Episode(
    val id: String,
    val seriesId: String,
    val episodeNumber: Int?,
    val sortOrder: Int?,
    val title: String,
    val slug: String,
    val description: String?,
    val recap: String?,
    val importantMoments: String?,
    val thumbnailUrl: String?,
    val videoProvider: VideoProvider,
    val videoReference: String,
    val status: PublishStatus,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val publishedAt: Instant?,
    val deletedAt: Instant?
)
```

Verify exact persistence for: - relation to series; - episode number vs
sort order; - title/slug; - description; - recap; - important moments; -
thumbnail; - video URL/provider ID; - `video_provider` representation; -
publish state; - SEO; - soft delete.

### Provider enum

Repository documentation confirms: - YouTube is primary for new
content. - Facebook remains for legacy rows.

Android domain:

``` text
YOUTUBE
FACEBOOK
UNKNOWN
```

Do not crash on unknown future database value. Preserve/read unknown
values safely and block destructive overwrite until understood.

## 6. Publish state

Status: VERIFIED-CONCEPT that draft/publish exists; exact representation
must be verified.

Android domain:

``` text
DRAFT
PUBLISHED
UNPUBLISHED (only if backend supports distinct state)
DELETED
```

Do not infer state from nullable dates until the production
implementation is inspected.

## 7. Soft delete

Status: VERIFIED-CONCEPT

Rules: - normal Android delete calls production soft-delete behavior; -
queries hide deleted rows by default unless a Trash/restore feature is
explicitly implemented; - no normal UI hard delete.

Verify: - field name/type; - RLS visibility; - restore behavior; -
cascade behavior for series with episodes.

## 8. Storage

Status: VERIFIED-CONCEPT

Current web CMS supports cover/hero/thumbnail upload via Supabase-backed
media.

Verify: - bucket names; - public/private bucket policy; - path
convention; - max sizes; - accepted MIME types; - whether DB stores full
URL or object path.

Android must reuse the same convention.

Preferred domain object:

``` text
MediaRef
- bucket
- objectPath
- publicUrl? (derived where possible)
```

Do not permanently couple domain logic to a hardcoded Supabase public
URL.

## 9. Analytics

Status: VERIFIED-CONCEPT

Repository documentation confirms first-party: - realtime visitor; -
pageview; - unique visitor; - top pages; - device; - referrer; - player
events.

Android initial release needs read-only aggregate access.

Verify actual tables/views/RPCs before implementation. Prefer existing
safe views/RPCs over downloading raw analytics events to the phone.

## 10. Relationships

``` text
Series 1 ----- N Episode

AuthUser 1 --- 0..1 AdminUser

Series/Episode ---- media object references

Episode.videoProvider + Episode.videoReference
    -> external or direct-upload video source according to the verified provider contract
```

## 11. Android DTO policy

DTO names reflect database contracts:

``` text
SeriesRowDto
EpisodeRowDto
AdminUserRowDto
AnalyticsSummaryDto
```

Mapping is explicit:

``` text
Supabase row
 -> DTO
 -> mapper
 -> domain
 -> ViewModel UI state
```

No `Map<String, Any>` beyond a narrow adapter boundary.

## 12. Core delivery extension: direct uploaded video

Status: CORE DELIVERY CAPABILITY; PRODUCTION MIGRATION MAY BE REQUIRED BEFORE R2 VIDEO SHIPS

The existing production migration must be audited first. If the current
episode schema cannot safely represent R2 assets, add an explicit
backward-compatible migration rather than overloading YouTube/Facebook
fields.

Prefer a separate media/upload entity rather than stuffing transient
upload state into `episodes`.

Conceptual:

``` text
video_assets
- id
- episode_id
- provider = R2
- object_key
- mime_type
- size_bytes
- checksum
- upload_status
- processing_status
- created_at
- verified_at
```

Possible statuses:

``` text
PENDING
UPLOADING
UPLOADED
VERIFYING
READY
FAILED
REPLACED
```

Episode should reference only a READY asset when published.

The final SQL must be designed only after the current production schema
and delivery/player strategy are verified.

## 13. Schema verification checklist

Before Android persistence code: - \[ \] read all current
`supabase/migrations/*` in order; - \[ \] record exact `series`
schema; - \[ \] record exact `episodes` schema; - \[ \] record
`admin_users`; - \[ \] record analytics tables/views/RPCs; - \[ \]
record RLS policies; - \[ \] record storage buckets/policies; - \[ \]
inspect web CMS create/update/delete actions; - \[ \] inspect TypeScript
database types; - \[ \] reconcile this document; - \[ \] mark exact
fields VERIFIED.

## 14. Compatibility rule

If Android needs a field not present in production: 1. do not invent it
client-side; 2. propose a migration; 3. verify old web code tolerates
it; 4. deploy backend migration first; 5. update web/types if needed; 6.
then release Android dependency.

This ordering prevents the thrilling experience of discovering that the
mobile app and production website have developed separate
interpretations of reality.

## 15. Local Android video job model

This is an Android persistence/state model, not a duplicate catalog table in Supabase.
Room is required as the durable operational store for this state.
Series and episode production content remain authoritative in Supabase.

``` text
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

Encoding state:

``` text
NOT_REQUIRED
QUEUED
ENCODING
READY
FAILED
CANCELLED
```

Upload state:

``` text
NOT_STARTED
QUEUED
UPLOADING
VERIFYING
READY
FAILED
CANCELLED
```

Encoding READY does not imply upload READY. Upload READY does not
automatically imply episode PUBLISHED.


## 16. Server upload-session contract

The trusted Ceritaria server owns privileged R2 authorization and canonical object naming.
The exact persistence implementation is verified during Phase 0 and must not be invented by Android.

Conceptual server-facing state:

``` text
UploadSession
- id
- assetId
- objectKey
- mode = SINGLE_PUT | MULTIPART
- expiresAt
- expectedMimeType
- expectedSizeBytes?
- multipartUploadId?
- status
```

Rules:
- authorization is short-lived and scoped;
- permanent R2 credentials are never returned to Android;
- multipart part identifiers/ETags required for resume are persisted locally in `VideoJob.multipartState`;
- upload completion is not equivalent to media verification;
- only a verified READY asset may replace the episode's active production video.

## 17. R2 object naming

Final convention must be server-controlled.

Conceptual example:

``` text
video/{seriesId}/{episodeId}/{assetId}/stream.mp4
```

Do not trust a user-entered filename as the canonical object key.

The server should generate/validate: - bucket; - object key; - permitted
MIME type; - expected maximum size; - expiration of upload
authorization.

Android receives temporary upload authorization, not permanent R2
credentials.
