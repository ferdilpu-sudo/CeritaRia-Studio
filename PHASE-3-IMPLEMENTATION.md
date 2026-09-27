# Phase 3 — Content Mutations Implementation

Status: **CODE / CI PASS; PRODUCTION MIGRATION + SMOKE PENDING**  
Implementation date: **2026-09-27**

## 1. Scope implemented

Android now supports the mutation behavior that the verified Ceritaria contract can represent:

- create and edit series;
- create and edit episodes;
- draft save;
- publish;
- unpublish while preserving the first publication timestamp;
- featured toggle for series;
- series soft delete;
- episode soft delete;
- conflict-safe episode reorder;
- matching CMS validation;
- duplicate-save protection;
- dirty-form exit protection;
- explicit success/failure states.

Restore is intentionally absent because the production CMS still has no restore action/RPC and deleted rows are hidden by admin queries.

## 2. Series mutations

Series writes use a dedicated `SeriesSaveCommand`, write DTO, repository method, and Supabase data-source adapter.

Behavior matches the existing web CMS:

- create generates a UUID client-side before insert;
- update preserves record identity;
- first publish assigns `published_at`;
- unpublish preserves an existing `published_at`;
- duplicate slug conflicts map to a user-facing conflict state;
- delete calls the existing production `soft_delete_series(target_id uuid)` RPC.

The editor is split into identity, story, media URL, and publishing/SEO sections. File upload remains Phase 4 ownership.

## 3. Episode mutations

Episode writes use a dedicated `EpisodeSaveCommand`, write DTO, repository method, and Supabase data-source adapter.

Behavior matches the existing web CMS:

- provider writes are restricted to `youtube` and `facebook`;
- unknown future provider values remain readable but cannot be written back blindly;
- YouTube URL validation mirrors the production HTTPS host/ID shapes;
- Facebook validation mirrors the supported public reel/video/watch permalink shapes;
- first publish assigns `published_at`;
- unpublish preserves an existing `published_at`;
- duplicate number/slug conflicts surface as conflict errors;
- episode delete sets `deleted_at` and `is_published = false`.

## 4. Reorder backend extension

The Phase 0 baseline had no safe multi-row reorder contract.

A new migration was added to the Ceritaria web repository:

```text
supabase/migrations/005_episode_reorder_rpc.sql
Ceritaria commit: 4828d57c926b2d40f40b8e6a3bd51a3c64e23cd6
```

The Ceritaria repository CI for that commit passed.

RPC:

```text
reorder_episodes(target_series_id uuid, ordered_episode_ids uuid[])
```

The function is admin-gated and atomic. It validates the complete active set, temporarily moves active episode numbers above the existing range, then reassigns the original active number set according to the requested ID order.

This avoids transient unique-constraint collisions and avoids collisions with soft-deleted rows retaining historical episode numbers.

## 5. Android reorder UX

Reorder is exposed from Series detail.

The dedicated screen:

- loads only active episodes for the selected series ordered by `episode_number`;
- uses explicit Move Up / Move Down actions;
- keeps changes local until Save Order;
- guards dirty navigation;
- sends one ordered ID list to the RPC;
- reloads the backend after success;
- shows conflict/general failure feedback.

No per-row mutation loop is used.

## 6. Architecture / file ownership

Mutation responsibilities remain separated across:

- domain commands/repository contracts;
- Supabase data-source adapters;
- editor state/validation/ViewModels;
- small Compose sections;
- dedicated reorder state/ViewModel/screen;
- feature-specific navigation graphs.

`StudioShell` remains a shell rather than accumulating every route. Series and Episode navigation are split into their own graph files.

## 7. Verification evidence

Android CI verified the mutation slices incrementally, including:

- series mutation data contract;
- series editor state/validation;
- series mutation UI;
- episode mutation data contract;
- episode editor state/validation after minSdk-compatible URL decoding fix;
- episode mutation UI;
- reorder contract.

The final reorder UI CI result must be green before this document is committed as the Phase 3 code/build record.

## 8. Production gate still pending

GitHub success does not prove the real Supabase project has executed migration 005.

Before Phase 3 becomes fully complete:

1. apply `005_episode_reorder_rpc.sql` to the production Supabase project;
2. configure the Android app with the real public Supabase URL/publishable key through secure local/CI configuration;
3. sign in as an authorized admin;
4. smoke-test series create/edit/publish/unpublish/delete;
5. smoke-test episode create/edit/publish/unpublish/delete;
6. reorder episodes and confirm the public Ceritaria per-series view reflects the new order;
7. verify RLS rejects unauthorized mutation attempts;
8. confirm no manual synchronization is required.

No privileged key belongs in the APK or repository.
