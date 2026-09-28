# Release & Rollback Operations

## Release secrets

Configure these only in GitHub Actions secrets:

- `CERITARIA_SUPABASE_URL`
- `CERITARIA_SUPABASE_PUBLISHABLE_KEY`
- `CERITARIA_API_BASE_URL`
- `CERITARIA_ANDROID_KEYSTORE_BASE64`
- `CERITARIA_ANDROID_STORE_PASSWORD`
- `CERITARIA_ANDROID_KEY_ALIAS`
- `CERITARIA_ANDROID_KEY_PASSWORD`

Never add a Supabase service-role key, R2 access key, or R2 secret key to the Android workflow or APK.

## Build a candidate

Run **Android Release Candidate** manually with a monotonically increasing `version_code` and the desired `version_name`.

The release workflow runs the same source/security guardrails, validates required environment/version inputs, requires HTTPS production endpoints, decodes the signing keystore only into the temporary runner directory, runs lint/unit tests, builds signed APK/AAB, verifies the APK/AAB signatures, generates SHA-256 checksums, and uploads APK + AAB + checksum as a GitHub Actions artifact for 30 days.

Minification is intentionally disabled for the first internal candidate. Enable it only after a separate shrinker compatibility pass.

After downloading the artifact, compare the packaged files against `SHA256SUMS.txt` before distribution. Keep the checksum together with the candidate record.

## Smoke order

1. install/launch and restore/login;
2. read Series/Episode/Analytics;
3. edit and save a controlled draft;
4. image upload/replace/remove on controlled content;
5. local video inspect/encode;
6. R2 upload and preview;
7. only after preview succeeds, attach to a controlled episode;
8. verify web/PWA reads the same content.

Do not use a business-critical published episode for the first attachment smoke.

## App rollback

Keep the last known-good commit and signed artifact.

For distributed installs, do not depend on Android accepting a lower `versionCode`. Rebuild the last-known-good source with a **higher** versionCode and distribute that signed build.

For one local test device, uninstalling the candidate before reinstalling an older artifact is acceptable if local Studio state may be discarded.

## Content rollback limitation

Attaching a new R2 asset may mark the previous R2 asset `REPLACED` and the backend currently attempts best-effort deletion of that object. Previous R2 attachment is therefore not an automatic rollback path.

Before controlled attachment testing:

- preview the new READY asset;
- record the episode's current provider/source;
- use a controlled episode;
- for YouTube/Facebook, keep the previous URL available for manual restoration.

A retained-asset restore workflow should be designed separately if instant R2-to-R2 rollback becomes a product requirement.

## Schema rollback

Video migrations are additive and already consumed by web and Android code. Do not blindly down-migrate production during an app rollback. Prefer rolling client/backend code forward from the last-known-good commit while preserving compatible schema.
