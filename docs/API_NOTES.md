# MangaUpdates API notes

Last verified: 2026-07-13

## Sources

Primary contract: repository openapi.json, OpenAPI 3.0.0, API version 1.0.0,
SHA-256 cb0fe8290ec7a8f1179f92a62d0be5f5602ac15feca343da6c35bceb269f30aa.

The official documentation URL is https://api.mangaupdates.com/. Two read-only
production requests were made during discovery solely to verify public search
and series-detail shapes. No authenticated or mutating request was made.

Context7 is intentionally not an API-contract source for this document. It was
used to audit Retrofit, OkHttp, serialization, Room, and Paging behavior, as
recorded in DEPENDENCIES.md, but no MangaUpdates endpoint, field, enum, or
authentication behavior is inferred from third-party library documentation.

## Implementation checkpoint

DTOs, a Retrofit service, remote-source error mapping, persistence mappings, and
repository code exist only for POST /series/search and GET /series/{id}. The
code compiles in debug and minified release variants and is covered by realistic
DTO, mapping, MockWebServer, Room, repository, mediator, and ViewModel tests. No
authenticated, list, mutation, or release operation is implemented. No
production request beyond the two read-only discovery checks described here has
been made. PROJECT_STATE.md records the exact state and commands.

## Service and acceptable use

- Confirmed base URL: https://api.mangaupdates.com/v1
- Media type used by the documented operations: application/json
- MangaUpdates requires credit when its data is used.
- Clients must space requests reasonably and use caching.
- The service provides no availability or stability warranty.

Kiroku identifies itself as unofficial, credits MangaUpdates, debounces search,
persists responses in Room, and does not prefetch unrelated records.

## Authentication

The contract declares HTTP bearer authentication with bearer format JWT.
Authenticated endpoints use:

    Authorization: Bearer <token>

Confirmed account operations:

| Operation | Method and path | Authentication | Contract result |
| --- | --- | --- | --- |
| Login | PUT /account/login | Username/password request | ApiResponseV1 |
| Logout | POST /account/logout | Bearer JWT | ApiResponseV1 |
| Current profile | GET /account/profile | Bearer JWT | UserModelV1 |
| Cookie refresh | GET /account/refresh | Described as cookie refresh | ApiResponseV1 |

Login requires exactly username and password according to AccountLoginModelV1.
The password is sent only in that JSON request.

Blocking unknown: ApiResponseV1 defines status, reason, and an unrestricted
context object. It does not define the context member containing the JWT, its
expiry, or required session metadata. The refresh operation is explicitly
cookie-oriented and cannot be assumed to refresh bearer tokens. Authentication
implementation must wait for a sanitized successful response example or an
updated official schema.

The root OpenAPI document applies bearer security globally, but public search
and detail operations omit a local security override. Both worked without a
token in live verification, matching the contract description that most
functions are public. This is a documentation inconsistency; Kiroku sends no
fabricated bearer value on public requests.

## Confirmed product endpoints

Only these documented routes are planned:

| Journey | Method and path | Key request/response |
| --- | --- | --- |
| Series search | POST /series/search | SeriesSearchRequestV1 / SeriesSearchResponseV1 |
| Series detail | GET /series/{id} | SeriesModelV1 |
| User lists | GET /lists | array of ListsModelV1 |
| List metadata | GET /lists/{id} | ListsModelV1 |
| Search a list | POST /lists/{id}/search | ListsSearchRequestV1 / ListsSearchResponseV1 |
| Series list state | GET /lists/series/{series_id} | ListsSeriesModelV1 |
| Add to list | POST /lists/series | array of ListsSeriesModelUpdateV1 |
| Update/move/progress | POST /lists/series/update | array of ListsSeriesModelUpdateV1 |
| Remove from list | POST /lists/series/delete | array of integer series IDs |
| Recent releases | GET /releases/days | ReleaseSearchResponseV1 |
| Search releases | POST /releases/search | ReleaseSearchRequestV1 / ReleaseSearchResponseV1 |

