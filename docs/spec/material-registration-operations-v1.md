# Material registration Operations v1

Contract date: 2026-10-22
Owner: TKL-M03 / connected ESS3-02
Status: owning runtime and executable specifications authored; validation and connected acceptance pending

This contract specializes [Material access and processing](material-access-and-processing.md),
[Material contribution and physical package v1](material-registration-package-v1.md)
and the [native registration lifecycle](material-native-registration-v1.md).
It declares three public Operations on the existing
`org.simplemodeling.textus.KnowledgeLake` component's `Material` service.
Earlier kernel documents' statements that they add no public registration
Operation describe those earlier implementation boundaries. The generated factory
overrides now delegate to the existing native lifecycle support under the admission
rules below. Authored implementation does not establish successful execution,
completed M03 delivery or Stage acceptance.

## Public inputs and logical result

All three Operations use `authenticated_only` access and retain the actual
provider-authenticated caller's CNCF execution context. Normal native Entity
create, read and write authorization remains independent of Operation admission,
host configuration and OAuth/provider grants.

| Operation | Kind | Inline input value and exact attributes | Output |
| --- | --- | --- | --- |
| `createMaterialRegistration` | COMMAND | `MaterialRegistrationCreateCommand EXTENDS CommandAction`: `contribution: json`, multiplicity `1` | `MaterialRegistrationResult` |
| `getMaterialRegistration` | QUERY | `MaterialRegistrationSelectionQuery EXTENDS QueryAction`: `registrationId: string`, multiplicity `1` | `MaterialRegistrationResult` |
| `materializeMaterial` | COMMAND | `MaterialRegistrationMaterializeCommand EXTENDS CommandAction`: `registrationId: string`, multiplicity `1` | `MaterialRegistrationResult` |

`contribution` is one complete neutral `textus.material-contribution/1` JSON
value admitted by the existing contribution parser. It is neither a Book packet
DTO nor a flattened record or JSON string. There are no request fields for
owner, registration identity, URN, lake, logical leaf, Drive key, connection,
grant or physical destination. The selector is an actual native registration
EntityId, validated against the exact existing registration collection; it is
never reconstructed from an App/business key or Material URN.

The global value `MaterialRegistrationResult` contains exactly:

| Attribute | Type and multiplicity | Meaning |
| --- | --- | --- |
| `registrationId` | `string`, `1` | Actual native-issued registration ID, distinct from the opaque Material URN |
| `revision` | `long`, `1` | Positive observed revision of the actual committed native snapshot |
| `state` | `string`, `1` | Exactly `prepared`, `materializing` or `registered` |
| `materialRef` | `string`, `1` | Assigned planned TKL reference, including before the Material is available |

Default JSON response projection uses `registration_id` and `material_ref`.
Request names remain `registrationId` and `contribution`. The result is logical
native status only. It contains no contribution, provenance, physical mappings,
permissions, provider details or grants. Only `registered` makes the Material
available through ordinary search/get/read. Clients obtain the existing logical
metadata through `getMaterial` after observing registered status; a planned
reference in Prepared or Materializing is not an availability promise.

## Mode and owning-host admission

