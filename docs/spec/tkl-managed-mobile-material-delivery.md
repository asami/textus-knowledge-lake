# TKL-managed mobile Material delivery

Decision: 2026-10-24; clarified 2026-10-25. Status: implementation in progress;
live acceptance pending.

The selected [mobile delivery v1 contract](mobile-material-delivery-v1.md)
binds additive Operation names, client values and native implementation
requirements for ESA63-01. Its CML contracts now generate/compile and native input admission passes focused
specifications. The native delivery runtime now supplies durable mediated
transfer/registration and direct-provider preparation/confirmation. Deployed
ingress and live provider acceptance remain pending.

## Authority and supported destinations

TKL is the authority for Material storage configuration. Operators provision
Google accounts, Shared Drives, destination folders and placement policies on
the TKL side. Mobile users do not define folder IDs, drive IDs, directory layouts,
provider mappings or raw/prepared storage rules.

Support both a folder in a Google Workspace Shared Drive and a shared folder in
My Drive through the same logical contract. The first connected delivery must
use an actual Workspace Shared Drive folder. A failure there must never redirect
Material to My Drive or another destination automatically.

Android receives the permitted logical destination and delivery instructions
from TKL. If multiple logical destinations are exposed, TKL supplies their names
and allowed choices; selecting one is not defining physical storage. Start with
one configured logical destination, without requiring a destination chooser.

## Responsibilities and route

| Owner | Responsibility |
| --- | --- |
| TKL | Destination policy, Material identity/URN, metadata, physical placement, provider mapping, upload preparation and registration outcome |
| Flutter Core | TKL client, physical transfer adapter, local credentials, durable queue, background execution, retry and status |
| Application Framework | Common connection/sign-in/consent and delivery-status presentation over Core |
| Editing Studio App | Book Material creation/Commit, application composition and domain-specific presentation |

Material capture/intake may call the TKL delivery contract directly or through
the deployed application facade. Such a facade must delegate to TKL, not keep a
second storage policy. This intake route does not change knowledge editing:
editing applications use Workbench for Material selection and Candidate work,
and Workbench uses TKL's public Material operations.

## Delivery contract

These are semantic capabilities, not final operation names or wire DTOs.

1. Resolve the authenticated application's permitted logical destination and
   supported submission requirements from TKL.
2. Prepare delivery for a committed Material, its metadata and declared resources.
   TKL returns a delivery reference and the required transfer instructions.
3. Core transfers resources through TKL or directly to the provider using those
   instructions. Prefer direct transfer when the selected provider exposes its
   optional preparation/status capability; otherwise explicitly select transfer
   through TKL and retain the capability reason. Direct Drive upload is not permission
   for the client to construct Lake locations.
4. Report transfer results and finalize registration through TKL. TKL returns
   the Material reference/URN and the actual registration outcome.
5. Resolve an interrupted or uncertain delivery by its existing reference before
   deciding what remains to retry. Preserve completed resources and local originals.

Uploading photo/audio bytes alone is not registration of a complete Material.
The contract must include committed metadata, resource associations and provenance.
TKL registration and any subsequent Editing Studio server acknowledgement remain
separate facts. Neither implies Candidate formation or Information admission.

Permission refusal, network failure and unknown effects are failures/recovery
states, not evidence that the provider lacks direct capability. They never
silently change the retained transfer method. The authorized client receives
only narrow per-original transfer instructions; TKL independently checks actual
provider completion before accepting that original.

## Test configuration and production discovery

The user explicitly permits configuration for testing (2026-10-24). The test
host may select the native service connection, provider configuration and
dedicated destination; client test composition may inject configured contract
responses. Keep fixtures distinguishable from real native/provider results.
Configuration alone proves neither Google access nor Material registration.

In production, TKL supplies the permitted integration information through its
public discovery/preparation contract. Client bootstrap may identify the service
and application authentication context, but it is not a second source of
storage policy. TKL failure must not activate a test destination. Server-side
Google credentials and physical mappings remain on the server.

## Configuration, authorization and recovery

Bootstrap may supply the TKL service address and application/account context via
deployment provisioning or an existing service connection. It must not require
mobile users to configure Google storage. Sign-in and necessary OAuth consent
remain explicit; server configuration does not grant Google access by itself.
Choose and bind the actual provider authorization route before implementation.
Never distribute a TKL service-account private key or general server credential
to Android. Direct transfer may use only the selected user's authorized access
or narrowly scoped provider transfer information.

Android can persist the minimal delivery reference, resource progress and
instructions needed for background recovery. This is execution state, not a
replica of TKL's configuration. Keep credentials in platform secure storage;
do not expose tokens or upload URLs in ordinary status/log output.

New deliveries resolve current TKL configuration. Existing deliveries retain
their TKL-owned destination binding: a policy change must not silently retarget
queued or partially uploaded Material. TKL decides whether they can continue or
must be re-prepared. Refresh provider authorization/transfer sessions through
the existing delivery, without an arbitrary application-level time limit,
content-hash ledger or whole-file comparison mechanism.

Offline capture/Commit remains available. Unavailable configuration or consent
is an explicit waiting/action-required state, not data loss or a locally invented
destination. Permissions revoked or a folder removed produce actionable failure.

## Acceptance

- No mobile storage-ID entry is needed to deliver metadata and an original to
  a real Shared Drive folder and obtain a TKL Material reference.
