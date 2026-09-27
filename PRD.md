# Ceritaria Studio Android --- Product Requirements Document

## 1. Summary

Ceritaria Studio is a private native Android app for managing the
Ceritaria mini-series catalog from a phone. It uses the same production
content source as the existing Ceritaria web/PWA and CMS.

This implementation baseline includes local video inspection/encoding and secure direct R2 upload as core delivery features, gated by the verified production schema and trusted server upload contract.

## 2. Problem

Content administration currently lives primarily in the web CMS. Mobile
production workflows need a faster way to inspect the catalog,
create/edit episodes, upload artwork, change publishing state and check
performance without opening a desktop workflow.

A second independent content store would create synchronization
failures. Studio therefore operates on the same authenticated Ceritaria
backend.

## 3. User

Primary user: Ceritaria owner/admin/content operator.

The initial production release assumes an authenticated admin, not public creators and not
multi-tenant creator accounts.

## 4. Jobs to be done

-   "I want to see all series and episodes already published."
-   "I want to add the next episode from my phone."
-   "I want to fix title, synopsis, recap or artwork quickly."
-   "I want to keep an episode as draft before publishing."
-   "I want to replace media without losing the episode identity."
-   "I want to know whether content is published and healthy."
-   "I want a compact view of performance."

## 5. Information architecture

Bottom navigation: 1. Home 2. Series 3. Episodes 4. Analytics

Settings/profile is accessed from the top app bar, not a fifth permanent
destination.

### Home

-   compact operational summary;
-   drafts requiring attention;
-   recently edited/published episodes;
-   quick actions: New Episode, New Series;
-   failed/pending upload banner.

### Series

-   search;
-   filters: All, Published, Draft, Featured;
-   sortable list;
-   series detail;
-   edit;
-   episode list.

### Episodes

-   search across episodes;
-   filter by series/status/provider;
-   sort newest/oldest/episode number;
-   detail/edit;
-   publish state.

### Analytics

-   selected time range;
-   total views;
-   unique visitors where available;
-   player events;
-   top content supported by existing analytics.

## 6. Functional requirements

### FR-01 Authentication

-   Sign in using Supabase Auth.
-   Only users authorized by Ceritaria admin policy may enter.
-   Restore valid sessions.
-   Expired/invalid sessions return to login.
-   Sign out clears local session material.

### FR-02 Series list

Display: - artwork thumbnail; - title; - status; - featured state; -
episode count where efficient; - updated/published metadata where
available.

Actions: - search; - filter; - refresh; - open; - create.

### FR-03 Series editor

Must support fields already recognized by production schema/CMS. Do not
invent production columns in UI before schema verification.

Conceptual fields: - title; - slug; - synopsis/description; - cover; -
hero; - status; - featured; - SEO/editorial fields already supported by
web.

### FR-04 Episode list

Display: - thumbnail; - episode number/order; - title; - parent
series; - status; - video provider; - publish information.

### FR-05 Episode editor

Conceptual fields: - parent series; - episode number/order; - title; -
slug; - description; - recap; - important moments; - video provider; -
video URL/provider identifier; - thumbnail; - publish state; - supported
SEO fields.

Exact persistence fields must follow verified production schema.

### FR-06 Publishing

-   Save Draft.
-   Publish.
-   Unpublish if supported by current backend contract.
-   Confirmation for destructive/visibility-changing actions.
-   Prevent invalid publish, such as missing required fields.
-   Never silently convert draft into published.

### FR-07 Delete

-   Use production soft-delete semantics.
-   Do not hard-delete catalog content from the normal UI.
-   Show clear confirmation.
-   Restore only if backend contract supports it.

### FR-08 Reordering

-   Episode order can be changed inside a series.
-   Update must avoid duplicate order values.
-   UI must show save progress and rollback/reload on failure.

### FR-09 Images

-   Select using Android system photo picker.
-   Validate MIME type and size.
-   Upload to existing authorized storage.
-   Show progress.
-   Retry recoverable failures.
-   Replace image atomically from the user perspective.

