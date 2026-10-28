# Material reference contract v1

Status: implementation contract; implementation and acceptance pending
Contract date: 2026-10-22
Owner: TKL M01/M02

This contract specializes [Material access and processing](material-access-and-processing.md)
for the first Studio -> Workbench -> TKL original-reference route. It does not
implement M03 registration/output or OpenClaw processing. Operation availability,
generated ABI and dependency coordinates must be verified before consumer acceptance.

## Public Operations

The component identity is `org.simplemodeling.textus.KnowledgeLake`; service
`Material` exposes the following QUERY Operations with authenticated-only access.
These are domain Operations, not Drive API wrappers.

| Operation | Input | Output |
| --- | --- | --- |
| `searchMaterials` | `MaterialSearch(text?, offset?, limit?)` | `MaterialSearchResult(items, offset, limit, hasMore)` |
| `getMaterial` | `MaterialSelection(materialRef, evidenceResourceIds*)` | `MaterialMetadata(materialRef, title, resources, provenance, observedEvidence*)` |
| `readOriginal` | `OriginalSelection(materialRef, resourceId)` | Private binary original response with the resource's admitted media type |

`materialRef` is an opaque TKL-issued Material reference, including an existing
Knowledge Lake URN. Clients retain and pass it unchanged; they neither compose
it from an editing Entity ID nor derive a provider location from it. Incoming
App request/subject IDs, native editing Entity IDs and TKL references remain
distinct. A work/evidence association links them without replacing any identity.

`resourceId` selects a logical resource within that Material. It is not a Drive
file ID, folder name or client-chosen URL. A resource description contains
`resourceId`, `role`, `mediaType` and optional `byteLength`; metadata contains
logical references and attributed provenance, never credentials/provider paths.
Search items contain only `materialRef` and `title`. Empty results use `items=[]`.

### Additive resource evidence observation — 2026-10-28

[Material evidence observation v1](material-evidence-observation-v1.md) defines
the explicit `evidenceResourceIds` opt-in on existing `getMaterial`. Omission or
an empty collection preserves the exact four-field metadata-only projection and
does not read originals. A nonempty exact logical selection adds typed
`observedEvidence` from complete original reads surrounded by source-version
observations. Unknown versions remain unverified; changed resources fail.
Source versions, native registration revisions and Candidate revisions remain
distinct. Observation provides neither historical pinned bytes nor retention.

The TKL-issued evidence identity includes an opaque resource-incarnation
qualifier, so reconfiguring a logical resource to another backing original
cannot reuse an evidence identity solely because both files have the same
file-scoped source version. The qualifier is an identity digest, never a
source version, content attestation or snapshot. Physical mappings remain private.

Compatibility here means existing Operation identities, omitted/empty wire
behavior and generated `createC` readers. At the development SNAPSHOT, generated
Scala value constructors gain the additive collection parameter and require
consumer regeneration/source adaptation where constructed positionally.
Binary or positional-constructor compatibility is not claimed. Generated source
is not hand-edited to create a second contract.

Search is read-only, case-insensitive title/reference substring matching for
this initial route. Text is at most 256 characters; offset defaults to 0 and
must be nonnegative; limit defaults to 20 and is between 1 and 100. Results have
stable reference ordering, and paging/hasMore applies after authorization.
Search does not list arbitrary Drive files or require Candidate creation.

## Explicit reference mode

The optional component parameter `material.reference.mode` selects one view at
component initialization. Absence and exact `bootstrap` retain the existing
trusted read-only catalog route. Exact `registered` selects the native
registration view described below. Empty, padded or unknown mode strings fail
with fixed `configurationInvalid`; rows, catalog contents and grants never
infer a mode. Exact `packages` selects the additive stored-package read-only
view in [Stored Material package read v1](material-package-read-v1.md).
Registered and Packages require `material.reference.catalog` to be absent;
supplying even an empty catalog with that mode is a configuration error. The
views are never merged, shadowed or used as fallback for one another.

Packages accepts only its trusted `material.reference.packages` bindings and
rejects registered read bindings. Bootstrap and Registered reject any supplied
package configuration, even empty. Package subjects authorize exact configured
roots before connection/provider access; remote owner or provenance does not
authorize. Packages uses bounded immediate-child enumeration (50 per page,
four pages and 200 children per collection) and a strictly bounded 65536-byte
manifest. It exposes logical metadata and original bytes through the same
Operations, never physical mappings, SQL registration or automatic writes.
Missing configuration is unavailable; an explicit empty materials list searches
as empty. Registration commands remain available only in Registered mode.
The paired MaterialPackageReadSpec and MaterialPackageReferenceSpec are linked
from the package-read contract.

In registered mode, the existing `searchMaterials` Operation uses the ordinary
native Entity Store/Owner query over Registered rows, including native
UserPermission read filtering before stable exact-URN ordering and paging.
Matching, valid input and paging semantics above are unchanged. Prepared and
Materializing registrations are unavailable as Materials. The existing
`getMaterial` Operation queries Registered AND the exact supplied materialRef
through the same native authorization boundary, without parsing, trimming,
case folding or reconstructing the reference. No authorized match returns
`resourceNotFound`, including references hidden by native read permissions;
more than one authorized match returns structured `operationConflict` for
`getMaterial` with reason `material-reference-ambiguous`, never a selected first
row. Neither operation needs a catalog, OAuth grant or provider access.

