# Knowledge Lake Migration to asamiair

Date: 2026-10-14 (Asia/Tokyo)
Status: Migration plan recorded; source integration, dependency changes, builds and live operations have not been executed.

## Purpose and operating policy

Move textus-knowledge-lake development from macmini to asamiair in support of
nict-editing-studio and nict-editing-studio-app, alongside Knowledge Workbench.
Use the same canonical-checkout and shared-library policy recorded in the
[Workbench migration plan](../../../../../textus-knowledge-workbench/docs/journal/2026/10/2026-10-14-asamiair-migration-plan.md).

- Continue TKL in `/Users/asami/src/dev2026/textus-knowledge-lake` on `main`.
- Keep CNCF, Cozy and other shared libraries in their canonical directories and
  primary development branches. Do not introduce TKL-specific dependency
  worktrees, separate SNAPSHOT versions or artifact overlays.
- Consume locally published shared SNAPSHOT JARs, not dependency source trees
  or development `target` classpaths. Publish at validated checkpoints and
  refresh affected consumers together.
- Complete CNCF Phase 96 before resuming Phase 100. TKL source migration and
  specification reconciliation may proceed without waiting for either whole
  Phase; bind actual required APIs rather than inventing a blanket dependency.
- Preserve accepted deliveries, unfinished work and execution history. A
  migration checkpoint is not acceptance or Phase closure.

This journal records the migration plan. It does not itself revise product
contracts, start a Phase, reactivate a Goal or authorize live provider writes.

## Source handoff and observed differences

