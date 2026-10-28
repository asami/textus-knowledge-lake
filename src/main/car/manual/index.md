# Knowledge Lake reference manual

Component: `org.simplemodeling.textus.KnowledgeLake`\
Version: `0.1.0-SNAPSHOT`

See the [user guide](user-guide.md),
Material reference v1 (`docs/spec/material-reference-v1.md`) and
Material access and processing (`docs/spec/material-access-and-processing.md`)
in the TKL source checkout. The latter two are repository-relative source
references, not packaged manual links.
This manual describes the Material reference route and owning registration
Operations. The remaining composed ingestion and processing gates are separate.

## Material service

| Authenticated-only QUERY Operation | Input | Output |
| --- | --- | --- |
| `searchMaterials` | `MaterialSearch(text?, offset?, limit?)` | `MaterialSearchResult(items, offset, limit, hasMore)` |
| `getMaterial` | `MaterialSelection(materialRef, evidenceResourceIds*)` | `MaterialMetadata(materialRef, title, resources, provenance, observedEvidence*)` |
| `readOriginal` | `OriginalSelection(materialRef, resourceId)` | Private binary original with admitted media type |

`getMaterial` remains metadata-only when evidenceResourceIds is absent or empty.
Explicitly selecting at most 128 distinct exact logical original IDs requests
complete read observations under the same caller. The additive observedEvidence
items contain evidenceRef, materialRef, resourceId, optional revision,
retrievalState and limitation. Available means the exact provider source version
was observed unchanged around a complete original read; missing versions remain
unverified without revision, and changed resources fail rather than returning
partial evidence. The opaque evidenceRef qualifies the actual resource binding;
it exposes no physical mapping. Its identity digest is never a source revision.
No historical version-pinned access or retention is promised. Existing search
and readOriginal behavior is preserved. The full source contract is
`docs/spec/material-evidence-observation-v1.md` in the TKL checkout.

| Authenticated-only registration Operation | Kind and input | Output |
| --- | --- | --- |
| `createMaterialRegistration` | COMMAND `contribution: json` | `MaterialRegistrationResult` |
| `getMaterialRegistration` | QUERY `registrationId: string` | `MaterialRegistrationResult` |
| `materializeMaterial` | COMMAND `registrationId: string` | `MaterialRegistrationResult` |

Registration requires explicit `material.reference.mode=registered`. Create
accepts one complete `textus.material-contribution/1` JSON value and returns a
new native registration ID, committed revision 1, `prepared` state and planned
`materialRef`. The four result fields are `registrationId`, `revision`, `state`,
`materialRef`; default response JSON uses `registration_id` and `material_ref`.
Keep the native ID and planned Material reference separately. Status takes the
exact native ID and returns its actual committed state/revision. It needs no
registration configuration, OAuth grant or registered-read binding.

Materialize selects that native ID, admits only Prepared, then uses ordinary
native write authorization and acknowledged durable Materializing before any
provider action. Confirmed completion returns Registered at actual revision 3.
Only Registered is available through search/get/read. Results expose no
contribution, provider mappings, credential, permission or grant.

Material references, including TKL-issued URNs, are opaque logical identities.
Keep them unchanged. Resource IDs select logical resources within a Material.
Drive file IDs, folder names, physical locations and credential references are
not public Operation selectors or result fields. Search items contain
`materialRef` and `title`; resource descriptions contain `resourceId`, `role`,
`mediaType` and optional `byteLength`. Provenance is logical and attributed.

Search is read-only case-insensitive title/reference substring matching. Text
is at most 256 characters; offset defaults to 0 and must be nonnegative; limit
defaults to 20 and is between 1 and 100. Reference ordering is stable. Paging
and `hasMore` apply after authorization, without leaking unauthorized counts.
An authorized empty page contains `items=[]`.

These are logical camelCase field names. Default native JSON Server projection
uses `material_ref`, `has_more`, `resource_id`, `media_type`, `byte_length` in
responses; request selectors remain `materialRef` and `resourceId`.

## Ownership and safety

The default `material.reference.mode` is `bootstrap`. Its initial catalog is
TKL-owned read-only operator bootstrap configuration.
It binds logical Material/resource identities and exact native authorized
subjects to private provider configuration. It is not discovery, general import
or registration. Only TKL interprets physical mappings. An absent catalog leaves
the capability explicitly unavailable; an explicitly empty catalog permits an
empty authorized search. Metadata queries need no provider access or grant.

Explicit `material.reference.mode=registered` uses native Registered Entity
records with normal Store/Owner and UserPermission authorization. Registered
users must not supply `material.reference.catalog`; the two views never mix or
fall back to each other. Search/get expose the retained logical originals and
server-issued registration provenance, while physical receipts remain private.