Registered metadata contains exactly `materialRef`, original `title`,
`resources` and `provenance`. Resources are the retained contribution's original
entries in their original order, each with exact `resourceId`, original `role`,
`mediaType` and known `byteLength`. Derived JSON documents are not original
resources. Provenance is one server-issued logical pair, `kind: registration`
and `reference: <native registration ID>`. This does not replace the Material
URN or invent an originating App identity. Complete opaque source references,
provenance and document histories remain lossless in private native storage,
but are not copied into this limited public metadata projection. Provider
mappings, storage binding, native security attributes and credentials do not
leave that storage boundary.

Registered `readOriginal` first resolves the exact Registered Material through
that native Store/Owner and UserPermission boundary, then selects the exact
`resourceId` among its retained original entries. Unknown and derived resource
IDs return `resourceNotFound` before binding, credential or provider access.
Duplicate authorized Material references retain the same structured ambiguity
conflict as get. The logical selectors are never interpreted as provider keys.

The opt-in private component parameter `material.reference.registered.bindings`
contains exactly `schema: "textus.knowledge-lake.material-read-bindings/1"` and
`bindings: [...]`. Each entry contains exactly `id`, `provider`, `connectionId`
and `storeName`. Provider is exactly `google-drive`; the other values each match
`[A-Za-z0-9][A-Za-z0-9._-]{0,127}` and IDs are unique. Text is at most 131072
UTF16 units. Missing, unknown, duplicate semantic object fields, wrong types,
unsupported providers and invalid identifiers fail fixed `configurationInvalid`
without echoing private values. Diagnostic displays are redacted. Bindings
contain no tokens, paths, resource keys or parent keys and are admitted once
at component initialization. Bootstrap rejects any supplied registered binding
configuration, including an explicitly empty one. Registered still rejects
any supplied bootstrap catalog. Configuration never infers or changes mode.

An absent configuration or explicit empty bindings produces an empty registered
map. After native authorization and exact original selection, an absent or
unmapped document `storageBinding` returns fixed
`serviceUnavailable("Registered Material original access is unavailable")`.
There is no catalog, provider or bootstrap fallback. A mapped alias selects
only its admitted managed connection and generic store name. The saved mapping
must exist, and every stored root, manifest, file reference and parent must
belong to that exact store. The ordered stored file at the selected original's
index supplies the confirmed key and parent; missing or mismatched storage
returns fixed `resourceInvalid` before credentials. No key is derived from a
URN, path, title or client input, and no discovery/list operation is performed.

The existing native GoogleDriveFileStore then confirms the exact persisted
file ID, file kind, parent, media and known length, followed by exact media and
known Bag length on read. Its binary Bag and private response headers are
preserved. The explicitly selected managed Google Drive authentication strategy
must be initialized before connection resolution. The connection remains with
this TKL instance and its admitted actual caller; when OAuth is selected, its
owner-bound user grant remains in that instance's custody. Native provider/custody
Conclusions propagate without retries, writes or identity substitution.
Public Operation identities and ordinary binary transport are unchanged. The
explicit evidence extension above is additive to the metadata DTO and ABI. This read adapter
adds no public registration commands or M03 availability claim. Runtime source
ingress/registration, composed Workbench/Studio registered activation, packaged
and real Google evidence and connected whole Phase acceptance remain required.

## Context, failures and original transport

Every hop uses CNCF public Operation dispatch with the caller's execution
context. Authentication, operation admission and Material/resource read
authorization precede credential lookup and outbound access. Component linkage,
a known URN, catalog configuration and Google consent confer no read authority.
Workbench does not substitute its own privileged identity for the caller.

Invalid selectors and paging fail with structured argument-invalid conclusions.
Missing Material/resource, denied access, unavailable connection and unfinished
preparation remain distinguishable according to the existing disclosure policy.
Search must not disclose unauthorized metadata through counts or paging.
Ordinary original reads never start preparation, create a Candidate, register
a package or retry an uncertain mutation. Failures retain their Conclusion
through both nested dispatches and the existing safe HTTP projection.

All three explicit modes transfer original bytes through the admitted native
Operation response, with private/no-store, nosniff, sandbox and attachment
headers. Studio may display safe raster image types using a revocable local
Blob URL, or offer a download for unsupported types. It never embeds active
provider content as application HTML. Original bytes are not base64 fields in
ordinary entity records. Direct transfer descriptors are not required in v1;
adding them later cannot expose a reusable Google bearer or shift registration.

## First Material preparation

For the default bootstrap route, use one explicitly selected existing original and a TKL-owned, operator-provided
read-only bootstrap catalog entry. That entry binds a logical Material reference,
title, provenance, authorized subjects and logical resource ID to its configured
provider connection and physical resource. The bootstrap catalog is persisted
as trusted TKL component configuration, not a Studio/Workbench registry, a
folder-discovery algorithm or a new general ingestion platform.

