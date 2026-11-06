# Mobile Material delivery contract v1

Date: 2026-10-24
Status: native delivery runtime enabled; controlled integration verification in progress; no deployed/live acceptance
Owner: TKL-D01 / App ESA63-01

This specializes the agreed TKL-managed mobile delivery direction. It reuses
the existing KnowledgeLake component, native Entity storage/authorization,
Material contribution/package model and ordinary Material access. It does not
change the existing Material registration Operations or their one-shot behavior.

## Public service and Operations

The additive service is `MaterialDelivery` on
`org.simplemodeling.textus.KnowledgeLake`. All Operations require actual native
provider authentication and the owning TKL application's permitted subject.
Component linkage, a delivery reference and Google credentials grant no Entity
or resource authority. Use normal native Entity authorization; do not select
ServiceInternal to avoid caller permissions.

| Operation | Kind | Required input | Result |
| --- | --- | --- | --- |
| `getDestination` | QUERY | None | `MaterialDeliveryDestination` |
| `prepare` | COMMAND | `requestId: string`, `contribution: json` | `MaterialDeliverySnapshot` |
| `findForRequest` | QUERY | `requestId: string` | `MaterialDeliveryLookup` |
| `get` | QUERY | `deliveryRef: string` | `MaterialDeliverySnapshot` |
| `uploadOriginal` | COMMAND | `deliveryRef: string`, `resourceId: string`, `content: blob` | `MaterialDeliverySnapshot` |
| `finalize` | COMMAND | `deliveryRef: string` | `MaterialDeliverySnapshot` |
| `prepareOriginalTransfer` | COMMAND | `deliveryRef: string`, `resourceId: string` | `MaterialDeliveryTransfer` |
| `confirmOriginalTransfer` | COMMAND | `deliveryRef: string`, `resourceId: string`, `transferRef: string`, `result: json` | `MaterialDeliverySnapshot` |

These names are declared in CML and the source ABI, and their generated Scala
contracts are generated. The handwritten runtime enables MaterialDelivery and
the existing Material service; no deployed MaterialDelivery HTTP route is claimed. Normal CNCF routing/generation supplies the ingress;
the client must not invent a second gateway. JSON command field names above
are camelCase. Native RecordEncoder spelling is grounded by executable serializer
assertions in MaterialDirectDeliveryOperationsSpec and MaterialDeliveryIntegrityOperationsSpec.
Those assertions cover RecordResponse encoding; an HTTP adapter may apply its
registered field aliases and needs its own parent validation. Core must accept
only closed typed aliases and reject ambiguous simultaneous aliases. Opaque contribution/document
JSON is never renamed. Structured `contribution` remains its original JSON tree and field
spelling; it is not JSON serialized into a string.

requestId and resourceId follow `[A-Za-z0-9][A-Za-z0-9_-]{0,127}`, matching
the existing Core Commit/resource identities. Opaque references, logical
destination IDs/labels and safe action text are nonblank strings of at most
2048 UTF16 units, with no C0/C1 control characters; retain accepted spelling.
Original media types use the existing parameter-free type/subtype grammar and
known byte lengths are nonnegative. A configured maxResourceBytes is likewise
nonnegative. Available destination projection requires its ID, label and
supported transferMethod; an unavailable/unconfigured projection may omit that
logical pair together instead of inventing values.

Original ingress selects the existing native multipart binary path. The
compiled upload Action retains the native Request/Record, while its blob schema
uses XBlob. TKL's MaterialDeliveryOriginalInput reads the native named Request
properties: content is MimeBody with a BinaryBag. Http4s also supplies optional
content.filename metadata in the original form. Because the generated upload
schema declares only deliveryRef/resourceId/content, public protocol ingress
classifies content.filename as a named Argument. The owning TKL boundary admits
that exact optional Argument only for HTTP POST when it uniquely matches the
original form field. Required business fields remain properties. Every admitted
business value must correlate with the original wire form; duplicate metadata
across properties and arguments, missing or mismatched metadata, unknown or
positional arguments, switches, duplicate/unknown wire fields, POST query fields,
and non-binary content fail admission. Header-derived authorization cannot hide
an authorization field supplied in the body or query.

