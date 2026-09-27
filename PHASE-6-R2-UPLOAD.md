# Phase 6 — Secure R2 Upload

Status: **CODE / CI PASS; PRODUCTION R2 SMOKE PENDING**  
Updated: **2026-09-27**  
Final Android verification: **36318085202**  
Backend verification: **36318207100**

## 1. Outcome

Ceritaria Studio can now take a locally prepared video and drive it to a server-verified R2 asset with status READY without embedding privileged R2 credentials in Android.

Phase 6 stops at:

```text
video_assets.status = READY
VideoJob.uploadStatus = READY
VideoJob.remoteAssetId = <asset id>
```

It does not replace the episode's production video.

## 2. Trusted server boundary

Android authenticates to Ceritaria with the current Supabase access token.

The server validates the authenticated user and admin status, then performs privileged R2 operations.

Permanent credentials remain server-only:

```text
R2_ACCESS_KEY_ID
R2_SECRET_ACCESS_KEY
```

Android receives only short-lived upload authorizations.

## 3. Server data model

Phase 6 adds:

```text
video_assets
video_upload_sessions
```

`video_assets` owns canonical uploaded-object state.

`video_upload_sessions` owns the upload mode, remote multipart ID, part layout, expiry, and lifecycle state.

Existing episode provider fields remain untouched in Phase 6.

## 4. Server API

Implemented endpoints:

```text
POST /api/video-uploads
GET  /api/video-uploads/{sessionId}
POST /api/video-uploads/{sessionId}/parts
POST /api/video-uploads/{sessionId}/complete
POST /api/video-uploads/{sessionId}/finalize
POST /api/video-uploads/{sessionId}/cancel
```

The server chooses SINGLE vs MULTIPART from its configured threshold.

## 5. Upload modes

### Single

```text
create session
 -> direct presigned PUT
 -> finalize
 -> server HEAD verify
 -> READY
```

### Multipart

```text
create session
 -> authorize missing part
 -> stream exact byte range
 -> save ETag
 -> repeat
 -> complete multipart
 -> finalize
 -> server HEAD verify
 -> READY
```

Completed ETags are persisted locally so a retry can skip completed parts.

## 6. Android ownership

Main responsibilities are split across:

```text
VideoUploadApi
R2UploadDataSource
VideoUploadRepository
VideoUploadTransferRunner
VideoUploadRecoveryResolver
VideoUploadScheduler
Room VideoJob
```

The UI does not sign R2 requests, call R2 account APIs, or own multipart state.

## 7. Source streaming

Upload reads either:

- the original compatible content URI; or
- the app-owned encoded MP4.

The source reader supports byte offsets for multipart transfer and does not load the complete video into memory.

## 8. Recovery

When an old upload session exists, Android asks the server for current state.

It can then:

- adopt an already READY asset;
- resume missing multipart parts;
- complete/finalize a multipart session that already transferred data;
- restart with a new session when the previous session is expired/terminal/unusable.

Single PUT partial transfer is not treated as resumable multipart state.

## 9. Execution lifecycle

### API 34+

Upload is scheduled as a User-Initiated Data Transfer Job using JobScheduler.

### Older supported Android

Upload uses foreground WorkManager with network connectivity required and the dataSync foreground-service type.

Both paths invoke the same repository and persist durable progress into Room.

## 10. Editor UX

Once a local source is ready, the Episode editor exposes:

- Upload video;
- queued state;
- determinate upload progress;
- VERIFYING;
- READY asset ID;
- Retry upload after failure/cancel;
- Cancel upload.

Source replacement is locked while a transfer/session is active or READY so Phase 6 does not accidentally create unmanaged replacement assets.

## 11. Local output cleanup

For sources that required encoding, the app-owned encoded MP4 remains available throughout upload and retry.

It is deleted only after the server has verified the remote asset as READY. The Room job then clears `encodedLocalUri`.

Compatible original content URIs are not deleted.

## 12. Security properties

- no R2 access key/secret in Android;
- server-generated object keys;
- admin bearer authorization;
- short-lived operation URLs;
- server-controlled size/part policy;
- server-side object verification before READY;
- invalid object cannot silently become READY;
- upload success does not publish;
- upload success does not replace existing episode video.

## 13. Production gate

The code and CI gates are complete.

Still required against real production infrastructure:

- migration application;
- R2 env configuration;
- single upload smoke;
- multipart upload smoke;
- network interruption/resume;
- cancellation;
- expired session recovery;
- unauthorized request rejection;
- verification-failure path;
- API 34+ UIDT device test;
- pre-34 foreground WorkManager device test.

Phase 7 consumes `remoteAssetId` and defines atomic attachment + playback delivery.