These Operations opt into exact `material.reference.mode=registered` under the
[existing explicit reference mode contract](material-reference-v1.md#explicit-reference-mode).
The default/bootstrap reads and all existing `searchMaterials`, `getMaterial`
and `readOriginal` DTOs, selectors and semantics remain unchanged. There is no
new service, gateway, general proxy or Candidate state.

Before consulting private configuration or accessing a provider, each Operation
requires an actual provider-authenticated caller. Creation and materialization
also require the owning host registration configuration below. Its absence
leaves those commands explicitly service-unavailable. Status requires only
explicit registered mode and ordinary native read authorization; it needs no
registration configuration, OAuth configuration, credential lookup or provider.
Existing metadata can likewise remain available without registration configuration.

After native Operation authentication, all three calls use the existing
provider-authentication/mode admission. Bootstrap returns fixed
`serviceUnavailable("Material registration requires registered mode")`.
Missing registration configuration for a command returns fixed
`serviceUnavailable("Material registration is unavailable")`. A configured-subject
refusal is fixed
`securityPermissionDenied("Material registration caller is not admitted")`.
Native authorization remains independently active after these admissions.

The optional nonsecret owning-TKL component parameter
`material.registration.configuration` is one strict JSON object. It has exactly
the following semantic fields; none is a per-request selector:

| Field | Required value |
| --- | --- |
| `schema` | Exact `textus.knowledge-lake.material-registration/1` |
| `id` | Host-selected storage-binding alias |
| `allowedSubjects` | Nonempty unique vector of exact nonblank provider-subject strings, each at most 256 UTF16 units and without ISO controls; no wildcard |
| `sourceFormat` | Exact `nict-book-material-transfer/1` for this initial adapter |
| `connectionId` | One TKL-managed CNCF OAuth connection ID |
| `storeName` | Generic FileStore name |
| `sourceParentKey` | Explicit selected source parent |
| `destinationParentKey` | Explicit selected destination parent |
| `lake` | Explicit TKL lake |
| `logicalParent` | Empty for the lake root, or an existing validated MaterialLocation logical path |

`id`, `connectionId`, `storeName` and `lake` each match
`[A-Za-z0-9][A-Za-z0-9._-]{0,127}`. Both parent keys use the current native
Drive key grammar `[A-Za-z0-9_-]{1,256}`. Both are explicitly selected, without
folder discovery or a requirement that they differ. The logical path retains
the existing MaterialLocation grammar and bounds. There is no default
production lake, multi-binding registry or caller-selected binding, folder,
principal, credential or destination.

Configuration text is at most 131072 UTF16 units. Missing, unknown or duplicate
semantic JSON fields, wrong types, unsupported format/provider tokens, invalid
identifiers, paths or subjects, and overlong text fail fixed
`configurationInvalid("Material registration configuration is invalid")`. The
bounded Jawn parser rejects duplicate semantic keys, including escaped spellings.
Configuration diagnostic displays are redacted and
failures echo no source or grant values. Supplied registration configuration
in Bootstrap fails configuration admission; it never switches mode or is
silently ignored. Configuration admission does not acquire credentials or
perform provider IO.
The Bootstrap refusal is fixed
`configurationInvalid("Bootstrap Material reference mode cannot include registration configuration")`.

The current runtime has one configured owning connection. Source and
destination use that same admitted `connectionId` and generic `storeName`.
The connection is CNCF-managed under this actual TKL component instance and
caller. Configuration holds no token, secret path or client secret. OAuth
authorization remains separate native owner/provider grant admission; actual
consent, credential changes and ACL changes are not performed by this contract.
An App/Studio `accessConnectionId` retained in input is provenance only, never
a grant, connection selector or requirement for a second legacy Studio credential.

Registration and registered-read bindings remain explicitly independent.
The new document's `storageBinding` is exactly configured `id`. To read its
output, the operator separately maps that alias to the exact same
`connectionId`/`storeName` in the existing registered-read configuration.
No binding is inferred, merged or installed automatically. Missing read
enablement means explicit original-read unavailability, not registration
failure or bootstrap fallback.

## Create a Prepared registration

`createMaterialRegistration` authorizes the actual authenticated subject
against configured `allowedSubjects` before source/contribution processing or
database access. It parses the complete neutral contribution using existing
admission, preserving all UTF16 units, complete JSON, array order and histories.
The owning adapter locally validates the source declarations described below
without provider or credential IO.

TKL selects the configured lake, logical parent and storage alias and delegates
to the existing native create support. Native support derives owner and
createdBy from the actual caller, issues the logical leaf through the existing
purpose-aware opaque-ID facility and lets Entity issue the native registration
ID. Client context/provenance is not authority, an identity derivation rule or
a production location default.

Creation returns the actual committed Prepared snapshot as
`MaterialRegistrationResult` only after durable acknowledgement. It performs
no provider read or write. An unacknowledged creation outcome is unresolved,
even if a row may exist. There is no automatic retry, replay or deduplication
by App/business key; every separate explicit native create issues an independent
registration. Existing structured native authorization, storage and uncertain
acknowledgement Conclusions propagate unchanged.

## Inspect actual registration status

`getMaterialRegistration` validates the exact native EntityId/collection and
loads the actual caller-authorized registration through existing owner
support. It projects that snapshot's ID, committed revision, lowercase state
and assigned logical reference. Ordinary native read authorization controls
disclosure. It performs no provider access, credential lookup, write,
preparation or materialization and does not require a host source configuration.
Malformed selectors, missing/unauthorized records and native load failures
retain their existing structured Conclusions. Explicit get is the authority
for saved status after an uncertain command outcome.

## Materialize exactly once from Prepared

`materializeMaterial` validates the exact native EntityId/collection through
existing owner support, loads and authorizes the actual record, and rejects
non-Prepared state before private connection or provider access. The actual
authenticated subject must be in configured `allowedSubjects`. The retained
document's `storageBinding` and location must match current host configuration;
an old record is never rebound to a new destination after configuration changes.
The exact retained storage alias, lake and logical path parent must match;
otherwise the call returns fixed
`resourceInvalid("Material registration does not match its configured destination")`
while the record remains Prepared. Pure source correspondence is re-admitted
from the retained contribution before managed custody is consulted.

The Operation constructs the existing managed credential, native FileStore,
writer and source capabilities without IO, then invokes the existing native
materialize helper. Native update/write authorization and an acknowledged
durable transition to Materializing using the observed native revision must
precede every actual external provider program. The writer executes once.
Confirmed completion supplies the private physical mappings for the final
native observed update, using the actual returned Materializing snapshot's
revision. Only confirmed native persistence returns the actual committed
Registered snapshot as `MaterialRegistrationResult`.
An absent initialized OAuth configuration returns fixed
`serviceUnavailable("Material registration connection is unavailable")` before
that transition. Missing, disconnected or wrong-owner managed credentials retain
their native Conclusions when the deferred provider program acquires authorization.
Capability construction performs no credential or provider IO; normal native write
authorization and durable Materializing therefore still precede credential use
and every provider effect. The helper retains its normal native reload and observed
revision behavior; no additional transaction or retry protocol is introduced.

The existing lifecycle conflicts remain structured `operationConflict` for
`materializeMaterial`, with fixed reasons `registration-not-prepared` and
`registration-not-materializing` at their respective transition boundaries.
Existing malformed-document, mapping, authorization, custody/provider, native
write and uncertainty Conclusions retain their exact meaning. There is no
direct SQL, shadow store, lease, outbox, permission change, retry or rollback
deletion introduced by public ingress.

Unknown Materializing acknowledgement invokes no provider. Provider failure
can retain durable Materializing and remote bytes. A refused final save leaves
Materializing despite confirmed bytes; unknown final acknowledgement can leave
the real row Registered while the caller receives the original failure.
The actual state and exact Conclusion remain intact. Repeated materialization
of Materializing or Registered fails before another provider action, including
after runtime reinitialization. Explicit get inspects status; it never authorizes
automatic replay, deletion or reset.

## Owning source adapter

For `nict-book-material-transfer/1`, every neutral original retains one complete
original App `materials` entry as opaque `sourceReference`. Only the owning TKL
adapter interprets `sourceReference.original.id`, `mediaType`, `byteLength` and
`sourceReference.receipt.providerId`, `remoteId`. Original `id`, `mediaType`
and `byteLength` must equal the neutral `resourceId`, `mediaType` and
`byteLength` respectively. Provider ID is exactly `google-drive`; remote ID
matches `[A-Za-z0-9_-]{1,256}`. Required correspondence failures return fixed
`argumentInvalid("Material original source reference is invalid")` before native
creation or provider IO. All originals are admitted before creation, including
unselected originals. The source capability resolves only an exact admitted
original and its retained receipt ID.

Extra original-entry, provenance and receipt fields are retained without
rejection or projection. `accessConnectionId`, client owner, URL, path and
folder are never authority or output destination. A complete receipt is
neither a Lake registration nor an access grant. No producer label in the
input is asserted to be authenticated. The actual admitted subject, known
source format and configured source parent bound this source-storage proxy.
The source capability itself confers no authority.

Before bytes are read, normal native GoogleDriveFileStore stat must confirm
the exact remote ID, File kind, configured source parent, neutral media and
known length. Read content must confirm that media and the known length of
the materialized Bag. Use the TKL-managed connection and normal native
GoogleDriveFileStore; there is no direct HTTP client, URL fetch, list or
discovery. Binary content and native failure Conclusions retain the existing
source/writer contracts. TKL's existing writer alone owns output identity,
manifest, layout and placement.
Native stat intrinsically checks the exact requested ID. The owning adapter then
requires File kind, configured parent, exact MIME and `Some(byteLength)` before
read; content requires that same exact MIME and known Bag size. Its mismatches
return fixed
`resourceInvalid("Material original source does not match its configured resource")`.
Native metadata/transport/custody failures propagate unchanged. Product code does
not materialize, stringify, close or mutate the borrowed Bag.

The writer may have created its fresh package collection before source
stat/read fails. Durable Materializing precedes every such external effect;
the record and possibly written bytes remain retained and unavailable without
automatic replay or deletion. An empty-original contribution performs no
source read but still requires admitted destination/writer and native
registration. This contract introduces no bulk corpus migration.

## Executable specifications and remaining integration

[MaterialRegistrationOperationsSpec](../../src/test/scala/org/simplemodeling/textus/knowledgelake/material/MaterialRegistrationOperationsSpec.scala)
and its owning controlled-provider fixture author normal generated dispatch and
ActionEngine execution for all three Operations. Required PostgreSQL 17.11 with
normal durability settings supplies separate committed JDBC/audit observations.
The scenarios cover complete opaque JSON retention, exact logical results,
source/caller/configuration admission, managed source/writer/read programs,
physical bytes and mappings, restart, native authorization, exact structured
Conclusions, uncertain native acknowledgements and no replay. Every provider
effect records a separately observed durable Materializing snapshot. Synthetic
private grant files and a counted native HTTP peer are controlled-provider
evidence; they do not establish real Google/App acceptance.

These authored scenarios supplement the existing
[native lifecycle specifications](material-native-registration-v1.md#executable-specifications-and-remaining-integration)
and [reference contract](material-reference-v1.md). Existing 115-case coverage
and original UTF16/source/search properties retain at least 100 successful
samples with zero discards. Missing required PostgreSQL is a failure, never a
skip. Validation results and acceptance are separate parent-owned gates.

Fixed public Workbench.MaterialWork forwarding and Studio immutable-source
conversion/action follow through the same caller context. They receive logical
status/references and never call private TKL classes or interpret Google
mappings. The integrated observable route requires its independent protected
Step review; no helper-only acceptance review or commit substitutes for it.
Composed Studio activation, private packaged runtime, actual Google/App
evidence, final full validation/review and the original whole Phase gates
remain open. Declared capability is not passing, reviewed, deployed or accepted
behavior.
