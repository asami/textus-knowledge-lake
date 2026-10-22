# M03 logical registration operation design

Date: 2026-10-21; public contract promoted 2026-10-22
Status: proposal retained as rationale; settled public contract declared, runtime and acceptance pending
Scope: existing TKL-M03 contribution to Studio server ESS3-02

## October 22 public contract promotion

The settled normative [Material registration Operations v1](../spec/material-registration-operations-v1.md)
now defines `createMaterialRegistration`, `getMaterialRegistration` and
`materializeMaterial`, with matching declarations in
[the owning CML](../../src/main/cozy/textus-knowledge-lake.cml). The initial
proposal and source-inspection reasoning below are retained as history, not
independent normative authority. In particular, the original suggestion to
return logical metadata from materialization is superseded by the exact
four-field `MaterialRegistrationResult`: native registration ID, committed
revision, state and planned Material reference. Metadata remains `getMaterial`'s
responsibility after registered status.

The adopted contract fixes one owning-host registration configuration, explicit
registered mode, exact allowed authenticated subjects, selected source and
destination parents, and a single TKL-managed connection/store. The initial
`nict-book-material-transfer/1` adapter alone interprets the retained App
original/receipt correspondence. Its stat/read checks enforce the selected
source parent, media and length; `accessConnectionId` remains provenance.
Registered-read enablement remains a separately configured alias mapping.
Native durable acknowledgement, actual revisions, authorization and no-replay
semantics follow the existing native lifecycle. No credential or ACL change
has been performed by declaring this contract.

Actual runtime/service/configuration/source implementation remains pending.
Normal generation and inspection of its actual DTO/action/factory API precede
that implementation; no generated API is inferred here. Fixed Workbench public
forwarding, Studio immutable-source conversion/action, public-operation real
PostgreSQL specifications, composed original access, packaged/real Google
evidence, independent integrated review and the original whole Phase gates
remain required. Contract promotion is not successful execution or acceptance.

The [package contract](../spec/material-registration-package-v1.md) supplies a
neutral contribution, TKL-owned location/manifest and create-only physical
writer. It deliberately does not register a Material. This proposal records
the remaining connected design, rather than treating physical success as M03
completion. Promote the settled public contract to spec/CML before implementing
its Operations. Operational setup and chronology remain in journal.

## Connected route and identities

The normal work route remains Studio -> Workbench.MaterialWork -> TKL.Material.
Studio authorizes and loads its native Book editing source, converts the
immutable admitted packet to a neutral contribution, and invokes fixed public
Workbench work Operations. Workbench uses fixed public TKL selectors with the
same caller context. Neither consumer knows the Lake's physical layout, chooses
a Lake folder, interprets a TKL URN, receives a provider credential or calls a
private TKL class. Reads continue independently of OpenClaw and registration.

A native `MaterialPackageRegistration` record would describe the incoming
contribution and its registration outcome. Its issued native Entity ID is a
registration identity, not the originating App's Material Entity ID and not
the package URN. No legacy App request/subject ID is converted into a native
Material ID. Retain a real originating native Entity reference as provenance
only where one actually exists.

TKL selects the explicit configured lake/logical parent and issues the leaf
through the existing purpose-aware execution-context `opaque_id`. Persist the
selected location before any physical write; no client-chosen logical leaf,
hash, business-key-derived Entity ID or production lake default. Native Entity
issuance remains with CNCF. Canonical `material.json.id` stays the TKL URN.

## Original proposed bounded public work operations

The following table retains the original proposal; the promoted specification
above settles the bindings and result shape, without claiming executable availability:

| TKL Material operation | Meaning |
| --- | --- |
| `createMaterialRegistration` | Validate producer/contribution/source binding, authorize native creation and persist a Prepared registration. Return its native registration ID and status before any physical write. |
| `getMaterialRegistration` | Load the caller-authorized registration and return its actual status and logical references; no provider access or preparation side effects. |
| `materializeMaterial` | Load and authorize that registration, admit only its Prepared state, persist the native transition to Materializing, execute one package writer, then persist Registered plus confirmed private provider mappings. Return logical Material metadata only after confirmed persistence. |

Workbench would expose corresponding fixed-purpose work commands/query, not a
general operation proxy or another registration database. One Studio user
action may call creation and then materialization exactly once, retaining the
returned registration ID for status inspection. This is not a new mobile
queue, Candidate lifecycle, polling scheduler or outbox.

The registration row contains the complete contribution, selected logical
location, server-owned principal metadata, native revision and status. Only a
Registered row is an available Material. Prepared/Materializing records must
not enter ordinary available search or supply an original. Native owner/access
rules apply before status disclosure, credential lookup or provider IO.

## Failure semantics and native persistence prerequisites

