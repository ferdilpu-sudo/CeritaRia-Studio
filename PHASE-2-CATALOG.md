# Phase 2 — Read-only Catalog

Status: **IMPLEMENTED / CI VERIFIED / PRODUCTION CATALOG SMOKE PENDING**  
Implementation head verified: `e30097a81dcf21cd9bff4d627e4a93b2a8288b43`  
CI run: `36297938559`

## Implemented

### Series

- production-compatible `SeriesRowDto`;
- DTO-to-domain mapper;
- repository and data-source boundaries;
- Supabase read adapter;
- range pagination with one-row lookahead;
- 300 ms debounced search;
- published, draft, and featured filters;
- loading/content/empty/error states;
- pull-to-refresh;
- network cover artwork with fallback;
- series detail;
- navigation from series detail to its filtered episode list.

### Episodes

- production-compatible `EpisodeRowDto`;
- nested parent-series read;
- DTO-to-domain mapper;
- repository and data-source boundaries;
- Supabase read adapter;
- range pagination with one-row lookahead;
- 300 ms debounced search;
- series/status/provider filters;
- YouTube and Facebook provider mapping;
- safe `Unknown(rawValue)` provider handling;
- loading/content/empty/error states;
- pull-to-refresh;
- network thumbnail artwork with fallback;
- episode detail.

## State semantics

Publish state is derived from the verified production fields:

```text
deleted_at != null
  -> DELETED

is_published = true
  -> PUBLISHED

is_published = false && published_at != null
  -> UNPUBLISHED

otherwise
  -> DRAFT
```

No Android-only production fields were invented.

## Pagination

Lists request `pageSize + 1` rows from Supabase. The extra row is used only to determine `hasMore`; it is not shown to the user.

This keeps pagination backend-driven and avoids loading the complete catalog into memory.

## Image loading

Catalog artwork uses Coil through one shared `RemoteArtwork` component.

The project intentionally pins Coil `3.3.0` for the current Kotlin 2.2 toolchain. A newer Coil release pulled Kotlin 2.4 metadata and failed compilation under the current compiler, so the dependency was kept on the compatible stable line instead of forcing an unrelated compiler/AGP migration.

## Tests

Coverage added for:

- publish-state mapping;
- series DTO mapping;
- episode DTO mapping;
- video-provider parsing and unknown-provider preservation;
- series repository pagination/error mapping;
- episode repository pagination/error mapping;
- series initial-load state;
- series search debounce;
- episode initial parent-series filter from navigation;
- episode provider-filter reload behavior.

## CI evidence

GitHub Actions run `36297938559` passed:

```text
lintDebug
testDebugUnitTest
assembleDebug
```

## File-size / responsibility audit

Representative changed files remain below the project limits:

- `SeriesListScreen.kt`: 38 lines;
- `EpisodeListScreen.kt`: 40 lines;
- `EpisodeListViewModel.kt`: 146 lines;
- `SupabaseSeriesDataSource.kt`: 60 lines;
- `SupabaseEpisodeDataSource.kt`: 86 lines;
- repository implementations: about 40 lines each.

UI, repository, Supabase adapter, mapping, and navigation responsibilities remain separate.

## Pending production smoke gate

CI cannot prove real production RLS/data compatibility without the configured Supabase public client values.

Before marking Phase 2 fully complete, verify with a real authorized admin:

1. login/session gate succeeds;
2. series list reflects production records;
3. series search/filter/pagination behave correctly;
4. series detail fields and artwork resolve correctly;
5. `Series -> Episodes` opens the correct filtered list;
6. episode search/filter/pagination behave correctly;
7. episode detail/provider/status values match production;
8. no read-only flow mutates production data.

Phase 3 mutations should not use a green production-complete assumption until this smoke gate has passed.