The source is the [macmini handoff](https://github.com/asami/textus-knowledge-lake/blob/codex/tkl-asamiair-20261014/docs/notes/asamiair-development-handoff.md).

| Item | Observed state |
| --- | --- |
| Migration branch | `codex/tkl-asamiair-20261014` |
| Remote checkpoint | `49e50aec2da3181fffd520415b0418e93e86d24f` — WIP, non-acceptance |
| Accepted source Phase 1 release | `f131d16a6dc98ff3ea754db6378aea64f090ff56`, as recorded by the handoff |
| GitHub main at inspection | `54cce9c8bdfa2b632e36aa918d7736891f6b21dc` |
| Remote branch relationship | Migration branch is 5 commits ahead and 16 behind main; both histories contain work to retain |
| Local asamiair checkout | `main` at `b91a597`, clean before this journal, one commit ahead of its locally recorded tracking ref |
| Local directive submodule | Initialized at `baf0ac8269f5b8c50c9ec54ec37620125ba97b5e` |

The handoff still says checkpoint/push pending, but the remote checkpoint and
branch were read successfully. Its clone-into-an-unused-directory instructions
are replaced here by integration into the existing canonical checkout.

The migration branch contains the accepted Phase 1 implementation and unfinished
Phase 2 capability work. GitHub main contains later design work, including
Material Package v1, the journal/index storage model and Material ID decisions,
which is absent from the migration branch. Neither branch can replace the other.
The current local checkout is also missing these newer documents.

The handoff reports Phase 2 stopped at TKL-P2-01 capability/contract discovery,
with PLAN state revision 34 and a blocked source-machine Goal because live
execution inputs were missing. Seventeen controlled HTTP characterization tests
and their scoped review passed. Production Drive storage, writer/recovery
behavior and live acceptance remain incomplete. These are source-machine facts,
not new-host validation results.

## Dependency baseline and destination

The handoff requires released artifacts, while the newer asamiair operating
decision uses shared development SNAPSHOTs. Preserve the old versions as the
historical acceptance baseline, then explicitly migrate the active build.

| Dependency | Source baseline | asamiair destination policy |
| --- | --- | --- |
| CNCF | `0.5.2` | Shared published development coordinate; currently `0.5.3-SNAPSHOT` |
| Cozy | `0.3.2.2` | Shared published development coordinate; currently `0.3.3-SNAPSHOT` |
| Core | Inspected transitive version `0.4.2` | Follow the shared CNCF dependency graph; the earlier machine inventory selected `0.4.3-SNAPSHOT` |
| sbt-cozy | `0.1.16` | Inspect and align with the common compatible build setup; do not guess a new plugin version |
| Scala / SBT | `3.3.8` / `1.12.6` | Retain unless the coordinated build requires a documented change |

Confirm current coordinates at execution time. Align `build.sbt`, `project.yaml`,
Cozy launcher/configuration, plugin settings and CAR runtime compatibility
declarations together. The source CAR declares CNCF minimum/maximum/tested as
`0.5.2`; do not leave that declaration contradictory to the actual runtime or
list a new version as tested before validation.

The source `externalResolvers` selects Maven repositories explicitly. Check that
the updated resolver configuration actually consumes Ivy local for shared
SNAPSHOTs, including generator selection. Do not satisfy the old release
coordinates by overwriting them in Ivy local. Normal released third-party
dependencies continue through their ordinary repositories and caches.

No Workbench WIP dependency branch is imported merely to run TKL. Use the shared
accepted publication checkpoint available on this machine. At inspection,
CNCF main already contains the Phase 96 preservation merge `08e54226`, but
Phase 96 remains in progress; do not repeat that integration or call it closed.

## Execution sequence

### 1. Preserve and integrate the three sources

1. Coordinate the canonical TKL editing owner and preserve local commits and
   any changes made since this inspection, including this plan.
2. Fetch remote main and the migration branch. Merge both into local main,
   retaining local directive integration, accepted Phase 1 code, Phase 2 WIP,
   later design documents and their histories. Do not reset or replace main.
3. Resolve overlapping README, architecture, Workspace notes and Phase documents
   by retaining accepted behavior and the later dated design decisions. A clean
   textual merge alone does not resolve their semantic differences.
4. Initialize `ai/directive` at the selected committed pointer if necessary;
   preserve local integration deliberately and do not edit shared directives
   inside the submodule or advance its pointer incidentally.
5. Transfer required ignored workflow records and referenced evidence separately
   if exact continuation is needed. Preserve Phase 1/2 bases, findings, review
   history and counters. Retain the source checkout and records until handoff
   is confirmed. Do not copy caches, generated `target` trees or macmini JARs.

The Git transfer does not restore the source chat or live Goal. Missing
temporary evidence stays explicitly unavailable rather than being reconstructed
as a historical PASS. Secrets remain in the selected credential facility.

### 2. Reconcile Material contracts before dependent implementation

Read the later GitHub decisions together with the accepted Phase 1 contracts
and the actual App/Core packet and upload behavior. Record the adjustments in
English and align affected specifications, plans and checklists before resuming
dependent Phase 2 implementation.

The existing target structure is:

```text
<Knowledge Lake>/components/nict-editing-studio/
  index/                         # rebuildable current projection
  journal/YYYY/MM/<package>/
    material.json
    assets/                      # optional when empty
    derived/                     # optional when empty
```

- Material Package is the source/working-asset unit; PreparedMaterial is the
  distinct preparation result. Do not substitute KAR or a PreparedMaterial
  schema for the source package.
- Dated journal packages preserve semantic history; meaningful revisions
  normally create another package with optional lineage. The mutable index is
  rebuildable. The Lake remains understandable without GitHub or a mandatory DB.
- Apply the later ID decision: `material.json.id` identifies the package using
  `urn:textus:klake:<lake>:material:<logical-path>`. The originating application's
  CNCF Material Entity ID is separate. Reconcile older `textus:material:...`
  examples and specify the association field; do not invent a settled DTO.
- The actual NICT lake name remains to be selected. Do not turn an example
  `nict` value into deployed configuration.
- App/Core currently provides direct Drive original-file upload and structured
  server submission. Journal/package creation and the TKL URN mapping were not
  found in that local implementation. Carry this as an explicit producer/consumer
  gap, not as proof that App Phase 6.1 delivered TKL package storage.
- Decide who creates package directories and `material.json`, how package
  completion becomes visible, how references reach Editing Studio, and how
  server-side original access is configured. Preserve the common Core/TFAF/App
  boundaries; cross-project implementation needs its own coordinated scope.

The old Phase 2 proposal includes competing-writer publication and substantial
version/recovery machinery. Reassess those requirements against the selected
execution model and journal storage before carrying them forward. Do not add
hashes, tamper-detection ledgers, file-copy proofs or another version system for
Material history. Retain required handling of malformed input, partial transfers
and real failures. Any change to accepted PreparedMaterial/local-store behavior
must be explicit; this migration plan does not silently remove those contracts.

### 3. Establish the shared build and one local route

After agreeing the contract adjustments, align the dependency declarations and
runtime selection with the shared publication checkpoint. Use registered runners
and the existing serialized SBT wrapper; never start independent top-level SBT
processes concurrently.

Start with generation and compilation through the existing Cozy/native route,
then focused native/local-flow regressions and the HTTP characterization cases
relevant to the chosen dependency versions. The old 17-case result describes
CNCF 0.5.2/Core 0.4.2; it neither proves the new versions nor requires preserving
old transport defects as desired behavior. Recheck actual status/body/method
capabilities needed for Drive and record the new results separately.

Do not rerun unrelated suites as a migration ritual. Preserve the current
Phase 2 focused/live acceptance and Phase 2.1 aggregate-full-validation ownership
unless that split is explicitly revised during replanning.

### 4. Resume Phase 2 with scoped live inputs

Resolve the selected Google account, read-only source folders, one TKL storage
root, credential mechanism, scopes and disposable probe objects before live
operations. The current handoff contains no usable selection of those inputs.
Source migration and offline contract/build work can proceed while they are
missing; live acceptance cannot be inferred from controlled tests.

Use the replanned Phase 2 to establish one complete storage route first: create
or acquire the selected Material, persist it in the agreed layout, resolve it
and reopen it in a fresh process. Extend that route with the required failure
and recovery cases rather than building isolated helper variants indefinitely.
Keep the existing Docs/text discovery requirements visible and explicitly
reconcile their placement with the mobile Material route during replanning.

Phase 2.1 remains the separate successor for manual preparation, result import,
PreparedMaterial/proposal export and the combined complete repository test run.
Do not start it merely because migration succeeds. TKW Candidate formation and
KnowledgeHub admission remain separate consumer responsibilities.

## Completion and retained limits

Migration is complete when the canonical checkout contains the preserved code
and reconciled current documents, the selected shared dependencies generate and
compile TKL and pass the affected local route, and remaining work can continue
with its history and explicit live-input gaps intact. Completing all Phase 2,
Phase 2.1 or Workbench work is not a machine-migration requirement.

This turn created only this English journal. Remote reads confirmed the WIP
checkpoint and branch divergence. No fetch into local Git, merge, build, test,
provider operation, publication, commit or push was performed.

## Source references

- [Migration handoff](https://github.com/asami/textus-knowledge-lake/blob/codex/tkl-asamiair-20261014/docs/notes/asamiair-development-handoff.md)
- [Phase 2 and released transport assessment](https://github.com/asami/textus-knowledge-lake/blob/codex/tkl-asamiair-20261014/docs/notes/google-drive-capability-assessment.md)
- [Journal and index design](https://github.com/asami/textus-knowledge-lake/blob/main/docs/notes/google-workspace-backed-tkl.md)
- [Material Package v1](https://github.com/asami/textus-knowledge-lake/blob/main/docs/notes/material-package-v1.md)
- [Material Entity ID and TKL URN decision](https://github.com/asami/textus-knowledge-lake/blob/main/docs/notes/material-universal-id-integration.md)
- [App dual-submission contract](https://github.com/KnowledgeHubProject/nict-editing-studio-app/blob/main/docs/notes/material-model-and-mobile-material-editor.md)