### FR-10 Video providers

The release preserves YouTube and Facebook URL-backed episodes and adds R2 asset-backed episodes after the committed server/database migration is applied.

R2 is not selected by typing an object URL or asset ID. The supported workflow is local preparation -> secure upload -> server verification -> READY -> optional preview -> explicit atomic attachment.

Upload READY alone does not change the episode provider and does not publish content.

### FR-11 Video encoding

-   Use AndroidX Media3 Transformer as the primary transformation
    pipeline.
-   Prefer hardware MediaCodec encoding when supported by the device.
-   Never perform blocking/manual codec work on the main thread. Media3 Transformer lifecycle/control calls remain on its required application looper while Media3 owns the codec pipeline.
-   Inspect codec, resolution, fps, duration, orientation and file size
    before deciding to transcode.
-   Default output: MP4, H.264/AVC, AAC, max 1080×1920 portrait, max 30
    fps, AAC target 128 kbps.
-   Preserve aspect ratio and correct rotation; never stretch video to
    9:16.
-   If source is already compatible, permit direct upload without lossy
    re-encode.
-   Show progress and allow cancellation.
-   Keep encoded output until upload/finalization succeeds or user
    explicitly discards it.
-   Surface insufficient-storage and unsupported-codec errors clearly.

### FR-12 R2 upload

-   Upload only through a secure server-mediated signed/temporary
    authorization flow.
-   Show bytes uploaded and total bytes when known.
-   Upload/finalization must survive normal navigation and
    backgrounding.
-   Retry recoverable failures without re-encoding when output still
    exists.
-   Verify server acceptance before attaching asset to episode.
-   Allow an authorized admin to preview a READY replacement through a short-lived server-signed URL.
-   Keep upload READY and episode attachment as separate states; attachment is explicit and atomic.
-   Replacing a video keeps the same episode identity.
-   Failed upload must not overwrite the currently valid episode video.

### FR-13 Analytics

Read-only access to existing first-party analytics: - pageviews; -
unique visitors where existing data supports it; - player events; - top
content/pages; - time range.

### FR-14 Search

Debounced search. Search state survives simple navigation back to the
list.

## 7. UX requirements

-   Phone-first portrait design.
-   Support landscape without broken layouts.
-   Compact professional admin UI, not oversized dashboard cards.
-   Primary actions reachable with one hand where practical.
-   Minimum touch target 48dp.
-   Visible progress for network/upload work.
-   Never use an indefinite spinner when meaningful progress is
    available.
-   Preserve unsaved form state during configuration change.
-   Warn before discarding dirty forms.
-   Snackbar for transient feedback; inline errors for field problems.
-   Destructive actions must not sit adjacent to primary Save without
    separation.

## 8. Reliability requirements

-   No duplicate record creation from double taps.
-   Save button disabled while the same mutation is active.
-   Idempotency or safe retry for upload/finalize flows.
-   App handles no-network state.
-   Durable background execution uses the mechanism appropriate to the job type and supported Android version; do not assume one generic Worker is correct for every long-running media operation.
-   Image transfer and short durable work may use WorkManager.
-   Video encoding and video transfer are separate jobs with separate states and lifecycle handling.
-   Room persists operational `VideoJob` state so recoverable encoding/upload work can survive navigation and process recreation.
-   Large video transfer must support resumable/multipart behavior when required by the verified server contract.
-   Database record must not reference an upload that never completed.
-   Sensitive writes depend on backend RLS/server authorization, not UI
    hiding.

## 9. Performance targets

-   First useful authenticated screen: target \<2.5s on normal
    connection after warm session.
-   List interaction should remain smooth at hundreds of episodes.
-   Paginate rather than load an unbounded catalog.
-   Thumbnail images use appropriately sized requests.
-   Avoid full-size hero downloads in list rows.

## 10. Acceptance scenario

Admin signs in, opens Wajah Kedua, sees existing episodes, edits an
episode title and thumbnail, saves as draft, publishes it, then verifies
the same data appears on Ceritaria web. No export/import or manual sync
occurs.