Only TKL interprets the physical binding. It validates the selected resource's
metadata/media/length and obtains its bytes under the configured connection.
A missing, revoked or mismatched resource fails explicitly; another file is not
selected as fallback. Provider success alone never registers a new Material.
The bootstrap association is an explicit TKL administrative configuration act,
not a claim that an arbitrary upload receipt is Lake registration. M03 remains
the owner of runtime registration, package output and placement migration.

Use a harmless test image already accessible to the selected Google account
for the real-provider case. Controlled native-provider specifications may use
test-owned bytes but must be recorded separately from real Google acceptance.
No folder creation, source copying, ACL change or corpus migration is implied.

### Trusted bootstrap catalog

The optional TKL component parameter `material.reference.catalog` contains one
operator-authored JSON object with `schema: "textus.knowledge-lake.material-catalog/1"`
and `materials: [...]`. An entry contains exactly `materialRef`, `title`,
`authorizedSubjects`, `resources` and `provenance`. Subject IDs are exact native
provider-authenticated CNCF subject IDs; no wildcard or implicit public entry
is admitted. They authorize reads of that Material and its resources, not
another Material or an OAuth grant.

A resource contains `resourceId`, `role`, `mediaType`, optional `byteLength`
and private `storage`. For this first binary-original route, role is `original`;
storage contains exactly `provider: "google-drive"`, `connectionId`,
`resourceKey` and `parentKey`. These are trusted TKL configuration, never
Operation input/result fields. Provenance contains logical `kind`/`reference`
pairs only. Duplicate Material/resource identities, unknown fields, invalid
selectors/media/length, empty authorization lists and unsupported providers
fail configuration admission without echoing the private values.

In bootstrap mode an absent catalog leaves the capability explicitly unavailable; an explicitly
empty catalog returns an empty authorized search. Metadata remains available
without contacting Google or having a grant. Original read first authenticates
the caller and authorizes the exact Material/resource, then uses the selected
managed connection to stat and read exactly that configured physical resource.
The stat must confirm file kind, parent, media and any declared length. The
returned media and any declared length must still match. Mismatch fails without
fallback, automatic registration or business writes.

## OAuth connection handoff

### Additive service-account selection (2026-10-24)

The owning TKL factory also declares CNCF's optional
`google-drive.service-account.configuration` opaque SecretReference. With this
explicit setting, the common GoogleDriveConnectionRuntime uses the service
account instead of a user grant; OAuth configuration and this setting are
mutually exclusive. Existing OAuth configuration and browser management remain
unchanged when the new setting is absent. A stale ambient
GOOGLE_APPLICATION_CREDENTIALS never changes an OAuth selection.

The private configuration follows CNCF `cncf.google-drive-service-account/1`:
connectionId, allowedSubjects, scopes, and either the canonical credentialFile
path or explicit credentialEnvironment=GOOGLE_APPLICATION_CREDENTIALS. Private
host/key custody, native token HTTP admission and renewal belong to CNCF.
TKL's existing logical storage mappings still refer only to connectionId and
storeName; no key, token, email or physical layout enters Studio/Workbench APIs.
Allowing a caller to use the server connection never grants Material/Entity
permission, and Material permission alone never grants connection use.
Registration and original reads share this explicit selection without an
OAuth-only availability prerequisite. Missing/invalid configuration or denied
token acquisition never falls back to another grant or retries a business write.

`MaterialServiceAccountSpec` covers the actual owning factory/native operation
read path, token reuse and independent Material/connection authorization, with
existing OAuth specifications retained. Real Mac mini/Google Drive connection
and upload/readback remain separate operational acceptance, not claimed here.

### Existing user OAuth handoff

TKL composes the existing CNCF `OAuthConnectionRuntime`. Its factory declares
the existing `oauth.connection.configuration` opaque secret-reference key.
The CNCF configuration/grant is bound to the KnowledgeLake component instance
and the actual provider-authenticated owner; a Studio-bound grant is not
silently relabeled, exported or copied. A new consent may be required.

TKL's ordinary provider binding retains only the nonsecret connection ID;
`GoogleDriveCredentials.managedConnectionC` and the existing FileStore obtain
authorization through the current TKL runtime. Workbench passes logical
Material/resource selectors, not grants, secret-file paths or access tokens.

Studio may initiate the existing CNCF connection UI at
`/web/knowledge-lake/connections`. Its status/authorize/callback/disconnect
routes remain CNCF-owned; the selected callback is
`/web/knowledge-lake/connections/<connectionId>/callback`, while returnPath may
be `/web/nict-editing-studio`. Account consent and permission changes remain
explicit human operations. The connection owner must authenticate at the TKL
management route too; composition must not invent an identity bridge or bypass
that admission when no compatible authentication provider is configured.

Missing Google client registration/configuration does not prevent controlled
software validation, but real Drive access remains unaccepted until the operator
provides the selected private configuration and test resource. Token refresh
does not imply resource permission or replay a business mutation. Preparation
instructions and operational observations belong in journal.