Non-HTTP callers retain the existing property-only contract with no arguments or
switches. A filename property remains compatible; an HTTP filename property must
also uniquely match its wire field. In either representation, filename is
optional untrusted metadata: a nonempty String of at most 2048 UTF16 units without
C0/C1 control characters. Its spelling is retained, never interpreted as a path
or provider key. Do not extract the binary part through a flattened Record:
dotted filename metadata changes that Record's structure.

MaterialDeliveryHttpInputSpec reproduces the native parameter classification
through public Ingress.recordToParameters with UploadOriginalOperation, then
retains the original HTTP source through Request.ofHttpRequest. It specifies
exact binary bytes/MIME and strict wire correlation. MaterialDeliveryInputSpec
specifies non-HTTP compatibility and malformed input rejection. These input
specifications do not themselves prove a live HTTP parser, persistence, provider
upload or registration. No CNCF or ValueReader extension is required. Do not
encode media into JSON or parse binary through text.

## Logical values

`MaterialDeliveryDestination` contains logical `destinationId`, `label`,
`availability`, `transferMethod`, optional `transferReason`, and `maxResourceBytes` when configured.
The first deployment supplies one destination. Direct transfer is preferred:
`googleDriveResumable` when the actual FileStore implements the matching optional
reserved direct preparation/independent status capability; otherwise `tklMultipart` with reason
`providerDirectUploadUnsupported`. An explicit operator `preferDirect=false`
selects server transfer with `operatorSelectedServerTransfer`; default is true.
This is server-owned policy, never mobile physical-storage configuration.
The selected method/reason are retained in each delivery and its public snapshot.
Availability is `available`, `configurationRequired` or `unavailable`; missing
configuration never produces an invented destination. No drive/folder ID,
server path, Lake layout, key locator, bearer token or provider upload URL
appears here. A read performs no provisioning, upload or consent action.

`MaterialDeliverySnapshot` contains:

- `deliveryRef`: opaque TKL delivery identity, separate from requestId and Material URN;
- `requestId`: exact admitted client correlation;
- `revision`: positive native committed revision;
- `destinationId` and `destinationLabel`: logical binding retained at preparation;
- `state`: `prepared`, `transferring`, `readyToRegister`, `registering`,
  `registered`, `verifying`, `verificationFailed` or `reconciliationRequired`;
- `resources`: ordered logical resource progress with resourceId, mediaType,
  byteLength and `pending`, `transferring`, `uploaded` or `reconciliationRequired`;
- `materialRef`: absent before confirmed Registered, then the exact TKL reference;
- `actionRequired`: absent or a safe supported action/reason, without private details.

Native RecordEncoder assertions cover `deliveryRef`, `requestId`, `destinationId`,
`destinationLabel`, `materialRef`, `actionRequired`, and nested `resourceId`,
`mediaType`, `byteLength`. Earlier snake_case client checkpoints do not prove HTTP
output spelling; Core's next pass must explicitly bind tested adapter aliases.
Enum strings above remain literal; opaque JSON keys are never renamed.
Absence and explicit JSON null remain distinguishable on input; an optional
result may use the generated omission behavior consistently. Do not substitute
an empty Material reference for an unfinished result.

`MaterialDeliveryLookup` contains `found: boolean` and optional `delivery`.
No authorized match returns found=false and no delivery. Unauthorized records
must not be disclosed through request correlation, existence checks or counts.
An unavailable datastore is a failure, not found=false. Queries are passive.

## Preparation, identity and durability

Core persists one requestId with the immutable local Commit before remote IO.
TKL scopes correlation to the actual authenticated caller and application; it
does not accept a principal or native EntityId from request JSON. App IDs are
provenance/correlation, never a rule for constructing a Material URN.

Use the existing protected native `entity_claim_or_load` for a stable,
server-admitted delivery Entity identity. The TKL adapter owns its validated
correlation-to-native-key mapping. Preserve ordinary create/read authorization
on both branches; do not use a query-then-create race, custom SQL uniqueness
table, shadow registry or content hash. Prove this mapping and caller isolation
through the actual Entity implementation before claiming idempotent prepare.

Preparation retains the complete admitted `textus.material-contribution/1`,
logical resource order, server-selected destination binding and identities
needed for subsequent work. On an existing claim, return its actual state
without another Material registration or destination selection. Reusing a
requestId with a different contribution is a structured conflict, not an
overwrite. This is request admission, not an artifact-immutability certificate.

