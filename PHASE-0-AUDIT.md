# Phase 0 — Production Contract Audit

Status: **COMPLETED**  
Audit date: **2026-09-27**  
Production source audited: `ferdilpu-sudo/ceritaria` branch `main`  
Audited tree SHA: `c7ea336224a5baaf37af3645ff375981051a8b31`

## 1. Sources inspected

Production migrations, generated/manual database types, CMS mutations, validation, media upload, public readers, analytics, authorization, and current API routes were inspected.

Primary files:

- `supabase/migrations/001_initial.sql`
- `supabase/migrations/002_youtube_video_provider.sql`
- `supabase/migrations/003_brand_ceritaria.sql`
- `supabase/migrations/004_first_party_analytics.sql`
- `src/types/database.types.ts`
- `src/lib/security/require-admin.ts`
- `src/features/admin/actions/series-actions.ts`
- `src/features/admin/actions/episode-actions.ts`
- `src/features/admin/services/admin-content.ts`
- `src/features/admin/services/media-upload.ts`
- `src/features/admin/services/schemas.ts`
- `src/features/series/services/public-series.ts`
- `src/features/episode/services/public-episodes.ts`
- `src/features/episode/components/EpisodeVideoEmbed.tsx`
- `src/features/analytics/services/admin-analytics.ts`
- `docs/ANALYTICS.md`
- `tests/schema-validation.test.ts`
- `src/app/api/health/route.ts`

## 2. Verified production entities

### admin_users

Exact columns:

- `user_id uuid primary key references auth.users(id) on delete cascade`
- `created_at timestamptz not null default now()`

There is no role/status column in the current schema.

### series

Exact columns:

- `id uuid primary key default gen_random_uuid()`
- `slug varchar(160) not null unique`
- `title varchar(200) not null`
- `short_synopsis varchar(320)`
- `synopsis text`
- `genres text[] not null default '{}'`
- `cover_url text`
- `hero_url text`
- `is_featured boolean not null default false`
- `is_published boolean not null default false`
- `published_at timestamptz`
- `seo_title varchar(200)`
- `seo_description varchar(320)`
- `created_at timestamptz not null default now()`
- `updated_at timestamptz not null default now()`
- `deleted_at timestamptz`

Constraint: published rows require non-null `published_at`.

### episodes

Exact columns:

- `id uuid primary key default gen_random_uuid()`
- `series_id uuid not null references series(id) on delete cascade`
- `episode_number integer not null check (episode_number > 0)`
- `slug varchar(160) not null`
- `title varchar(200) not null`
- `short_synopsis varchar(320)`
- `recap text`
- `highlights text[] not null default '{}'`
- `video_provider varchar(30) not null default 'youtube'`
- `video_url text not null`
- `thumbnail_url text`
- `duration_seconds integer`
- `is_published boolean not null default false`
- `published_at timestamptz`
- `seo_title varchar(200)`
- `seo_description varchar(320)`
- `created_at timestamptz not null default now()`
- `updated_at timestamptz not null default now()`
- `deleted_at timestamptz`

Unique constraints:

- `(series_id, episode_number)`
- `(series_id, slug)`

Current provider constraint:

- `video_provider in ('youtube', 'facebook')`

Therefore R2 is **not representable by the current provider constraint**.

## 3. Verified publish semantics

The existing CMS writes `is_published` and preserves the original `published_at` after first publication.

Safe Android mapping:

- **DRAFT**: `is_published = false`, `published_at = null`, `deleted_at = null`
- **PUBLISHED**: `is_published = true`, `published_at != null`, `deleted_at = null`
- **UNPUBLISHED**: `is_published = false`, `published_at != null`, `deleted_at = null`
- **DELETED**: `deleted_at != null`; production delete also sets `is_published = false`

Public readers additionally require `published_at <= now()`.

## 4. Verified authorization and RLS

`public.is_admin()` returns true when the current `auth.uid()` exists in `public.admin_users`.

Policies:

- authenticated user can read only their own `admin_users` membership row;
- anon/authenticated can read only published, non-deleted series whose `published_at <= now()`;
- anon/authenticated can read only published, non-deleted episodes whose parent series is also public;
- authenticated admins can manage all series;
- authenticated admins can manage all episodes.

