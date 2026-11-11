# Participant Material access v1

Status: normative opt-in source contract; composed qualification and deployment
are separate pending gates. The ordinary knowledge-editing route remains
Studio -> Workbench -> TKL. The accepted App-to-TKL intake/direct-byte route is
a separate physical-transfer path, not a second Knowledge Lake storage policy.

## Server-owned same-origin trust

The optional Component setting `material.participant.configuration` selects the
closed `textus.material-participant/1` profile `same-origin-user-account`.
Absent configuration preserves existing legacy/service admission and adds no
authentication grant. A present malformed/empty/unknown profile or missing/null/
wildcard trusted-provider field fails component initialization. Clients cannot
select a profile or provider with request data, a URL, principal attributes or
public case-class products.

The server pins exact CNCF `AuthenticationProviderIdentity` names:
`bindingName`, `providerName`, `componentName`. Admission requires native Provider
origin and that selected identity, SubjectKind.User, a current access-purpose
SessionContext with nonempty sessionId, and physical credentialTransports with
bearerHeader=true, sessionHeader=false and sessionCookie=false. The selected
provider owns credential/session authenticity, expiry and revocation. TKL never
copies its session store or interprets opaque UserAccount IDs. A refresh token,
other issuer/provider, trusted-local or copied context, absent transport proof,
and Bearer mixed with known session header/cookie cannot enter this profile.

Admission runs before datastore/provider effects at delivery_subject, Material
mode/catalog and registration-principal boundaries. It supplements existing
native Operation/Entity authorization, destination allowedSubjects and persisted
owner binding. Operators explicitly admit permitted native subjects in the
existing destination/account connection policies; no wildcard enrollment is
introduced. Material/delivery IDs from clients select data but never authority.
Reserved file/destination/account bindings, incomplete and unknown outcomes,
provider verification, read limits and TKL-mediated fallback remain unchanged.

## Native REST contract for the client

The configured same-origin host cohosts real UserAccount authentication and TKL.
The client uses its existing UserAccount ACCESS Bearer token; CNCF selects that
provider from trusted wiring. URL equality alone establishes no trust. The base
path is `/rest/v1/knowledge-lake/material-delivery`; ordinary automatic selectors
use the operations below (kebab-case URL spelling). The actual composed native
HTTP endpoints/methods require qualification before a deployed address is handed
off. No persistent/deployed URL is asserted by this source contract.

| Method | Operation / selector | Business input |
| --- | --- | --- |
| GET | getDestination / get-destination | none |
| POST | prepare | requestId, contribution |
| GET | findForRequest / find-for-request | requestId |
| GET | get | deliveryRef |
| POST | uploadOriginal / upload-original | retained delivery/resource and bounded original content |
| POST | prepareOriginalTransfer / prepare-original-transfer | deliveryRef, resourceId |
| POST | confirmOriginalTransfer / confirm-original-transfer | retained transferRef/resource and actual transfer claim |
| POST | finalize | deliveryRef |

Material search/get/original reads and registration commands retain their
existing `/rest/v1/knowledge-lake/material/...` contracts and the same admission
when this profile is configured. Native Operation permissions remain independent
of Material/resource ownership. An allowed command cannot use a foreign user's
reservation, finalize it or disclose its original. Reconcile existing uncertain
transfers through their retained reference; denial/failure never grants replay.

The UserAccount token is sent only to the same-origin server as Authorization.
TKL's separately managed Drive connection keeps server credentials private.
Direct transfer instructions contain only the narrowly scoped existing upload
capability. Never forward the UserAccount Bearer to Google, expose a service-account
key/general credential, or log upload-session URIs/tokens in ordinary status.
Provider/root/checksum observations are not trust or registration evidence.

## Operator template and evidence

TEMPLATE ONLY — replace names with the actual resolved cohost binding after
provisioning; this is not a working deployment or account enrollment:

```json
{
  "schema": "textus.material-participant/1",
  "profile": "same-origin-user-account",
  "trustedProvider": {
    "bindingName": "<actual-resolved-binding>",
    "providerName": "<actual-provider-name>",
    "componentName": "<actual-provider-component>"
  }
}
```

Keep this JSON in server-owned Component configuration, supply existing private
storage/connection settings separately, and retain each destination's explicit
allowedSubjects. Dev/test isolate synthetic owners and fake providers. Participant
demo uses private provisioned storage and actual owner authentication; reset must
never touch participant originals. Production requires its own qualified bindings.
This work neither provisions Google, enrolls all accounts nor changes any ACL.

MaterialParticipantAdmissionSpec uses controlled synthetic provider bindings through
actual in-memory Http4s/native ingress, public-copy and shrinking mismatch controls.
MaterialParticipantOperationsSpec adds two-user PostgreSQL owner/effect controls
with fake Drive. These are source specifications, not production UserAccount or
live Drive evidence. Native compile/focused13/full, independent integrated review,
local acceptance/CAR delivery and later real UserAccount/TKL/Studio HTTP plus
PostgreSQL composition remain required before App adoption.