After a lost prepare reply, Core calls findForRequest before another mutation.
After it knows deliveryRef, it uses get. A confirmed absence may permit another
prepare with the same correlation; query failure/uncertainty does not. TKL owns
the native Material registration identity and logical package location. They
are selected and durably associated with the delivery before materialization,
so finalization cannot issue another registration after a lost acknowledgement.

## Original transfer and registration

`prepareOriginalTransfer` authorizes the owner and declared original, persists
the busy transition before native provider session initiation, then checkpoints
the session privately before returning an instruction. Its result contains
`delivery` and optional `instruction`: transferRef, resourceId, method, uploadSessionUri,
mediaType, byteLength and nextOffset. The URI is narrow provider upload authority,
returned only to this authorized transfer client, without a server key or general
Google credential. Its session field uses the native nested confidentiality
redaction; an enabled real ActionEngine calltree must omit the URI. It is
excluded from passive status and ordinary diagnostics.
Repeated preparation inspects the retained session; Completed checkpoints the
provider file, Pending returns its observed offset, and actual Expired marks
reconciliationRequired. There is no arbitrary local expiration or new-session
retry after an unknown initiation.

Core uploads bytes directly to the provider. It privately checkpoints the safe
transferRef/resourceId/length before sending `confirmOriginalTransfer` with
`result={outcome:"uploaded",byteLength:<declared length>}`. TKL inspects the
retained reservation, including an independent known-file metadata GET; client
metadata and session checksum alone never confirm verification. Provider identity,
name, MIME, exact parent, root observation and length must match preparation. Pending remains
unconfirmed; expiry requires reconciliation. A lost client upload response is
resolved by another preparation/status inspection of the same session.

`uploadOriginal` remains the server-mediated route and rejects deliveries
prepared for direct transfer. Direct preparation/confirmation reject mediated
deliveries. Capability selection precedes transfer: 401/403, network errors and
unknown outcomes never authorize route changes, duplicate creates or a new
physical destination. Current policy changes cannot retarget existing work.

Authorize the delivery and select an exact declared original before provider
access. The client supplies no provider file ID, folder, URL, owner or server
source path. Validate actual media and known length against the retained
declaration. TKL owns physical references; byte length is not a byte-integrity
claim. Existing direct-Drive receipts remain provenance and are not silently
relabeled or treated as a grant for this ingress.

Persist the relevant native transition before a remote write and retain each
confirmed resource result before advancing. Known uploaded resources are not
uploaded again. Repeated requests for confirmed work return actual progress
without overwriting bytes. Native revision conflicts are reconciled by read;
they do not authorize concurrent provider attempts or unconditional updates.

The existing create-only FileStore/package writer cannot by itself resume an
uncertain physical write. If its outcome cannot be resolved from exact retained
provider information using existing facilities, expose reconciliationRequired
and preserve local/remote originals. Do not search by display name, repeat a
possibly successful create, delete partial files or fabricate an uploaded
checkpoint. The resource-level checkpoint/materialization integration remains
an implementation obligation, not a guarantee supplied by this document.

Finalize requires every original and derived document association. It reuses
the neutral package/manifest and native Registered metadata/read model. A
planned URN or successful byte upload alone is not a registered Material.
If finalization acknowledgement is lost, read the same delivery/registration;
return actual Registered when saved, otherwise retain the unfinished or
uncertain state without creating another registration or replaying the old
one-shot materializer. Only confirmed Registered exposes materialRef.

## Configuration, bootstrap and recovery

Explicit test configuration/injection is permitted by the user (2026-10-24).
A test host may select its native provider connection and dedicated destination;
a client harness may inject the public port to exercise defined states. Identify
fixture responses as such. Production Core obtains integration information from
TKL's getDestination/prepare contract, rather than requiring users to supply
physical storage settings. Bootstrap identifies/authenticates the TKL service.
An unavailable production service never triggers a test-config fallback.


For the first Shared Drive route select server-mediated transfer with the
existing CNCF-managed service-account connection. Keep its secret reference and
key exclusively on the server. The currently inherited read-only connection
is not an upload configuration. Select a separate explicit write-capable
connection, target folder and native HTTP method admission before real writes;
retain the old read connection and do not widen its scope implicitly.

Provide Android the normal HTTPS service bootstrap and application credential
through deployment/secure platform storage. A server allowed-subject entry is
not proof that an Android request authenticates as that subject. Bind and
verify the actual native ingress authentication independently. Do not reuse an
ended test listener, temporary bearer, removed fixture or ephemeral test database.

