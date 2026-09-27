# Phase 7 — Episode Video Integration

Status: **CODE / CI PASS; PRODUCTION MIGRATION + SMOKE PENDING**  
Implementation date: **2026-09-27**

## 1. Goal

Phase 7 turns a verified remote R2 asset into an actual episode video without weakening the replacement safety contract.

The key distinction is:

```text
remote asset READY != episode attached
```

The previous production video remains authoritative until the explicit attachment transaction succeeds.

## 2. Repository contract

Ceritaria backend migrations 006–009 provide R2 asset/upload state. Migration 010 adds the episode attachment contract:

- `episodes.video_asset_id`;
- provider `r2`;
- URL-backed YouTube/Facebook invariant;
- asset-backed R2 invariant;
- trigger validation for READY same-episode assets;
- public RLS for READY assets attached to published content;
- atomic `attach_ready_video_asset` RPC.

These migrations are committed and CI-verified in the repository. Their live production application still requires deployment verification.

## 3. Atomic attachment

The server attachment flow is:

```text
READY target asset
 -> lock/validate asset
 -> lock active episode
 -> update episode:
      provider = r2
      video_url = null
      video_asset_id = target
 -> mark previous R2 asset REPLACED
 -> commit
 -> best-effort delete previous R2 object
```

If target validation or the database mutation fails, the existing episode source is unchanged.

If old-object deletion fails after a successful swap, the new valid episode reference remains active and the failure is reduced to orphan cleanup rather than broken playback.

## 4. Admin preview

A READY asset may be previewed before attachment through:

```text
GET /api/video-assets/{assetId}/preview
```

The endpoint requires bearer admin authorization and returns a short-lived presigned R2 GET URL.

Android treats the preview URL as one-shot operation data:

- it is not stored in Room;
- it is not written to the episode;
- it is not treated as a permanent delivery URL;
- preview failure does not change upload READY or attachment state.

## 5. Android attachment boundary

Android uses a dedicated `EpisodeVideoAssetRepository` separate from low-level upload.

Attachment UI state is also separate:

```text
IDLE
ATTACHING
ATTACHED
FAILED
```

The Episode editor only shows the attach action when the local upload job reports a verified remote asset ID.

On success, the editor synchronizes:

```text
videoProvider = r2
videoUrl = empty
videoAssetId = attached asset
```

into both the current and persisted-form baseline for those media fields. Unrelated unsaved title/story/SEO edits remain dirty and are not silently marked saved.

## 6. Public playback

The public web service resolves the attached READY asset object key and builds delivery through the configured `R2_VIDEO_PUBLIC_BASE_URL`.

The player uses HTML5 video with MP4 source, controls, playsInline, poster artwork, and object-contain presentation.

Public RLS prevents unattached or unpublished assets from being read through the normal public data contract.

## 7. Legacy provider compatibility

Existing YouTube/Facebook episodes continue to use `video_url`.

R2 is activated by trusted attachment rather than by manually typing an R2 URL/asset ID.

A pending local replacement does not invalidate an existing YouTube/Facebook or attached R2 source. Uploading, verifying or previewing a replacement never changes the active episode.

## 8. Publish semantics

Upload completion and attachment never automatically publish an episode.

The episode's normal content publish state remains explicit.

The editor validator requires the active source contract to be valid:

- YouTube/Facebook: supported provider URL;
- R2: valid attached asset ID.

A pending replacement alone is not a reason to unpublish/block an otherwise valid existing episode.

## 9. Automated verification

Relevant CI evidence:

- backend upload contract code gate: `36318207100`;
- backend preview contract: `36321253749`;
- Android Phase 6 final upload state: `36318085202`;
- Android R2 provider compatibility fixture: `36320558227`;
- Android READY attachment UI/repository: `36320883372`;
- Android preview + attachment integration Verify: `36321377376`.

Tests cover provider mapping/validation, attachment response/error handling, attachment ViewModel state/effect, editor synchronization while preserving unrelated dirty edits, and short-lived preview repository/ViewModel behavior.

## 10. Production gate

Phase 7 is not production-complete until:

1. migrations 006–010 are applied to the real Supabase project in order;
2. R2 upload credentials and policy configuration are set server-side;
3. the public delivery domain is connected and `R2_VIDEO_PUBLIC_BASE_URL` configured;
4. preview TTL is configured;
5. non-admin preview/attach requests are rejected;
6. one legacy YouTube/Facebook episode is replaced with READY R2 and remains unchanged before attach;
7. one attached R2 asset is replaced by another and old-object cleanup behavior is inspected;
8. failed attach leaves the current source unchanged;
9. published R2 playback works on the public site;
10. unpublished/deleted R2 media is not exposed through the public contract;
11. Android reload/list/detail/editor all display the attached R2 source correctly.

Code/CI is complete; migration/configuration/runtime smoke is the remaining gate.
