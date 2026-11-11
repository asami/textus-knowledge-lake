# Native mobile direct and mediated delivery

Status: implementation in progress; no TKL/App Phase acceptance, live provider
result or committed release is claimed.

App Phase 6.3 adds MaterialDelivery beside the existing Material APIs. Its
eight Operations use normal authenticated caller/Entity authorization, native
claim/load/update and the component application datastore. The selected
destination, transfer method/reason and native registration identity are retained
before remote effects. Busy checkpoints preserve unknown provider outcomes;
completed resources are not created again. Normal Material get/read resolve the
registered result after restart, even without current delivery configuration.

Direct transfer is preferred through CNCF's optional DirectUploadFileStore.
A provider lacking that capability selects tklMultipart with an explicit reason.
The server operator may also select mediated transfer deliberately; this is
distinct from capability absence. Authorization/network failure and unknown
effects never change an existing delivery's method or destination.

prepareOriginalTransfer creates or inspects the retained per-original session,
returning current progress and an ephemeral instruction. Core sends the bytes
to the provider and checkpoints a safe receipt before notification. TKL checks
the actual provider session against retained name/MIME/parent/length, independently
of the client's success assertion, then allows finalization. Reply loss reuses
the same session; actual expiry requires reconciliation. Mediated upload rejects
direct deliveries, and direct Operations reject mediated deliveries.

The authorized instruction includes uploadSessionUri, without a server key or
general bearer. Native nested session redaction excludes it from a real enabled
ActionEngine calltree; passive snapshots contain no URI. Private sessions stay
inside the owning delivery document. Native RecordResponse JSON is camelCase;
Core also reads its retained snake_case projection without renaming opaque JSON.

Verification: 19 focused native integration/input/reference cases passed,
followed by the full owning suite: 195 tests across 14 suites passed. These use
isolated PostgreSQL and controlled native providers, including independent
checkpoint reads and real authorization/interpreter paths. Final source ABI
matches generated metadata (14 total Operations, 26 values). SBT results have
parent-verified typed receipts and released serial locks. The approved CNCF
SNAPSHOT dependency refresh is local availability evidence only.

App/Core/TFAF queue and UI integration is connected and locally tested. Owned
HTTPS/ingress bootstrap, actual Shared Drive permissions/session delegation,
Android end-to-end acceptance and the connected independent review remain.
No original was deleted, no old queue was reset, no server producer was edited,
and no remote publication/deployment or Git commit/push occurred here.