Support Workspace Shared Drive and My Drive shared folders through these same
logical Operations. Their server authorization adapters can differ: a service
account Shared Drive route does not prove user-OAuth My Drive behavior. No
failure silently changes target or enables mobile physical-ID configuration.

New deliveries use current configuration. Existing ones retain the original
binding; TKL either continues it or reports a required explicit action.
Credentials/session renewal follows CNCF/provider facilities, without an
invented application TTL. Offline Commit, queue ownership and restart remain
Core responsibilities. Preserve legacy queue entries and all local originals.
Upload status, TKL registration and Studio acknowledgement are separate facts;
this result never completes the existing Studio acknowledgement slot.

## Required integrated verification

Implement these Operations with their behavioral specifications as one connected
delivery: caller isolation; same-request prepare after lost reply; ordinary
binary original ingress; native persistence/restart; partial and uncertain
physical outcomes; finalization reply loss; configuration change; normal
Material get/read after registration; and preservation of legacy pending work.
Generated input admission passes its focused checks; actual HTTP output,
durable runtime, provider execution and connected acceptance remain required.
Use one real Shared Drive folder for device acceptance; My Drive fixtures are
reported as fixtures. Existing source contracts and this specification do not
establish those results. No per-helper full review or full-suite loop is needed.
## Native HTTP input boundary

For HTTP calls, exact business-field admission uses the original HTTP query or
form. CNCF augments the generated Request with transport properties such as the
Authorization header; those properties are not business DTO fields. Every
admitted source field must agree with exactly one generated operation property.
GET accepts only query business fields with no form/body; POST accepts form
business fields with no query. Duplicate, extra and inconsistent fields fail
before resource access. An `authorization` or `cookie` field supplied in the
query/body remains an unknown field, including when its value is JSON null.
Non-HTTP Request admission continues to require exactly the declared properties.

Executable specification: `MaterialDeliveryHttpInputSpec`.


## DU-02 immutable expectation and independent integrity gate

Authoring date: 2026-10-25. This server boundary is authored for parent validation
and independent review; it supplies no new live, deployed, device or Phase acceptance.

`prepare` takes the same two inputs. Each original may add exactly one optional
`contentDigest: {"algorithm":"sha256","value":"<64 hexadecimal characters>"}`.
Absent means legacy; explicit null, wrong algorithm, malformed value or inner
unknown keys fail contribution admission. CNCF's checked SHA-256 factory normalizes
the typed digest to lower case. The complete raw contribution retains its exact
JSON, including original digest spelling, opaque source/provenance/document trees
and nulls. Expected length remains `byteLength`. The first authenticated native
claim fixes the complete expectation; changed contribution/digest at the same
caller/requestId conflicts before provider IO. Completion supplies no expectation.

New direct delivery requires every declared original's digest before any claim,
write or provider effect. Partial digest sets fail on both routes. All absent
digests remain valid for explicit or capability-selected mediated delivery. Empty
original vectors require no binary hash. Direct selection requires matching
`ReservedDirectUploadFileStore`; plain FileStore and old DirectUpload-only stores
select `tklMultipart/providerDirectUploadUnsupported` before any session.
Explicit `preferDirect=false` retains `operatorSelectedServerTransfer`.

The existing private Entity JSON appends reservation file reference and observed
storage root to each transfer; old eight-field sessions remain readable as legacy.
The upload URI and these physical bindings are private. The caller still receives
only transferRef/resourceId/method/uploadSessionUri/MIME/size/offset. Preparation
checkpoints the busy state before provider IO and complete reservation before the
instruction or first inspection. Unknown preparation outcome stays busy and never
automatically generates another ID, session or object. Pending offsets do not place
or verify bytes. Expired sessions retain reconciliation state.

`files` records independently confirmed placement, with optional retained root.
Old three-field entries read with absent root. `.originalUploaded` means placement.
A separate ordered private verification vector records `pending`, `verifying`,
`verified` or `failed`, attempts 0 through 3, fixed reason, observed checked digest
only when actually present, and the independently observed file/root binding.
Restore checks resource order/uniqueness, declaration/session/file binding, proof,
attempt/state/reason consistency; file presence never manufactures verification.
Expected digest is read only from the retained contribution.

