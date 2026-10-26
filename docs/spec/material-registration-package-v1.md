# Material contribution and physical package v1

Contract date: 2026-10-21
Owner: TKL-M03
Status: authored library contract; validation and connected logical registration remain pending

This contract specializes [Material access and processing](material-access-and-processing.md)
for a neutral contribution and a create-only physical package. TKL owns package
identity, format and placement. Source adapters own interpretation of their
source payloads and must authorize the selected source before invoking the
writer. A source reference, contribution, plan, principal or physical storage
confirmation grants no access.

The public Material Operations and read-only reference contract in
[Material reference v1](material-reference-v1.md) are unchanged. This library
adds no CML, HTTP, SPI or MCP selector. A confirmed physical package is not
logical Material registration, an index entry, accepted Material or a completed
M03/ESS3 delivery. Those connected obligations remain open.

## Contribution admission

`MaterialContribution.parseC(Json)` is the sole public constructor of the final
immutable contribution. It returns a structured `argumentInvalid` with fixed
message `Invalid Material contribution` for malformed input. It does not echo
opaque source, provenance or body content. Validated declarations have
instance-independent ownership and no case-class `copy` admission bypass.
Their diagnostic displays are redacted.

The external object contains exactly these seven keys:

| Key | Required value |
| --- | --- |
| `schema` | `textus.material-contribution/1` |
| `title` | Nonblank string, retaining literal spelling |
| `createdAt` | Nonblank string, without date parsing |
| `classifications` | Ordered vector of nonblank strings; duplicates remain literal |
| `provenance` | Complete opaque JSON object |
| `originals` | Ordered vector of original objects |
| `documents` | Ordered vector of document objects |

An original object contains exactly `resourceId`, `role`, `mediaType`,
`byteLength`, `sourceReference`. IDs match
`[A-Za-z0-9][A-Za-z0-9_-]{0,127}`. Role is a nonblank string. MIME has the existing
type/subtype token grammar, with no parameters; accepted capitalization is
retained. `byteLength` is a known nonnegative Long, including zero.
`sourceReference` must be an object and remains a complete opaque JSON value
owned by its originating source adapter. TKL does not interpret it as a URL,
FileStoreRef, access grant or output destination.

A document object contains exactly `resourceId`, `role`, `body`, `derivedFrom`.
ID and role follow the same rules as originals. `body` is any JSON value,
including null, scalar or an arbitrary complete tree. `derivedFrom` is an
ordered vector of distinct original resource IDs from this same contribution;
it cannot refer to another document. Every document is `application/json` in
v1. IDs are unique across both originals and documents.

Empty originals and documents are valid. TKL imposes no additional schema on
opaque source/provenance/body contents. Admission preserves Unicode code units,
whitespace in selected strings, JSON trees, array order, nulls and empty or
singleton arrays. It does not trim, casefold, normalize text, parse dates or
reinterpret a producer's provenance path.

## Location and pure planning

`MaterialLocation.createC(lake, logicalPath)` privately constructs a validated
location. Lake matches `[A-Za-z0-9][A-Za-z0-9._-]{0,127}`. Logical path is nonempty,
at most 1024 characters and consists of slash-separated components matching
`[A-Za-z0-9][A-Za-z0-9._-]*`. Empty components, dot/dotdot, slash boundaries,
backslash, escapes, query/fragment syntax and whitespace are rejected. Invalid
locations return fixed `argumentInvalid("Invalid TKL material location")`.
Accepted spelling is retained exactly.

The domain ID is exactly `urn:textus:klake:<lake>:material:<logicalPath>`.
There is no default production lake/path, native Entity ID conversion or
inference from client/provider data. Native Entity IDs, package URNs and legacy
App request/subject provenance remain distinct identities.

`MaterialPackage.planC(contribution, location, owner, createdBy)` performs no IO.
The two `PrincipalId` values are explicit caller/server selections, nonblank
and free of ISO control characters; client metadata cannot supply authority.
Missing input or malformed principals return fixed
`argumentInvalid("Invalid Material package request")`.

The plan retains the admitted contribution, selected location, manifest,
ordered originals and serialized documents. Its manifest contains exactly
`schema`, `id`, `title`, `createdAt`, `classifications`, `provenance`, `owner`,
`createdBy`, `assets`. Schema is `textus.material/1`; ID is the location URN.
Other metadata is retained literally from the contribution or explicit server
principals. No component or Book-specific field is added.

TKL selects these paths:

| Resource | Path and manifest entry |
| --- | --- |
| Each original, in original order | `assets/<resourceId>`; exact role and MIME |
| Each document, in document order after originals | `derived/<resourceId>.json`; exact role, `application/json`, ordered `derivedFrom` mapped to `assets/<originalId>` |

