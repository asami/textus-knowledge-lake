# Native multipart input boundary — 2026-11-06

The parent supplied StudioQualifiedDemoHttpSpec line 163 evidence: the native
mediated tklMultipart route returned HTTP 400, while the direct route passed in
the real HTTP/PostgreSQL/synthetic Drive harness (175 of 176 focused examples).
That observation establishes a mediated-route failure; it does not alone identify
the rejecting layer.

Source inspection established the incompatible representations. Accepted CNCF
ca073867 Http4sHttpServer._multipart_form_entries_from_request emits content as
MimeBody plus content.filename metadata. Public protocol Ingress.recordToParameters
classifies fields absent from the generated operation definition as named
Arguments. UploadOriginalOperation declares only deliveryRef, resourceId and
content as properties. TKL MaterialDeliveryInput required all raw fields to match
properties and rejected every argument; MaterialDeliveryOriginalInput repeated
the empty-argument requirement. The previous input example supplied filename as
a non-HTTP property and therefore did not reproduce this native classification.
This is source-based causal reasoning for the observed 400, pending native
execution of the corrected route.

The scoped change projects only the optional HTTP POST content.filename named
Argument after exact original-form correlation and unique carrier checks. The
owning original decoder consumes that admitted projection. Non-HTTP argument
rejection, required properties, duplicate and unknown input rejection, switches,
filename validation, binary checks and authorization wire checks remain explicit.
An orphan allowed property is rejected when absent from the raw HTTP form.
Public-ingress specifications cover the generated classification, bytes/MIME,
missing or inconsistent metadata, duplicates, malformed filenames and injection;
non-HTTP specifications retain property compatibility and strict arguments.

Only the two input sources, their two executable specifications, the mobile
contract and this journal are assigned product paths. No generated API/schema,
CNCF/core library, storage, identity, SQL or authorization policy change is made.
The private CAR readiness condition means source readiness for later parent-owned
validation and normal runtime local publication into this task's private warehouse.
It is not evidence of an actual CAR publication or consumer acceptance.

No compiler, focused suite, full suite, independent review, commit, publication or
consumer retest was executed by the implementation worker. Their outcomes remain
pending with the parent/native runtime; no completion or deployment is claimed.