`confirmOriginalTransfer` still accepts exactly
`result={"outcome":"uploaded","byteLength":<declared length>}`. Null, extra file IDs,
checksums or other claims fail before effects. Reserved inspection confirms the
retained ID, exact parent/root, MIME and length with independent metadata; no
FileStore.read or original download occurs. Once these match, candidate placement
is saved before verification. Equal independent SHA verifies; different SHA records
`failed/integrityMismatch`; absent SHA records `verifying`, then after the third
independent check records `failed/verificationUnavailable`. A session SHA cannot
override missing or different independent SHA. Metadata identity/taxonomy failures
propagate safely with no route fallback, deletion or recreated session.

`finalize` is also the bytes-free recheck command. Before any derived document,
manifest or registration write it makes exactly one new independent stat per
placed, digest-bearing, eligible original. Initial confirmation/upload checks and
finalize share the same three-check total. Verified/failed entries are skipped;
one resource's failure never erases another's verified proof. Unfinished results
return truthful `verifying` or `verificationFailed`, without manifest or registration.
Before reserved inspection or an eligible candidate stat, native revision CAS
claims the existing private `busy="verification:<resourceId>"` checkpoint from
an empty busy state. A competing command cannot replace existing verification,
registration or unknown-write ownership. Known Pending releases its ownership
without placement, proof or an attempt, retaining the same instruction/offset.
Completed placement retains ownership until the known missing/equal/different
SHA observation and its single attempt are saved together with busy cleared.
Expired retains the existing expired-session reconciliation checkpoint.
Provider failure, lost response/process, invalid metadata or failed checkpoint
save leaves ownership unresolved and blocks automatic inspection replay. Thus a
lost third actual GET cannot trigger a fourth, even when only two observations
were durably counted. Attempts are never decremented and unknown observations
never manufacture proof. Already placed direct resources keep their passive
idempotent projection; retained busy still reports reconciliationRequired.
All required digests must verify before package readiness or registration. Actual
already-Registered legacy delivery returns its retained registration read-only,
without a retroactive SHA claim. Unfinished legacy direct sessions lacking digest
or reservation are `reconciliationRequired/actionRequired=integrityUpgradeRequired`;
they issue no URL and cannot bypass the gate through an old manifest.

For all-digest mediated delivery, create returns a server-owned placement/root
checkpoint before independent stat. Repeated upload of that candidate does not
create again or relay its body merely to recheck SHA. Finalize uses the same bound.
No-digest mediated delivery keeps prior semantics and makes no integrity claim.
Service-account and delegated OAuth connection selection/ownership stay native;
this boundary changes no credentials, configuration, scopes or live permissions.

Available destination status adds `contentIntegrity:"sha256"`. A digest-bearing
resource adds structured JSON `contentDigest`, `verificationState`, integer
`verificationAttempts`, and optional fixed `verificationReason`. Legacy resources
retain their old four fields. Snapshot failures add `actionRequired` as
`integrityMismatch`, `verificationUnavailable` or `integrityUpgradeRequired`;
`materialRef` exists only for actual registration. Ordinary status includes no
physical file/root IDs, session URI, provider body or credentials. Safe diagnostics
do not echo digests. The operation names and inputs are unchanged; CML declares
optional json/string/int outputs, with no generated Scala edits or ninth operation.
The native Record projection carries contentDigest as a nested Record with exact
algorithm/value fields; checked `RecordEncoder.jsonC` specifications require the
same JSON object on the wire. Private document and opaque JSON trees retain
their existing representation. No JSON string workaround or field alias is added.

Executable chapters: MaterialContributionSpec (40 generated valid digest cases and
malformed presence), MaterialDirectDeliveryOperationsSpec (restart with retained
expectation/reservation/proof), MaterialDeliveryIntegrityOperationsSpec (grouped
admission, independent verification, bounded recheck/restart and compatibility),
and MaterialDeliveryRouteSpec (old capability fallback). Native fixtures use real
isolated PostgreSQL, generated Operations, normal Entity authorization and controlled
HttpDriver observations of synthetic bytes. They establish no real Google proof.
Ownership scenarios cover known Pending, loss after the third actual GET and a
one-shot competing generated finalize in the same native session. Legacy
Registered setup assertions reconstruct registration through beginC/packageC/
completeC and compare exact JSON with an independent JDBC registration row
before reopening. These diagnostic assertions do not establish or repair the
unresolved historical legacy setup mismatch; parent validation remains required.