The first vertical slice implements only the first two routes.

## Series search

Confirmed request fields include search, stype, licensed, type, year,
filter_types, category, pubname, filters, list, page, perpage, letter, genre,
exclude_genre, orderby, pending, include_rank_metadata, and
exclude_filtered_genres. The deprecated singular filter field is documented but
Kiroku will use filters.

Confirmed stype values: title and description.

Confirmed filter values: scanlated, completed, oneshots, no_oneshots,
some_releases, and no_releases.

The response contains total_hits, page, per_page, and results. Each result may
contain record, hit_title, and authenticated-user metadata.

Observed behavior for a title search:

- request page 1 and perpage 2 returned page 1 and per_page 25;
- 25 results were returned;
- total_hits was 10,000;
- series ID 55,099,564,912 was returned, which exceeds Int.MAX_VALUE.

The contract does not state allowed perpage bounds or whether 10,000 is a hard
cap. Kiroku therefore requests the confirmed 25-item page size, stores IDs as
Long, and considers pagination complete only when the returned page/per_page
and total_hits indicate completion or a returned page is empty.

## Series detail

GET /series/{id} supports optional unrenderedFields. Kiroku does not request
unrendered editing fields.

Confirmed useful fields include series_id, title, url, associated, description,
image, type, year, bayesian_rating, rating_votes, genres, categories,
latest_chapter, status, licensed, completed, authors, artists represented by the
author type field, publishers, related series, recommendations, rank, and
last_updated.

Descriptions and status values can contain Markdown and line breaks. Categories
can contain hundreds of entries. The UI preserves all cached categories but
collapses the initial presentation.

## Nullability and inconsistencies

Most top-level series schema properties are not marked required. Live responses
also showed explicit null values where the OpenAPI property only declared a
non-null primitive:

- record.description can be null or an empty string;
- bayesian_rating can be null;
- image.url.original and image.url.thumb can be null;
- image height and width can be null;
- last_updated.as_rfc3339 and as_string can be null while timestamp is zero;
- year can be an empty string;
- optional properties such as latest_chapter may be absent.

DTOs model these confirmed optional or null values safely. Domain mapping
requires a positive series_id and non-blank title; invalid records fail mapping
instead of receiving invented values. Response type values remain strings so a
new server value does not become a serialization failure.

## Errors

ApiResponseV1 requires:

- status: string
- reason: string
- context: optional arbitrary object, optionally shaped as validation errors

Validation context is a map from field names to arrays containing an optional
index and string error list. Several 400 responses use this envelope. Some 401,
403, 404, and 412 responses have no documented body.

The contract documents no 429 response on any operation and no Retry-After
format. Kiroku still maps an actual HTTP 429 generically and honors a standard
numeric Retry-After header if supplied. Host lookup failures, refused
connections, and other non-timeout I/O connectivity failures map to Offline;
socket/read/call timeouts map to Timeout. MockWebServer and deterministic fake
service tests verify these cases without calling production.

## Rate limits and mutation delay

No numeric request quota, rate-limit headers, or reset policy is documented.
The acceptable-use policy only requires reasonable spacing and caching.

Many list mutations document HTTP 412 as a five-second update delay. Phase 3
must serialize relevant mutations, avoid immediate retries inside that window,
and preserve the outbox item. This is not treated as an ordinary permanent
precondition failure.

## Unknowns requiring confirmation

- Login context token and expiry field names.
- Whether bearer token refresh exists separately from cookie refresh.
- Numeric rate limits or official Retry-After behavior.
- Exact supported perpage bounds and whether total_hits is capped at 10,000.
- Cache validators or freshness headers suitable for detail reconciliation.
- Idempotency-key support for mutation endpoints; none is documented.

No endpoint or field depending on these unknowns will be guessed.
