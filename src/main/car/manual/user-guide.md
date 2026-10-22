# Knowledge Lake user guide

Use this guide with an already configured Studio → Workbench → TKL assembly.
The [reference manual](index.md) summarizes the public Operations; the exact
producer contract is Material reference v1 (`docs/spec/material-reference-v1.md`)
in the TKL source checkout. This is a repository-relative source reference,
not a packaged manual link.

Operator preparation remains in the primary **NICT Editing Studio** repository,
at `docs/journal/2026/10/2026-10-21-workbench-tkl-first-original-setup.md`.
This is a cross-project document
reference, not a file in the TKL repository. Consult it for registration,
bootstrap/configuration procedures and the retained execution history.

Before use, the operator needs a TKL-bound Google client and private connection
configuration, plus the explicitly selected reference mode. The default
bootstrap mode uses one selected harmless existing original and a trusted
read-only TKL catalog. Its caller must have native provider-authenticated
identity and appear in that Material's exact `authorizedSubjects`.

Explicit registered mode uses normally Registered native Entity records and
their actual owner/read permissions. Do not supply a catalog in registered
mode. The operator's private `material.reference.registered.bindings` maps each
retained storage-binding alias to `provider: google-drive`, the TKL-managed
`connectionId` and the exact stored `storeName`, under schema
`textus.knowledge-lake.material-read-bindings/1`. It supplies no file/parent
keys or tokens. An absent/empty or unmapped binding leaves original access
unavailable while authorized metadata/search remain available. Bootstrap rejects
registered binding configuration. Physical mappings and credentials remain
inside TKL. The owning registration Operations below are authored; composed
Studio registered activation still requires its remaining integration gates.

1. Establish authentication for Studio and for TKL connection management.
   Shared hosting alone does not establish identity at both components.
2. Use the existing connection management at `/web/knowledge-lake/connections`
   and explicitly complete any required OAuth consent. The callback belongs to
   TKL. Do not bridge authentication or copy/relabel an old Studio grant.
3. In Studio at `/web/nict-editing-studio`, explicitly search, select and inspect
   the logical Material, then read its selected original. Supported raster
   content can be previewed; original bytes can be downloaded. No Candidate or
   intake receipt is required for this reference read.

Consent and a connected status authorize neither the Material nor another
subject. Metadata availability also does not prove that the original is
currently accessible under the selected managed connection.

| Result | Operator next step |
| --- | --- |
| Unavailable capability or connection | Check the selected mode, owning assembly and TKL connection configuration. Bootstrap needs its admitted catalog; registered reads need the exact storage-binding alias. An absent catalog differs from an explicitly empty authorized catalog. |
| Denied | Check native caller identity and the selected mode's Material authorization (bootstrap subjects or native owner/read permissions); connection consent cannot replace it. |
| Missing Material/resource or revoked original | Check the selected logical binding and the original's continued availability. |
| Resource mismatch | Resolve the exact store/mapping, kind, persisted parent, media or known length discrepancy without selecting another file. |

Keep the native failure Conclusion. Do not pick a different original, fall back
to a direct-provider route, register from provider success or automatically
retry an uncertain write. Registered reads use the original's acknowledged
stored key and parent after native authorization, with no folder discovery or
source reread. This read route performs no preparation or M03 output.

## Explicit registration and saved status

In registered mode an admitted caller may submit one complete neutral
`textus.material-contribution/1` JSON object as the `contribution` property of
`Material.createMaterialRegistration`. Preserve its complete originals and
documents, including unselected originals, source provenance and review history.
The initial source format retains each complete App materials entry as
`sourceReference`: its `original.id`, `mediaType`, `byteLength` must match the
neutral original, and `receipt.providerId`/`remoteId` must identify its exact
Google Drive source. Receipt connection labels, client owners and URLs remain
provenance; they do not select credentials or a Lake destination.

The operator supplies strict owning-host `material.registration.configuration`
with schema `textus.knowledge-lake.material-registration/1`, storage alias `id`,
exact `allowedSubjects`, `sourceFormat: nict-book-material-transfer/1`, one
TKL-managed `connectionId`/`storeName`, explicit `sourceParentKey` and
`destinationParentKey`, `lake` and `logicalParent`. Source and output use the
same connection. Source and destination parents may coincide if explicitly
selected. This configuration grants no native Entity permission or Google
consent. The reference manual describes admission bounds; operational setup and
consent history remain in the Studio journal cited above.

Creation commits Prepared without reading credentials or contacting Google.
Retain both returned `registrationId` and planned `materialRef`: they are
different identities. Results contain only those identities, actual native
`revision` and lowercase `state`. Default JSON names are `registration_id`,
`material_ref`, `revision`, `state`. Prepared does not make the reference
available in ordinary Material search or reads.

Call `Material.materializeMaterial` with that exact `registrationId` once.
The owning component checks native read authority, Prepared state, configured
subject, unchanged storage alias/lake/logical parent and source correspondence.
Normal native write permission and acknowledged Materializing revision 2 precede
provider activity. Exact source stat/content must match the configured parent,
File kind, media and known length. Confirmed package creation and native final
save return Registered revision 3. Then obtain logical metadata with `getMaterial`
and select an original with `readOriginal`. The independent registered-read
binding must explicitly map the same storage alias to the exact connection/store;
source configuration does not create it automatically.

On any uncertain command failure, keep the returned Conclusion and native ID.
Use `Material.getMaterialRegistration(registrationId)` to inspect actual saved
state. Status requires registered mode and native read authorization only;
registration configuration, read bindings, OAuth and Google access are unnecessary.
Materializing can retain output bytes; an uncertain final acknowledgement can
already have saved Registered. Another materialize of either state fails before
provider access, including after restart. Do not automatically retry creation or
materialization, reset state or delete remote output. Every explicit create is
independent and can issue another registration. Native status and provider
receipt remain separate facts.

Absent registration configuration leaves commands explicitly unavailable.
Configured-subject, native owner/read/write, custody and provider refusals retain
their native structured Conclusions. Changing destination configuration does not
rebind an existing Prepared record. Missing consent requires the ordinary owning
TKL connection management flow; another subject's grant cannot replace it.

The Studio journal distinguishes controlled reference/browser and legacy
compatibility results from real Google/App and private packaged CAR gates.
Those operational observations do not supply missing real-account consent,
configuration or packaging evidence.
