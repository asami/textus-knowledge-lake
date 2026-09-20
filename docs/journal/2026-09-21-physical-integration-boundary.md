# Physical Integration Boundary: OpenClaw + TEAI

Date: 2026-09-21

## Decision

TKL does not own physical integration with Google Workspace.

Physical integration belongs to TEAI. For agent/tool-mediated SaaS integration, TEAI delegates the provider-specific external interaction to OpenClaw.

Thus the Google Workspace reference path is:

```text
Google Workspace / NotebookLM
  -> OpenClaw
  -> TEAI
  -> TKL
  -> Textus World
```

TKL begins at the semantic KnowledgeCandidate ingestion boundary and must remain unaware of Google APIs, OAuth flows, Drive/Docs mechanics, or OpenClaw tool details.

Direct deterministic integration remains possible through TEAI without OpenClaw. Therefore OpenClaw is a preferred integration edge for suitable SaaS/tool interaction, not a mandatory dependency of TKL or TEAI.