- My Drive shared-folder configuration uses the same contract; test both target
  types without conflating their permissions. Record live proof separately from fixtures.
- Missing access, unavailable TKL and interrupted upload/finalization preserve
  originals and never produce false success or a silent alternative destination.
- Restart/retry reconciles one delivery; configuration changes do not move it.
- Workbench can resolve the registered Material through the ordinary TKL API.

Plan: [mobile delivery workstream](../phase/tkl-managed-mobile-material-delivery-plan.md).
Parent contract: [Material access and processing](material-access-and-processing.md).
## Untrusted-client admission and access — 2026-11-02

The [UC-01..04 acceptance profile](https://github.com/asami/goldenport-cncf/blob/main/docs/spec/untrusted-client-material-acceptance.md)
refines this delivery contract.

- UC-01: keep provider service-account keys and server-wide credentials on the
  server. Core receives only the permitted user/scoped transfer capability;
  configuration/status never leaks privileged credentials or transfer secrets.
- UC-02: derive or authorize owner/tenant bindings from CNCF's authenticated
  subject and policy. Client owner fields are not authority. Apply the same
  resource policy to prepare/status/finalize, Material queries, originals and
  access-reference issuance. Cross-user references cannot retarget a reservation.
- UC-03: a completion report is a claim, not provider proof. Before admission,
  independently verify the reserved resource, expected size/content digest and
  placement under the existing integrity contract. Missing/failed/unknown proof
  cannot register the Material; a valid retry reconciles the existing delivery.
  Do not add a client-supplied arbitrary file ID or verification-proof parameter.
- UC-04: Operation access and Material/resource access remain separate. CNCF
  provides enforcement/context propagation; TKL defines Material policies and
  admission. Service-account storage access does not grant the external caller
  access to arbitrary provider files.

Accept with users A/B: a legitimate upload registers and can be read by its
authorized owner; a fabricated success with no uploaded bytes, wrong reserved
content, missing provider proof or B's reservation cannot register. Confirm
stored state and no private original/access-link disclosure via direct API.
Use provider-controlled fixtures for failure cases and reuse relevant live
evidence with its limits; do not equate fixtures with actual Drive proof.

TKL retains logical admission after EAI takes over provider mechanics in
[Phase 2](../phase/phase-2.md). The migration must retain these outcomes and
trusted provenance of verification results. No full EAI migration is required
before the current mobile route can satisfy them.

## Standard storage and configurable policy — 2026-11-02

The [Textus baseline](https://github.com/asami/goldenport-cncf/blob/main/docs/spec/small-business-security-baseline-and-options.md) makes non-public customer
storage, owner/resource enforcement and verified registration standard behavior.
TKL/operator provisioning supplies the selected storage route; mobile users do
not configure physical IDs or individual security guards.

Supported sharing/processing/access choices are business policies applied to
actual provider access and link issuance, not UI-only settings. Retention choices
must respect permanent-Material semantics and cannot silently delete originals.
External key custody/offline media require explicit provider/equipment support;
they are not properties of the current online Drive route or standard-use gates.
Standard deployment documentation identifies operator sharing/backup/restore
duties and actual qualification rather than implying configuration is evidence.

## Runtime profile isolation — 2026-11-02

Apply [shared mode defaults](https://github.com/asami/goldenport-cncf/blob/main/docs/spec/runtime-mode-security-defaults.md) through trusted common configuration.
Dev/test use isolated synthetic owners, storage and provider adapters.
Participant demo uses private demo storage and real owner authorization;
production uses its qualified real bindings. Clients never choose provider
credentials or weaken registration checks by sending a mode value.

The same reservation, authoritative verification and Material-admission contract
applies in every mode. Fixture provider results are test evidence only.
Destination/account changes preserve reservation ownership and do not replay
pending transfers elsewhere. Synthetic reset cannot delete participant originals;
post-event handling follows the declared Material retention/sharing policy.


## Collection-write reconciliation

`MaterialDelivery.reconcile` is an authenticated command taking exactly the opaque
`deliveryRef` and current `revision`. It verifies the retained delivery owner and
optimistic revision. Collection creation reserves an opaque native identity and
durably retains its complete owner/operation/parent/name binding before submission.
A confirmed observation may checkpoint that same identity. A retained native
pre-dispatch refusal can release the checkpoint for a later explicit operation;
unknown, possibly dispatched, missing and legacy observations remain blocked.
Inspection reads only the reserved identity and does not create a resource.
Provider identities and observations remain private; a display name or HTTP
status alone is never proof of absence or identity. No provider create is replayed
by reconciliation. Existing unfinished legacy checkpoints do not acquire a
reservation retroactively.

The optional host key `material.delivery.legacy-recovery-decision` is declared
for a separately owned legacy-recovery consumer. Its declaration alone does not
authorize or implement recovery; the current refusal of unfinished legacy
checkpoints remains unchanged until that consumer is implemented and qualified.

A reserved-ID confirmation identifies the retained resource only when the full
provider binding matches. A 404, malformed or incomplete response, and any
nonconfirming observation remain unknown: they do not establish zero resources.
A display-name match, including one or several same-name children, cannot identify
the attempted create. The operation performs no child-name scan to produce an
apparent zero, one or ambiguous proof. A reservation interrupted before it is
checkpointed also remains blocked; no new reservation is substituted on retry.
