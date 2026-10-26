# Native Material registration lifecycle v1

Contract date: 2026-10-22
Owner: TKL-M03
Status: native kernel and explicit public metadata view authored; connected acceptance pending

This contract extends [Material contribution and physical package v1](material-registration-package-v1.md)
with owning-TKL native persistence. A confirmed physical package and a native
registration are distinct facts. The native registration ID is issued by Entity;
the logical Material reference remains exactly
`urn:textus:klake:<lake>:material:<logicalPath>`. Neither identity is a legacy
App/Book receipt or an inferred client identifier.

This kernel adds no public CML, ABI, HTTP or MCP Operation. Public registration
Operation names and Workbench/Studio consumer wiring remain unimplemented.
Native search projections and protected owner-registration queries are authored
below. Existing generated Material search/get Operations consume them only under
the explicit `registered` reference mode in the
[reference contract](material-reference-v1.md#explicit-reference-mode).
This adds no public M03 registration availability and does not replace the
default read-only bootstrap Material reference path.

## Internal document

`MaterialRegistrationDocument` is immutable and private to the owning TKL
package. Its exact schema is `textus.material-registration/1`, with these fields:

| Key | Meaning |
| --- | --- |
| `schema` | Exact schema above |
| `contribution` | Complete admitted `MaterialContribution.json`, without projection or normalization |
| `lake` | Existing validated `MaterialLocation.lake` |
| `logicalPath` | Existing validated `MaterialLocation.logicalPath` |
| `storageBinding` | Nonsecret host-selected identifier matching `[A-Za-z0-9][A-Za-z0-9._-]{0,127}` |
| `owner` | Actual authenticated PrincipalId string selected by the native caller |
| `createdBy` | Actual authenticated PrincipalId string selected by the native caller |
| `status` | Exactly `prepared`, `materializing` or `registered` |
| `stored` | Present only for `registered`, with confirmed physical mappings |

Storage binding is not a credential, URL, folder, source grant or client-owned
destination. No time, date interpretation, identity inference or text
normalization is introduced. Principals obey existing package principal
admission: nonblank strings free of ISO control characters, retaining spelling.
Document, record, draft and physical receipt diagnostic displays are redacted.

Only Prepared may begin, producing Materializing without `stored`. Only
Materializing may complete, and completion requires a confirmed
`StoredMaterialPackage` whose plan has the exact contribution, location and
principal-attributed manifest. Completion produces Registered with its saved
mapping. There is no backward transition, reset or retry transition.

State conflicts use structured `operationConflict` for `materializeMaterial`
with fixed reasons `registration-not-prepared` or `registration-not-materializing`.
Malformed saved documents use fixed
`argumentInvalid("Invalid Material registration document")`. An inconsistent
confirmed receipt uses fixed
`resourceInvalid("Invalid Material registration storage receipt")`. These
failures contain no raw submitted source values.

## Confirmed physical mapping

The exact `stored` object has `root`, `manifest` and ordered `files`. Root and
manifest are generic FileStore references encoded as exact `{store,key}`
objects. Every file is exactly `{path,reference,parent}`, where reference and
parent use the same generic shape. The manifest is a child of the confirmed
root by the existing writer confirmation contract; the root remains the
configured writer's fresh package collection.

All references belong to one store. Keys are nonempty, at most 4096 code units
and free of ISO controls; opaque slash and Unicode keys retain their spelling.
Root, manifest and file references are distinct. Original and derived parent
references are distinct from those resource references and from each other
when both resource groups exist. All originals share the exact confirmed
assets parent; all derived documents share the exact confirmed derived parent.
The file vector has precisely the plan's paths and order: originals then
derived documents. Decoding performs no physical IO, stat, list or discovery.

`StoredMaterialPackageFile` privately retains the exact parent already used and
validated during creation. This supports a later authorized original-read
adapter without folder discovery. The public FileStore API, contribution
schema, Material manifest and existing Operation responses are unchanged.

## Native Entity storage

The collection is
`EntityCollectionId(textus, knowledge_lake, MaterialPackageRegistration)`,
owner name `MaterialPackageRegistration`, with embedded managed revision.
The component registers a passive native Entity realm using StoreOnly memory
policy and Optimistic concurrency. Its small native realm Ref is native
runtime state, not a second registration database or application lock.

The logical record fields are `id`, `revision`, `document`, `materialRef`,
`state`, `titleSearchV1` and `referenceSearchV1`. ID/revision are native-managed.
The create codec supplies document, materialRef, state and the two projections;
it supplies no ID, revision, security, publication or audit defaults. The normal
Entity create boundary issues and complements those attributes.

The stored document is ASCII JSON TEXT produced with
`Printer.noSpaces.copy(escapeNonAscii = true)`. It preserves arbitrary UTF16,
including NUL and unpaired surrogate units, and complete opaque JSON. There is
no raw title, provenance or opaque-content SQL TEXT projection. `materialRef`
is the exact safe logical URN and `state` the exact lowercase document status.
Both are derived from the document.

The physical `titleSearchV1` and `referenceSearchV1` columns use common CNCF
`DataStoreTextSearchField` v1 with `caseInsensitive = true`. They encode the
complete title and exact logical URN after `String.toLowerCase(Locale.ROOT)`
as ASCII `u16x1:` followed by `x` and four lowercase hexadecimal digits for each
UTF16 code unit. These derived search fields are written in the same native
create/update as the complete document; the original contribution JSON and
Strings remain unchanged. No raw title column or SQL constraint relaxation is
needed for NUL or unpaired surrogate units.

Decoding rejects wrong collections, malformed or noncanonical native IDs,
absent or duplicate domain fields and invalid managed revisions. Unrelated
native security, audit and unknown attributes survive updates. Colliding
attributes cannot override managed fields. The decoder requires both projection
columns to be Strings exactly equal to the v1 encoding of the retained document;
missing, duplicate, wrong-type, wrong-version, noncanonical, stale or contradictory
columns are rejected along with inconsistent materialRef/state. There is no
automatic migration, backfill, fallback encoding or local completeness scan.
Existing rows without coherent projections require separately owned migration.
Malformed entities use fixed
`argumentInvalid("Invalid Material registration entity")`.

## Protected native programs

`MaterialRegistrationSupport` is available only to owning TKL native
FunctionalActionCalls. It requires provider authentication and uses the normal
component application datastore and authorized Entity APIs. It exposes no
public or test-only product API and calls no private interpreter, internal
Entity route, JDBC or direct transaction boundary.

`create_material_registration` derives owner and createdBy from the current
authenticated subject, issues one leaf using
`opaque_id("knowledge-lake.material-package")`, validates the explicit lake and
logical parent, and creates Prepared through native Entity issuance. Every
separate explicit create issues another independent registration. Get loads
the normal authorized native snapshot with no provider IO. Every supplied
registration EntityId is checked against the exact collection.

`search_registered_materials(text, offset, limit)` is the protected
registration-owner view consumed by the opted-in public search adapter. It
requires provider authentication before validating
parameters: text is absent or a nonnull String of at most 256 UTF16 units without
ISO controls, offset is nonnegative and limit is 1 through 100. Empty text is
valid. Invalid parameters return fixed
`argumentInvalid("Material search parameters are invalid")` before datastore
search. It uses native Entity `Store` scope and explicit `Owner` visibility with
the actual authenticated subject. The query is exactly Registered AND
(titleSearchV1 contains literal text OR referenceSearchV1 contains literal text),
using the common paired field vocabulary. Native UserPermission read filtering
occurs before ascending logical-URN ordering and offset/limit paging. The result
contains only `items` (materialRef/title summaries), `offset`, `limit` and
`hasMore`; the datastore query itself is unbounded with no offset.

Registration status is independent of native publication status. Ordinary create
uses native `EntityCreateDefaultsPolicy`'s `PostStatus.Published` with private
owner permissions, and materialization preserves both attributes. This internal
post status is neither a public-read grant nor external publication. Owner
visibility does not publish records, grant read permission or synthesize manager/System/ServiceInternal
privileges. An owner whose native read permission is refused receives no matching
items and `hasMore = false`. Search performs no native mutation or provider IO.
The default bootstrap search view and M01 literal substring meaning are
unchanged. No new CML Operation or privileged caller is introduced.

`get_registered_material(materialRef)` authenticates and validates the exact
existing bounded selector, then searches native Store/Owner scope for
Registered AND exact materialRef. UserPermission read filtering applies before
cardinality is considered. No authorized match returns fixed
`resourceNotFound("Material was not found")`; one returns the native record;
more than one returns structured `operationConflict` for `getMaterial` with
reason `material-reference-ambiguous`. It performs no query paging, case folding,
URN reconstruction, privileged existence/count probe, provider IO or mutation.
The public metadata projection exposes only the logical original resource
descriptions and server-issued registration provenance specified by the reference
contract. Full contribution/provenance and confirmed provider mappings remain
private and unchanged. Opted-in registered original reads authorize the exact
Material and select an exact original before consulting the initialized private
operator read-binding map. The document's `storageBinding` aliases an admitted
connection ID and store name, never a credential or a provider path. An absent
or unmapped alias remains explicitly service-unavailable. Every saved root,
manifest, file and parent must match that store before custody is consulted;
the selected original's index chooses its exact confirmed file and parent.
Missing/inconsistent mapping is resource-invalid. Native GoogleDriveFileStore
stat/read checks preserve the exact persisted resource, media, known length,
binary Bag and safe headers. Native custody/provider failures retain their
structured Conclusions without retries or native mutations. The full strict
configuration and transport contract belongs to
[Material reference v1](material-reference-v1.md#explicit-reference-mode);
the bootstrap provider route is never a fallback.

Materialize loads and authorizes the record and rejects non-Prepared state
before provider IO. It performs one observed update to Materializing and only
after that update returns successfully invokes the package writer exactly
once with the retained contribution, location, principals and trusted source
capability. On confirmed writer completion it performs one final observed
update using the actual returned Materializing snapshot revision, preserves
attributes, and returns the actual committed Registered snapshot.

Future public ingress must validate the explicit trusted producer/source
binding and TKL-managed grant before calling this support. A source capability
is not authorization. This slice parses no App/Book receipt, acquires no grant
and changes no credential or ACL contract. Ordinary native caller and write
authorization remain active independently of the provider capability.

## Durability and failure

The native datastore commits successful create and observed updates before
returning. Normal FunctionalActionCall/UnitOfWork owns the outer commit or
abort. A later outer abort cannot undo an already committed Prepared or
Materializing row. These states describe persisted domain progress, not worker
liveness, exclusion, ownership leases or automatic recoverability.

No external write occurs before acknowledged durable Materializing. Unknown
acknowledgement of that update propagates unchanged and invokes no provider.
Provider failure, including actual manifest-write-then-error, leaves the
actual durable Materializing record and may leave physical bytes. A refused
final save similarly leaves Materializing despite confirmed physical bytes.
Unknown final commit acknowledgement may leave the real row Registered while
the caller still receives the native structured failure. Explicit get is
authoritative for that actual saved state.

Native failure Conclusions propagate unchanged. Repeated materialization of
Materializing or Registered fails before another external write, including
after runtime reinitialization. There is no replay, rollback deletion, retry,
polling scheduler, queue, outbox, competing-writer protocol, shadow store or
component-local reservation/index protocol.

## Executable specifications and remaining integration

[MaterialRegistrationEntitySpec](../../src/test/scala/org/simplemodeling/textus/knowledgelake/material/MaterialRegistrationEntitySpec.scala)
authors state/mapping admission, native codec metadata and malformed data,
strict paired-projection coherence, redacted displays and at least 100
undiscarded arbitrary UTF16/JSON roundtrips. Its pure query scenarios preserve
independent original String literal/ROOT/UTF16 expectations and exact Owner/Store
selection without query paging.
[MaterialRegistrationPersistenceSpec](../../src/test/scala/org/simplemodeling/textus/knowledgelake/material/MaterialRegistrationPersistenceSpec.scala)
and its fixture author the real `postgres:17.11-bookworm` lifecycle with
`fsync=on` and `synchronous_commit=on`, separate committed JDBC read/audit
oracles, normal native authenticated command drivers, physical byte readback,
reinitialization, authorization, outer abort and actual commit-acknowledgement
uncertainty. Search scenarios retain nonempty positive owner results, foreign
subject isolation, Published SQL values decoded with existing `ValueReader[PostStatus]`
and independent private-owner permission expectations,
actual owner-read-permission refusal, unchanged SQL/audit/provider baselines and
at least 100 undiscarded arbitrary UTF16 source/needle correspondences against
an independent original String corpus oracle. Missing Docker/PostgreSQL is a
hard prerequisite failure. All
filesystem artifacts are test-owned below `target/cncf/work`, and each unique
database schema and physical episode has exact cleanup.

These are authored specifications, not passing-result claims. Parent-selected
validation also retains existing writer, Material reference Operation and
original-read regressions. Additional real-PostgreSQL scenarios author generated
public search/get dispatch in explicit registered mode, logical resource metadata,
exact ambiguity, private-owner read denial, unchanged SQL/audit/provider effects,
configuration rejection and reconstruction after native runtime reinitialization.
[MaterialRegisteredOriginalSpec](../../src/test/scala/org/simplemodeling/textus/knowledgelake/material/MaterialRegisteredOriginalSpec.scala)
authors normal PostgreSQL create/materialization followed by generated
ActionEngine original reads through synthetic owning-TKL managed custody and
the native Google FileStore/HTTP programs. It covers the second exact stored
original, retained binary Bag/private headers, metadata/search without HTTP,
reinitialization without a catalog or registration replay, authorization before
binding/custody, strict startup binding admission, mapping mismatch, native
provider/custody failures and unchanged SQL/audit/source/destination/custody
baselines. This controlled provider evidence is distinct from real Google proof.

Public registration ingress, trusted source ingestion/grant binding,
Workbench/Studio registered activation, focused validation, integrated review,
packaged/real Google evidence and the original whole Phase gates remain pending.
No deployed schema migration, successful registered original display or Stage
acceptance is claimed by these authored specifications.