Registered original reads opt in through private operator configuration
`material.reference.registered.bindings`, whose exact schema is
`textus.knowledge-lake.material-read-bindings/1` with a `bindings` array. Each
entry contains only `id`, `provider: google-drive`, `connectionId` and
`storeName`; the three identifiers follow the bounded native token grammar.
Each unique alias maps the retained registration's storage binding to the
owning TKL managed connection and exact stored FileStore name. It contains no
tokens, provider keys or paths. Unknown/duplicate fields, invalid types/schema
or identifiers fail configuration admission with redacted diagnostics.
Bootstrap rejects any supplied registered bindings, including an empty array.
Registered permits absent or empty bindings; exact original reads then remain
explicitly unavailable. Metadata and search still require no provider grant.

Creation/materialization additionally need the nonsecret owning-host parameter
`material.registration.configuration`, a strict JSON object with schema
`textus.knowledge-lake.material-registration/1`, `id`, `allowedSubjects`,
`sourceFormat: nict-book-material-transfer/1`, `connectionId`, `storeName`,
`sourceParentKey`, `destinationParentKey`, `lake` and `logicalParent`.
Identifiers are bounded native tokens; parents are exact Drive keys, logical
parent follows the existing Material path grammar and may be empty. Subject
strings are exact nonblank native provider subjects without wildcards or
controls. Missing/unknown/duplicate fields, invalid types and bounds fail with
redacted Configuration/Invalid; Bootstrap rejects supplied registration
configuration. No mode, production lake, subject, folder or connection is inferred.

Only admitted subjects may create/materialize. The initial source adapter checks
each complete retained `sourceReference.original` ID/MIME/length against the
neutral original and checks `receipt.providerId=google-drive` with a valid
`remoteId`. Extra fields remain opaque provenance. In particular,
`accessConnectionId`, client owner and URLs grant no authority. Before copying,
native stat/read must confirm the exact receipt ID, configured source parent,
File kind, MIME and known length. Both source and output use the same configured
owning-TKL connection/store. The retained registration alias/lake/logical parent
must match current configuration; records are never rebound after configuration
changes. Separate registered-read configuration must explicitly map that alias
to the same connection/store; registration installs no read binding or consent.

Every public dispatch carries the caller's CNCF execution context. Authentication,
Operation admission and exact Material/resource subject authorization precede
credential custody lookup and provider access. A URN, linkage, catalog entry or
Google consent alone does not confer read permission. Workbench does not supply
a privileged replacement caller.

TKL owns the existing managed CNCF OAuth runtime, configuration and grant bound
to its component instance and provider-authenticated owner. Management is at
`/web/knowledge-lake/connections`, with callback
`/web/knowledge-lake/connections/<connectionId>/callback`; the return page may
be `/web/nict-editing-studio`. The connection owner authenticates at TKL too.
There is no auth bridge or export/copy of an old Studio grant.

Alternatively, TKL can explicitly select CNCF's server service-account strategy
with `google-drive.service-account.configuration`, an opaque reference to a
private configuration file. It is mutually exclusive with
`oauth.connection.configuration`. The existing managed connection ID and
Material Operations remain unchanged. The account's Drive permissions and
configured `allowedSubjects` are independent of Material authorization.
This strategy uses no browser consent or user impersonation. OAuth management
continues to manage OAuth only; a successful Material read verifies the server
connection for that caller and resource.

Original reads stat and read exactly the configured resource, checking kind,
parent, media and any declared length. Missing, revoked or mismatched resources
fail explicitly. Invalid arguments, denied access, missing references,
unavailable connections/capabilities and unfinished preparation retain native
structured Conclusions and safe HTTP projection through consumers.
In registered mode, native Material authorization and exact original selection
precede binding and custody lookup. Every stored reference/parent must match
the configured store. The confirmed saved original key and parent are selected
by original index without deriving provider locations from a URN or discovering
folders. Store/mapping mismatch fails before credentials; stat/content mismatch
fails without fallback or mutation.

Binary originals retain `Cache-Control: private, no-store`,
`X-Content-Type-Options: nosniff`,
`Content-Security-Policy: default-src 'none'; sandbox` and
`Content-Disposition: attachment; filename="original"`. Bytes are not base64
entity fields. Consumers may preview safe raster types with revocable Blob URLs
or download other types; they do not embed active provider content as HTML.

After uncertain create/materialize failure, preserve the exact native Conclusion
and inspect `getMaterialRegistration` for actual saved status. Materializing may
retain remote bytes; unknown final acknowledgement may have saved Registered
while the caller still receives failure. Repeated materialization of Materializing
or Registered fails before provider access, including after restart. There is no
automatic retry, replay, reset, rollback deletion or provider fallback.

Reads start no registration, preparation or OpenClaw workflow and create no
Candidate. A provider receipt is neither native registration nor human approval.
Owning public registration authoring does not establish composed Studio activation,
real Google/App evidence, packaged-runtime acceptance or whole Stage acceptance.
