# TKL-managed mobile Material delivery

Decision: 2026-10-24. Status: agreed specification; implementation pending.

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
   instructions. Direct Drive upload is a physical optimization, not permission
   for the client to construct Lake locations.
4. Report transfer results and finalize registration through TKL. TKL returns
   the Material reference/URN and the actual registration outcome.
5. Resolve an interrupted or uncertain delivery by its existing reference before
   deciding what remains to retry. Preserve completed resources and local originals.

Uploading photo/audio bytes alone is not registration of a complete Material.
The contract must include committed metadata, resource associations and provenance.
TKL registration and any subsequent Editing Studio server acknowledgement remain
separate facts. Neither implies Candidate formation or Information admission.

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


## Planned content-integrity extension

The [direct-upload integrity plan](../../../../Project2026/nict-editing-studio-app/docs/notes/direct-upload-integrity-plan.md) adds
server-service-account Shared Drive new-file reservations and SHA-256
verification before registration to the existing route. This is an explicitly
authorized content-integrity use, not a management-hash ledger. Existing
metadata/length-only confirmation does not establish this new acceptance.
Status remains planned for this delta; preserve prior evidence and active work.
