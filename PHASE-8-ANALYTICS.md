# Phase 8 — Analytics Implementation

Status: **CODE / CI PASS; PRODUCTION DATA SMOKE PENDING**  
Implementation date: **2026-09-27**  
Final verification run: **36326340217**

## 1. Scope

Ceritaria Studio now exposes read-only operational analytics from the existing Ceritaria first-party analytics backend.

Android does not select from `analytics_events` directly. It calls the existing admin-only aggregate RPC:

```text
get_analytics_dashboard(
  p_days,
  p_timezone
)
```

with `Asia/Jakarta`, matching the web admin service.

## 2. Data contract

The RPC payload maps into:

- summary;
- hourly;
- top pages;
- devices;
- referrers;
- events.

Summary contains:

- today pageviews;
- today unique visitors;
- period pageviews;
- period unique visitors;
- period sessions;
- total events.

The Android domain model uses `Long` counters so aggregate counts are not artificially constrained to `Int`.

## 3. Range behavior

Supported ranges are exactly:

```text
7 days
30 days
90 days
```

These mirror the existing web admin choices.

Changing range cancels the previous load job and clears the stale report while the new range loads.

## 4. UI

The Analytics bottom-navigation destination is no longer a placeholder.

The screen provides:

- compact summary metrics;
- 7/30/90 day chips;
- horizontally scrollable 24-hour traffic strip;
- top pages;
- device distribution;
- referrers;
- player interaction events;
- pull-to-refresh.

No chart dependency was added.

The backend does not currently return dedicated top-series or top-episode rankings, so Studio renders top pages rather than synthesizing unsupported analytics.

## 5. Player events

The aggregate RPC contains all event counts. The Android player-interaction section excludes generic `page_view` and `episode_view` rows and surfaces interaction events such as play intent, next/previous navigation, and fallback clicks when present.

## 6. Loading and errors

State is explicit:

- initial loading;
- content;
- empty;
- initial failure with retry;
- refresh failure while preserving the last successful report.

A failed refresh does not blank a valid report.

## 7. Data boundary

Feature ownership is split across:

```text
feature/analytics/domain/
feature/analytics/data/
feature/analytics/presentation/
```

The Compose screen never calls Supabase directly.

The repository returns app-level results and does not expose raw PostgREST errors to UI.

## 8. Verification

Automated coverage includes:

- RPC DTO/domain mapping;
- initial ViewModel load;
- range changes;
- stale request cancellation behavior;
- refresh failure preserving previous content.

Final Android CI run `36326340217` passed lint, unit tests, and debug assembly.

## 9. Production gate

A configured admin smoke test remains required to compare Android and web admin reports for the same range and timezone.

The existing backend/RLS contract remains authoritative: analytics reporting is admin-only and raw event history is not part of the Android report flow.