There is no atomic transaction spanning PostgreSQL and Drive. A physical
manifest confirmation is not a native registration commit. If provider output
or final native persistence is uncertain, keep the registration non-available
until its actual stored state is inspected; do not repeat the writer, delete
remote bytes as rollback or call the attempt successful from a provider receipt.
Materializing is a retained attempted/possibly-unconfirmed state, not proof that
a worker is still running. A restart never automatically replays it.

The record/state is domain information required for crash-consistent external
effects, not application-side writer arbitration. Use existing native Entity
create/load/update and their declared storage revision contract. Add no claims,
leases, management hashes, custom compare-and-swap protocol, competing-writer
retry or distributed lock. A revision selects the actual stored data version;
it is not a substitute for native persistence or source authorization.

Source inspection identifies the applicable existing facilities:

- `ActionCall.entity_create`, `entity_load_snapshot` and native observed-revision
  `entity_update` preserve normal caller authorization; do not use service-
  internal bypasses or private capture-outcome APIs.
- `StandardEntityCreatePart.create` uses the native `createEntity` plan.
- PostgreSQL `SqlDataStore.createEntity` uses `_with_entity_create`; versioned
  mutations use `_with_versioned_mutation` and native commit acknowledgement.
- `UnitOfWorkInterpreter.run` can subsequently abort its outer UnitOfWork on a
  failure. A program expression alone therefore does not prove that the earlier
  registration state is durably visible after the external effect fails.

Before freezing the implementation, trace the exact create/update return path
through the current CNCF provider contract. The connected executable
specifications must prove with real PostgreSQL that Prepared and Materializing
survive the relevant later failures/outer abort, and that a second explicit
materialize of a non-Prepared record causes no provider write. Also exercise
unknown native commit acknowledgement and final-write-then-error. If the public
native capability is insufficient, report that exact CNCF requirement; do not
invent a TKL shadow persistence/reservation mechanism.

## Source contribution and OAuth custody

Studio's conversion retains all original entries, including unselected images,
and complete packet/review histories as neutral documents. The originating
`materials` entry can remain an opaque complete `sourceReference` object. It
is not a TKL resource binding or an access grant. Producer-specific provenance
must not prescribe a physical output path.

For existing App receipts, the TKL-owned ingress adapter may interpret the
physical receipt vocabulary only under an explicit trusted producer binding.
The current Studio compatibility mapper reads `receipt.providerId`,
`receipt.accessConnectionId` and `receipt.remoteId`; this locates the migration
edge, not authority to trust an arbitrary receipt. The proposed TKL binding
must select the supported receipt schema, exact authenticated subject/producer,
expected source parent and TKL-managed connection. Before bytes are read,
validate the exact stat parent, kind, MIME and known length. A stronger Google
credential or a client-supplied owner/source Entity ID cannot grant source
access. This is an explicitly bounded source-storage proxy, not arbitrary
Drive browsing or URL fetching.

The grant remains CNCF-managed under the KnowledgeLake component and actual
authenticated owner. Use the existing TKL OAuth management/callback route;
do not copy a Studio grant, export tokens, relabel the component, add a
privileged identity bridge or require a second legacy Studio grant for the
normal path. Host source/destination settings are nonsecret references. Actual
consent/account selection remains a human action.

## Ordinary reference projection

Registration must feed the existing Material search/metadata/original route.
Use the native searchable datastore, owner filtering, stable ordering and
paging; do not introduce a component-local index or completeness scan. Resolve
an exact issued URN through its stored registration relation and fail explicit
ambiguity rather than picking a row. Keep provider mappings private to TKL.

The existing read-only bootstrap catalog remains a distinct explicit mode.
The managed registration mode must not silently merge, shadow or fall back to
bootstrap data. Bind an explicit mode/configuration contract before changing
the existing runtime. Preserve existing configured bootstrap behavior and its
accepted scope; a new registered route still needs its own restart proof.

## Remaining implementation and acceptance work after promotion

The settled public contract/CML is now declared. Run normal generation and
inspect the actual generated DTO/action/factory API before freezing the owning
runtime/service/configuration/source implementation and public-operation real
PostgreSQL specifications. The native registration kernel and explicit
registered metadata/original adapters are already authored under their own
contracts; the public registration ingress and its host-selected source still
need implementation. Fixed Workbench forwarding and Studio Book immutable-source
conversion/action follow. The old Studio direct package endpoint remains an
explicit compatibility path until a separately specified migration, never a
fallback for failure in the new logical route.

Focused acceptance must cover real PostgreSQL state/readback across restart,
caller/owner denial before provider access, receipt-parent mismatch, no replay
after unknown outcomes, logical search -> metadata -> exact original, and the
Workbench/Studio public dispatch with unchanged structured failure information.
Retain one integrated protected Step review and the original final full gates.
Actual Google/App and private packaged-runtime evidence remain independent
requirements; neither this proposal nor a synthetic provider proves them.