Each asset contains `path`, `role`, `mediaType`, plus `derivedFrom` for a
document. Opaque source references remain in the retained contribution and
are not exposed in manifest assets. Explicit producer provenance is retained
without an inferred replacement path. Empty resources emit `assets: []`.

Document and manifest serialization uses Circe
`Printer.noSpaces.copy(escapeNonAscii = true)`, then explicit UTF8 bytes in
known-length materialized Bags. This preserves arbitrary UTF16, including
unpaired surrogates, through ordinary JSON storage/parsing. Tree equality is
promised; original JSON byte formatting is not. Original binary octets never
pass through text encoding.

## Create-only physical storage

`MaterialPackageWriter.createC(store, lake, logicalParent, parent)` validates a
literal operator/native-caller mapping. Store and store name are nonnull;
name is nonempty and free of ISO controls. Lake follows the location grammar.
Logical parent is either empty for the lake root or a valid logical path.
The physical parent identifies exactly the configured store; its opaque key
is nonempty, at most 4096 characters and free of ISO controls. Slash and Unicode
physical keys remain literal. Invalid configuration returns fixed
`argumentInvalid("Invalid Material package destination")`; displays redact
physical configuration.

`materialize(contribution, location, owner, createdBy, originals)` constructs an
`ExecUowM[StoredMaterialPackage]` without provider IO. The supplied original
source must be nonnull. Output location must use the exact configured lake and
be exactly one final leaf below the configured logical parent. An empty parent
permits one root leaf only. Invalid requests stop before any provider program
with fixed `argumentInvalid("Invalid Material package request")`.

The writer uses existing public native FileStore programs in this order:

1. Create a fresh package collection under the configured physical parent,
   using the last logical path component as its display label.
2. Create `assets` only when originals exist. For each original in order,
   invoke `MaterialOriginalSource.read(original)` exactly once and create its
   destination file exactly once.
3. Create `derived`, including for an empty document vector, then create each
   complete derived JSON document in order.
4. Create `material.json` last. Only its valid final confirmation yields a
   `StoredMaterialPackage`.

`MaterialOriginalSource` is a capability, not authorization. Its owning source
adapter/native Operation must authorize source access before using this helper.
`MaterialOriginalContent` returns the exact selected original declaration and
a borrowed nonnull materialized Bag, with a non-URL backend and known exact
declared byte length. A changed reference, URL Bag or length mismatch returns
fixed `resourceInvalid("Original material source is inconsistent")` before
that destination original is submitted. The writer borrows the Bag unchanged;
it neither opens nor closes streams. The provider retains its ordinary
per-open stream close contract.

Every successful create must confirm the configured store, valid nonempty
opaque key, expected parent, exact display label and kind. Files also confirm
exact MIME and known exact length. No returned reference may equal the
configured parent or any previous create reference in that attempt. Malformed
successful metadata returns fixed
`resourceInvalid("Material package storage confirmation is invalid")` and
stops before another provider action.

`StoredMaterialPackage` retains its plan and owning-TKL-private FileStore
confirmations for later registration. Its file vector contains originals then
documents, excluding the final manifest. These are internal physical receipts,
not public Operation DTOs or domain IDs. The helper performs no native Material
database, index or registration mutation.

## Failure and ambiguous outcomes

Native FileStore/source failure Conclusions pass through unchanged. A failed
submission may already have retained remote bytes, including the final
manifest after a write-then-error outcome. Failure yields no confirmed stored
package or accepted Material. It does not prove that nothing was written.

The writer performs no replay, retry, discovery/stat-by-label, overwrite,
rollback, delete, hash registry, competing-writer exclusion, attempt lease,
revision or outbox protocol. Fresh references can share a display label.
Caller/operator reconciliation must resolve an uncertain write before another
attempt is considered; this helper gives no logical uniqueness or idempotence
promise. TKL logical registration must separately define the same-reference
relationship under the existing M03 ownership boundary.

## Paired executable specifications

[MaterialContributionSpec](../../src/test/scala/org/simplemodeling/textus/knowledgelake/material/MaterialContributionSpec.scala)
documents closed-shape admission, literal retention, resource/lineage rejection,
location grammar and pure generic package planning.
[MaterialPackageWriterSpec](../../src/test/scala/org/simplemodeling/textus/knowledgelake/material/MaterialPackageWriterSpec.scala)
authors native interpreter/GoogleDriveFileStore scenarios using the current TKL
Component identity, synthetic credentials, native HTTP grants and counted
test-owned physical files. Readback occurs after the physical peer stops and
does not depend on an in-memory expected-byte echo. Both suites include bounded
ScalaCheck properties requiring at least 100 successful samples and zero
discards; storage samples include arbitrary UTF16 and nonempty binary payloads.

These are authored specifications, not recorded passing results, production
Google-account evidence, public registration acceptance or Phase closure.