Android may authenticate directly with Supabase as a normal user and test admin membership using the existing protected contract. It must not ship a service-role key.

## 5. Verified soft-delete behavior

Series deletion uses RPC `soft_delete_series(target_id uuid)`:

1. marks all non-deleted child episodes with `deleted_at = now()` and `is_published = false`;
2. marks the series with `deleted_at = now()` and `is_published = false`.

Episode deletion directly updates the episode to `deleted_at = now()`, `is_published = false`.

**Restore is not implemented by the current CMS.** Android must not expose Restore in the initial implementation unless a production restore contract is added explicitly.

## 6. Verified image storage

Buckets:

- `series-media`
- `episode-media`

Both are public buckets.

Limits:

- 5 MiB maximum file size;
- allowed MIME: `image/jpeg`, `image/png`, `image/webp`.

Admin insert/update/delete is protected by `public.is_admin()`.

Current CMS path convention:

```text
{ownerId}/{randomUUID}.{extension}
```

The database currently stores the resulting public URL in `cover_url`, `hero_url`, or `thumbnail_url`.

Android Phase 4 must match this behavior unless production is deliberately migrated first.

## 7. Verified form validation

Series:

- slug: lowercase kebab-case, 2..160;
- title: 2..200;
- short synopsis: <=320;
- synopsis: <=8000;
- genre input converted to max 12 items;
- SEO title <=200;
- SEO description <=320.

Episode:

- series UUID required;
- episode number: positive integer, max 10000;
- slug: lowercase kebab-case, 2..160;
- title: 2..200;
- short synopsis <=320;
- recap <=20000;
- highlights converted from lines to max 12 items;
- provider: youtube or facebook;
- video URL validated according to selected provider;
- duration: positive integer, max 86400 seconds;
- SEO title <=200;
- SEO description <=320.

## 8. Verified analytics contract

Table: `analytics_events`.

Public clients cannot directly INSERT/SELECT it.

Write RPC:

- `track_analytics_event(...)`

Admin report RPC:

- `get_analytics_dashboard(p_days, p_timezone)`

Dashboard supports clamped 1..90 day ranges; current web UI uses 7/30/90 and `Asia/Jakarta`.

Returned aggregate includes:

- today pageviews/visitors;
- period pageviews/visitors/sessions/total events;
- 24-hour hourly data;
- top pages;
- devices;
- referrers;
- event counts.

Realtime online visitors use Supabase Realtime Presence, not `analytics_events`.

## 9. Episode ordering finding

Production has a unique `(series_id, episode_number)` constraint.

There is currently no dedicated reorder RPC or batch transaction contract in the audited repository. The CMS can edit `episode_number`, but an Android drag-reorder workflow must not perform a naive sequence of conflicting updates.

Phase 3 must either:

- introduce a safe server/RPC reorder operation; or
- use another verified conflict-safe algorithm before enabling drag reorder.

## 10. R2 finding

Current production has:

- no `video_assets` table;
- no R2 provider value;
- no R2 upload-session table;
- no `/api/video-uploads` routes;
- only the unrelated `/api/health` route under the current API tree.

Therefore direct R2 video **requires backend work before shipping**.

At minimum a backward-compatible migration must resolve how READY R2 assets are represented. Preferred direction remains a dedicated media/video asset entity rather than storing transient upload state in `episodes`.

Required server capabilities for Phase 6:

- create authorized upload session;
- authorize/initiate multipart work when required;
- complete multipart transfer;
- verify uploaded object;
- finalize READY asset;
- cancel/abort incomplete transfer;
- attach/replace READY media atomically.

Permanent R2 credentials must remain server-side.

## 11. Android contract decisions unlocked by Phase 0

Phase 1/2 may now safely use:

- direct Supabase Auth user sessions;
- `admin_users(user_id, created_at)`;
- exact `series` and `episodes` fields documented in `SCHEMA.md`;
- direct admin reads protected by RLS;
- existing public image URLs;
- analytics RPC contract.

Not yet unlocked:

- R2 persistence;
- R2 playback URL strategy;
- restore workflow;
- drag reorder mutation;
- any new production column/table not defined by audited migrations.

## 12. Phase 0 exit gate

**PASS.**

No Android repository code needs to invent the current Ceritaria production schema. Remaining gaps are explicitly classified as later migrations/server contracts rather than being silently guessed.
