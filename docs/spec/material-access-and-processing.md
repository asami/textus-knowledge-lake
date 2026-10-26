# Material access and processing component contract

Status: agreed direction; public operation bindings and implementation pending
Decision date: 2026-10-21

## Mobile delivery configuration amendment — 2026-10-24

[TKL-managed mobile delivery](tkl-managed-mobile-material-delivery.md) defines
TKL as the source of destination configuration and upload/registration
instructions. Android does not define Google folders or Lake layout. Both
Workspace Shared Drive folders and My Drive shared folders are supported; the
first live acceptance targets Workspace. This extends registration and the
physical-transfer exception below without changing Workbench's editing role.

## Responsibility

TKL is the logical Knowledge Lake component. Storage is not the Knowledge Lake
interface. Google Drive is the initial physical provider, not the public domain
model. TKL owns Material identity, URN resolution, metadata, logical locations,
raw/prepared relationships and mapping to physical resources.

One TKL component provides two service surfaces sharing those domain facts:

| Surface | Client and behavior |
| --- | --- |
| Material access | Workbench queries/searches Material, resolves references and obtains original access information through public Operations. Ordinary reads require no OpenClaw registration, running worker or continuation exchange. |
| Knowledge Lake processing | TKL is registered with OpenClaw through the continuation protocol for collection/preparation/organization work. Correlated requests/results resume TKL processing and update the same Material model exposed by the access surface. |

Services may run in one host or separate deployments. One component contract
does not imply singleton process state. Published processing results must be
visible through the same persistent Material identity/metadata used by queries.
Running preparation remains distinguishable from an available result. A read
never silently starts preparation or requires AI merely to return stored data.

## Logical route and composition

```text
Editing Studio / other knowledge editing apps
  -> Workbench public work operations
    -> TKL public Material operations
      -> Google Drive / other physical providers

TKL processing <-> OpenClaw continuation protocol
Workbench -> KnowledgeHub admission after human approval
```

Workbench links/composes the TKL component and calls its public Operations via
CNCF. Same-host composition is the initial direction; a mandatory REST hop or
separate server is not required. Workbench must not call TKL private storage
classes. CNCF carries execution context and applies Operation/resource access
policies; linking components does not grant service authority.

## Minimum access capabilities

The following are semantic requirements, not invented executable selectors or
final DTO declarations. Bind exact typed inputs/results against CNCF before
implementation.

- Search Material using domain criteria and normal paging; return logical
  references and summaries, not folder listings.
- Resolve an existing Material reference/URN to metadata, provenance and
  available raw/prepared resources. A client preserves the reference rather
  than constructing or parsing provider locations from it.
- Obtain access to a selected original/resource, with media information and
  an explicit available/unavailable result under the caller's permissions.
- Register/materialize incoming Material when replacing the current Studio
  package-writing path; TKL assigns/resolves identity and manages physical
  placement and the registration outcome. File creation alone is not logical
  registration. Bind repeat-attempt and ambiguous-outcome behavior to existing
  storage/operation contracts; do not invent hashes or a comparison ledger.

Two different stored bodies at one logical location must not silently become
two indistinguishable answers to a single Material reference. Define the
identity/version relationship in TKL, not independently in each editor.

## Physical transfer exception

For large originals, TKL may supply access/upload information and bytes may move
directly between the client and Drive. Workbench can pass that information to an
editor. The transfer adapter may understand the physical protocol; the editor
does not infer file IDs from URNs, select Lake folders, or construct raw/prepared
layouts. TKL retains logical identity, metadata and resource correspondence.
Completion/registration is reported by the owning TKL operation, not inferred
from an upload response. Credentials and access information follow existing
CNCF/provider policies, without inventing a new token or expiry system here.

## Processing and lifecycle

Use the existing CNCF continuation facilities for OpenClaw registration,
delegated work, result submission and resumption. Bind the actual protocol
before implementation; no ad hoc polling/continuation framework is authorized.
TKL owns processing facts, preparation outputs and provenance. Workbench owns
Information Candidate identity, human formation/review/approval and admission
requests; KnowledgeHub owns canonical Information. A TKL processing result is
not human approval and cannot bypass Workbench to admit Information.

Material -> Information Candidate -> Information -> Knowledge is the conceptual
lifecycle, not a requirement for every component to implement every transition.
TKL can produce proposals/references for Workbench without becoming a parallel
Candidate lifecycle store.

## Acceptance scenarios

- Search, resolve and obtain an original through the access surface with no
  OpenClaw worker running; no processing request is created by those reads.
- Resume one registered OpenClaw processing request and expose its completed
  result through the same Material reference/metadata and provenance contract.
- Workbench consumes public TKL operations without Drive folder knowledge;
  another domain editor can use the same Workbench operations.
- Unavailable/unauthorized resources remain explicit; no provider fallback in
  Studio or Workbench and no success inferred from a provider receipt alone.
- Direct transfer, if selected, preserves TKL-managed identity/registration.

Development sequence: [Material access and processing plan](../phase/material-access-and-processing-plan.md).
