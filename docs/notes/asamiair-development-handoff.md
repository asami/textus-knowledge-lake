# Knowledge Lake: asamiair development handoff

Date: 2026-10-14 (Asia/Tokyo)
Status: procedure prepared; migration branch, WIP checkpoint and GitHub push pending

## Migration method

Use the same GitHub-based development handoff as Knowledge Workbench:
preserve the current development state in a WIP checkpoint on a dedicated
branch, push that branch, then clone it on asamiair and open a local Codex
project there. The checkpoint preserves unfinished work; it does not establish
Step acceptance or Phase closure.

| Item | Selection |
| --- | --- |
| Repository | `asami/textus-knowledge-lake` |
| Proposed migration branch | `codex/tkl-asamiair-20261014` |
| Source checkout | `/Users/asami/src/dev2026/textus-knowledge-lake` |
| Current source branch | `main`, four commits ahead of locally recorded `origin/main` |
| Current HEAD / accepted Phase 1 release | `f131d16a6dc98ff3ea754db6378aea64f090ff56` |

TKL has no dedicated dependency source checkouts. Its build selects released
CNCF and Cozy artifacts. Workbench's four dependency branches are not part of
this migration and must not substitute for TKL's declared dependencies.

## Prepare on the source machine

1. Record the current HEAD and intended changes with `git rev-parse HEAD` and
   `git status --short --branch`. Verify the proposed migration branch is unused
   locally and on GitHub before creating it.
2. Create the migration branch from the current local HEAD, retaining all four
   local Phase 1 commits. Include the current README change, new Phase 2 and
   Phase 2.1 plans/checklists, definition and capability journals, capability
   assessment, `ReleasedHttpDriverSpec.scala`, and this handoff document in the
   WIP checkpoint. The source inventory currently contains nine changed/new
   paths before this document is added.
3. Use the checkpoint/commit workflow and registered commit runner to save the
   selected work as-is. Record its actual commit ID here or in the final handoff.
   Preserve the existing Phase acceptance records and unfinished checklist
   items. No new behavioral PASS is implied by migration.
4. Push only `codex/tkl-asamiair-20261014` to `origin`, set its upstream, and
   confirm the remote branch names the checkpoint commit. An ordinary clone of
   the old remote `main` would omit both the four local commits and the
   uncommitted Phase 2 work.

These steps are instructions for the later migration execution. This document
does not record a completed checkpoint or push.

## Obtain the source on asamiair

Run after the migration branch has been pushed. Choose an unused destination
under the local development workspace; preserve any existing checkout.

```sh
git clone --recurse-submodules --branch codex/tkl-asamiair-20261014 git@github.com:asami/textus-knowledge-lake.git textus-knowledge-lake
cd textus-knowledge-lake
git status --short --branch
git rev-parse HEAD
git submodule status
```

Compare HEAD with the actual pushed checkpoint ID. The `ai/directive` submodule
must be initialized at the repository's recorded revision so that the root
`AGENT.md`, `AGENTS.md`, and `RULE.md` symlinks resolve. The source machine
currently has this submodule uninitialized. For an existing clone, initialize
it with `git submodule update --init --recursive` without advancing its pointer.

Open the checkout as a local Codex project on asamiair. Read the shared
directives, this handoff, Phase 2 plan/checklist, Phase 2.1 plan/checklist, and
the capability assessment/journal before continuing. The README's Phase 2
"not started" label predates the capability work; the current handoff below
records the actual development position.

## Restore the build environment

Use the ordinary workstation setup for GitHub access, Java, SBT, Cozy and the
registered Codex agents/skills. Resolve their locations on asamiair instead of
copying assumptions about source-machine absolute paths. Retain the declared
versions: Scala 3.3.8, SBT 1.12.6, sbt-cozy 0.1.16, Cozy 0.3.2.2 and CNCF 0.5.2
(the inspected core dependency is 0.4.2).

Resolve released artifacts through the declared Maven repositories. Regenerate
managed sources and local runtime preparation through the project's existing
build/startup procedure; generated `target/` contents and machine caches are
not source-transfer requirements. Use the registered serialized SBT runner
for independent top-level SBT commands. Suitable initial checks are:

```text
sbt --batch cozyGenerate compile
sbt --batch "testOnly org.simplemodeling.textus.knowledgelake.drive.ReleasedHttpDriverSpec org.simplemodeling.textus.knowledgelake.nativeapp.NativeFlowSpec"
```

Record actual new-machine results separately from historical source-machine
results. These checks do not establish live Google acceptance. The complete
repository suite remains assigned to Phase 2.1 under the existing split.

## Resume the development

Phase 1 is closed. Phase 2, Google Drive Backend, remains incomplete at
`TKL-P2-01` capability/contract discovery. The source workflow retains `PLAN`,
state revision 34, and a blocked native Goal because scoped live execution
inputs are missing. Controlled released-HTTP characterization passed 17 tests;
the transport and documentation slices passed independent review. Production
Drive acquisition, canonical storage, writer/publication behavior, retained
history and restart/recovery have not been implemented or accepted.

The user selected migration before Phase 2 completion because specification
adjustments are needed. Begin development on asamiair by discussing and
recording those adjustments in English, then align the affected Phase plans,
checklists and contracts before proceeding with dependent implementation.
The adjustment details remain to be settled; preserve the accepted Phase 1
baseline and historical evidence while assessing their impact.

After the specification adjustments, settle the selected live Google account,
read-only source folders, one TKL storage root, credential facility and scopes,
and disposable probe objects. Then verify released transport capabilities and
live provider behavior before selecting the storage/writer/recovery contracts
and proceeding with production implementation. Retain the observed transport
limitations in the capability assessment; the loopback PASS does not resolve
them. Migration alone does not resolve the blocked condition.

Keep Phase 2.1 as a separately authorized successor. Other-project integration
work remains managed proposals rather than a requirement to change another
repository during this TKL development handoff.

## Local workflow records

GitHub transfers source and management documents, not the original Codex chat,
native Goal binding, ignored control/review records, temporary command logs or
locally generated artifacts. This is the same distinction used in the
Workbench handoff.

Retain the original `.codex-workflow/phases/PHASE-1/` and
`.codex-workflow/phases/PHASE-2/` directories on the source machine. If exact
workflow continuation is required, transfer those private records and the
specific external evidence files they reference separately, then use the
current supported continuation/relocation procedure. Verify referenced
`/private/tmp/skill.cncf.d/...` evidence is available before relying on it.
Retain original bases, ledgers, review/repair counters, blocked audits and
acceptance obligations; do not rewrite historical receipts to manufacture a
new-host PASS. A filesystem copy does not itself recover the live Goal or chat.

Until that continuation is established, use this document as a development
handoff. Keep the original checkout and records until the asamiair checkout
and handoff are confirmed.
