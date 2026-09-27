# Phase 4 — Image Media Implementation

Status: **CODE / CI PASS; PRODUCTION SMOKE PENDING**  
Implementation date: **2026-09-27**  
Final verification run: **36305633832**

## 1. Scope

Ceritaria Studio now manages the existing production image references for:

- Series cover;
- Series hero;
- Episode thumbnail.

No new production table or media field was introduced.

## 2. Production contract

The implementation uses the Phase 0 verified buckets:

```text
series-media
episode-media
```

Allowed MIME types are `image/jpeg`, `image/png`, and `image/webp`. The production object limit is 5 MiB.

Database references remain the existing public URL columns:

```text
series.cover_url
series.hero_url
episodes.thumbnail_url
```

## 3. Selection and preparation

The editor uses Android's system Photo Picker with image-only selection and does not request broad gallery/storage permission.

Before background work, Studio attempts to persist read access to the selected URI. `AndroidImageSourceReader` then:

1. reads MIME and declared size;
2. validates the production MIME set;
3. rejects declared files above 5 MiB;
4. copies the source to app cache through a bounded streaming buffer;
5. enforces the same 5 MiB limit during copy when metadata is absent or wrong;
6. returns a prepared file plus normalized metadata.

The full image is not loaded into a giant in-memory byte array.

## 4. Durable work

Image replacement/removal uses WorkManager with Hilt worker injection.

Replacement requires network connectivity and exposes queued, running, success, failure, retry, and cancel states.

Cancellation exceptions are propagated and are not converted into automatic retry.

## 5. Progress

Supabase Storage upload uses its file-based progress flow.

Worker progress is mapped as:

```text
10%      source preparation starts
25..90%  byte-based Storage upload progress
100%     DB media reference finalized
```

Reaching Storage does not mean the UI reports completion before the database reference succeeds.

## 6. Replacement safety

Replacement is ordered as:

```text
upload new object
 -> update exactly one DB media column
 -> cleanup old owned object
```

Failure behavior:

- upload failure leaves the previous DB reference untouched;
- DB update failure attempts to delete the newly uploaded object;
- cleanup runs only after the new reference is committed;
- cleanup failure is tolerated after a valid DB update, preferring a possible orphan object over a broken published reference.

## 7. Cleanup scope

Old-object deletion is deliberately conservative.

A URL is deletable only when:

- scheme is HTTPS;
- host matches the configured Supabase project;
- path matches the expected public Storage bucket;
- a non-empty object path exists after the bucket prefix.

An external or legacy manual URL is therefore never interpreted as an internal object to delete.

## 8. Editor integration

Artwork UI provides preview, choose/replace, remove, progress, retry, and cancel.

New Series/Episode records must be saved once before upload becomes active so the object and DB reference have a stable owner ID.

Media work mutates only its media reference column. It does not save unrelated title/story edits.

After a successful media operation, the editor synchronizes the media URL into both its current form and initial form so background media completion does not create a false dirty-form warning.

The obsolete manual Series media-URL section was removed once Photo Picker became authoritative.

## 9. Tests

Phase 4 tests cover:

- allowed MIME types;
- 5 MiB boundary;
- unsupported types;
- unknown declared size;
- safe owned-object URL parsing;
- wrong-bucket/external-host rejection;
- replacement operation ordering;
- rollback on DB-reference failure;
- remove ordering;
- cancellation propagation;
- unsaved-owner guard;
- successful media-reference effects.

## 10. File ownership

Phase 4 remains split across:

```text
feature/media/domain/
feature/media/source/
feature/media/data/
feature/media/work/
feature/media/presentation/
```

Series/Episode editor ViewModels only synchronize completed media references. They do not own Storage SDK calls, URI streaming, or WorkManager scheduling.

## 11. Production gate

Code/build gate is **PASS**.

Production completion still requires an authorized admin using the configured APK to verify:

1. cover upload;
2. hero upload;
3. thumbnail upload;
4. replace;
5. remove;
6. retry after network interruption;
7. cancel;
8. resulting artwork in Ceritaria web/PWA;
9. unauthorized mutation rejection through RLS.

No service-role key or Storage secret is present in the APK contract.
