# Phase 5 — Local Video Pipeline Implementation

Status: **CODE / CI PASS; REAL-DEVICE SMOKE PENDING**  
Implementation date: **2026-09-27**  
Final code verification run: **36308774838**

## 1. Outcome

A selected video can now become either:

```text
READY_WITHOUT_ENCODING
ENCODED_READY
```

without any R2 credential or upload endpoint.

## 2. Durable job model

Room `VideoJob` stores source URI/fingerprint/metadata, encode requirement, encoded local URI, separate encoding/upload states and progress, future upload-session fields, errors, and timestamps. Supabase remains the catalog source of truth.

## 3. Selection and inspection

The Episode editor uses the system video picker, persists source read access, then uses Media3 Inspector. The pipeline records duration, dimensions, rotation, frame rate, video/audio MIME types, audio details when available, and file size before creating the durable job.

## 4. Compatibility

Streaming baseline:

```text
MP4
H.264 / AVC
AAC when audio exists
short side <= 1080
long side <= 1920
frame rate <= 30 fps
AAC encode target 128 kbps
```

The dimension limit is orientation independent. Existing AAC above 128 kbps does not force re-encode because 128 kbps is an encode target, not a source acceptance ceiling. Unknown resolution/FPS is treated conservatively as needing preparation.

## 5. Output planning

`VideoOutputPlanner` preserves aspect ratio and rotation. Examples:

```text
1080×1920 -> unchanged
1920×1080 -> unchanged
2160×3840 -> 1080×1920
2160×900  -> 1920×800
```

No forced 9:16 stretch is used.

## 6. Media3 encoding

`Media3VideoEncoder` exports H.264/AAC, requests AAC 128 kbps, caps frame rate at 30 fps, adds a Presentation downscale only when required, polls progress, cancels Transformer on coroutine cancellation, and removes partial output on failure/cancel.

Transformer control calls stay on one application looper. The app does not implement blocking codec work on the UI thread; Media3 owns the codec pipeline.

## 7. Storage and retention

Encoded output lives in app-owned no-backup storage. Before encode, Studio checks estimated source size plus reserved headroom.

Partial output is deleted on retry, failure, cancellation, and interrupted recovery. READY output is retained for Phase 6 upload/retry. There is no blind age-only cleanup.

## 8. Errors and recovery

Encoding distinguishes:

- job not found;
- encoding not required;
- insufficient storage;
- unsupported codec;
- generic export failure.

If the process dies while `ENCODING`, the restored job returns to `QUEUED` and partial output is removed. If Room says `READY` but the output file is missing, it also returns to `QUEUED`.

Recovery is applied only to the initially restored job so live ENCODING updates are not reset by their own observer.

## 9. Episode editor

Local video is a separate panel from legacy YouTube/Facebook fields. It shows source metadata, expected Studio output, Select/Replace, Prepare, progress, Cancel, READY, and error states.

A new Episode must first be saved to obtain a stable episode ID. Phase 5 never mutates `episodes.video_url` from local-video completion.

## 10. Verification

Automated tests cover Room mapping, compatibility, portrait/landscape/4K/ultra-wide planning, source preparation, skip-encode, progress persistence, storage guard, cancellation, unsupported codec classification, interrupted/missing-output recovery, and local-video ViewModel restoration.

Real hardware smoke remains required for representative H.264, HEVC, portrait, landscape, 4K, silent-video, cancellation, and hardware codec behavior.
