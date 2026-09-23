# TKL Core Architecture Consolidation

- Date: 2026-09-23
- Status: Current design decision
- Supersedes: Google-Workspace-as-Knowledge-Lake assumptions in earlier exploration

## Summary

The design has shifted from treating Google Workspace as the Knowledge Lake to treating **Textus Knowledge Lake (TKL) itself as the federated Knowledge Lake**.

Google Workspace, Gmail, Slack and future systems are providers. TKL manages provider-neutral identity, Evidence, Preparation, PreparedMaterial, provenance, materialization and downstream processing.

## Why this change

Hands-on investigation of Google Workspace showed useful but only partially connected capabilities:

- Drive stores file evidence.
- Gmail stores mail evidence.
- Drive Project provides a persistent/saved Drive context for Gemini in Drive.
- Gemini in Drive is useful for exploratory preparation and can introduce additional Workspace context such as Gmail.
- Gemini Notebook requires explicit file-level sources and is strong at producing reports, Mind Maps, Slide Decks, Infographics, Audio and Video.
- Slack conversation data is not a native Google reasoning source in the same way and needs federation/materialization.

Rather than encode these product boundaries into the Knowledge Lake model, TKL will normalize them behind provider adapters.

## Federated resource identity

TKL registers important external resources without copying them by default.

A stable TKL resource identity is issued independently of provider location:

```text
textus:klake:kr:<id>
```

The registry resolves this ID to provider representations such as Google Drive, Gmail, Slack or a materialized local/Drive copy.

This allows resources to move or gain additional representations without changing references from PreparedMaterial or downstream KnowledgeHub content.

The operating principle is:

> Reference by default, materialize when necessary.

## Preparation becomes the core

The mandatory TKL pipeline is now:

```text
Federated Evidence
      |
      v
Scope / Resource Assembly
      |
      v
Normalization / Materialization
      |
      v
Preparation
      |
      v
PreparedMaterial
```

What happens after PreparedMaterial depends on the purpose.

Examples:

- KnowledgeHub: map directly to KnowledgeCandidate / Information / Knowledge-oriented structures.
- ChatGPT/Gemini/Codex/local LLM: render Markdown and reason over it.
- Gemini Notebook: synchronize selected files and produce presentation artifacts.
- Other processors: consume the canonical PreparedMaterial model.

Gemini Notebook is therefore an optional production/presentation path, not a mandatory knowledgeization stage.

## Google preparation workflow

For the NICT pilot, Drive Project + Gemini in Drive is used primarily for Preparation.

Drive Project contains the Drive files/folders that should normally be in Gemini's reasoning scope. Gmail can be introduced when reasoning. Slack communication must first be normalized/materialized into Drive.

A typical path is:

```text
Drive files -----------+
Gmail -----------------+--> Gemini in Drive / Drive Project
Slack --> TKL --> Drive+          |
                                  v
                              Preparation
                                  |
                                  v
                         PreparedMaterial
```

The preparation result can then be consumed directly by KnowledgeHub processing or passed to a presentation environment.

## Communication Value Objects

Mail/chat/conversation import must not be defined directly as a Google Docs layout.

TKL first defines provider-independent Communication Value Objects.

Candidate structure:

```text
Communication
  Kind
  Subject
  Context
  Participant*
  Message*
    Sender
    Recipient*
    Timestamp
    Content
    ReplyTo
    AttachmentReference*
  SourceReference
  Provenance
```

Gmail and Slack adapters map native provider data into this common model.

For import into Google Drive, the Google Workspace Adapter can choose:

1. JSON serialization of the VO; or
2. Google Docs rendering optimized for Gemini/human reading.

Both originate from the same model. An experiment should compare Gemini's handling of JSON vs Google Docs, especially thread/reply structure, participants, timestamps, evidence traceability and long conversations.

## PreparedMaterial as Entity

PreparedMaterial has identity and lifecycle and is stored in the TKL database.

It is not defined by JSON.

The authoritative definition is the TKL domain model. JSON/KAR are mappings.

PreparedMaterial retains:

- purpose and context;
- source/evidence references;
- structured content;
- provenance/lineage;
- preparation history;
- resource references;
- version/status.

Evidence references use TKL resource identities rather than provider IDs.

## JSON, Markdown and KAR

### JSON

Canonical lossless export/interchange format.

The JSON must contain everything required to generate the LLM/human Markdown representation without fetching external resources again.

### Markdown

Generated projection for LLM/human use. Not canonical storage.

### KAR

**KAR = Knowledge Archive**.

When PreparedMaterial needs embedded attachments/resources, use a ZIP-compatible `*.kar` package containing `prepared-material.json`, a manifest and resources.

Without embedded resources, `*.json` alone is valid and preferred.

Thus:

```text
PreparedMaterial Entity
       |
       +--> JSON       lossless portable snapshot
       +--> KAR        JSON + embedded resources
       +--> Markdown   LLM/human projection
```

## Provider-specific processing

Google Workspace remains the first reference provider because it combines storage, mail, Gemini reasoning and rich presentation tooling.

However, PreparedMaterial and Communication models must work unchanged with non-Google providers.

Provider adapters are responsible for:

- source discovery/access;
- native-to-TKL normalization;
- representation resolution;
- materialization;
- workspace synchronization;
- provider-specific rendering;
- artifact collection.

TKL owns the semantics, identity, provenance and workflow.

## Next implementation direction

A useful first executable slice is:

1. Define KnowledgeResource identity/registry model.
2. Define Communication / Message / Participant / AttachmentReference VOs.
3. Implement Gmail and Slack deterministic fixtures mapping to the same Communication model.
4. Define PreparedMaterial Entity and minimal lifecycle.
5. Implement PreparedMaterial JSON codec with round-trip tests.
6. Implement Markdown renderer from JSON/domain model.
7. Implement KAR packaging for an attachment-bearing fixture.
8. Experiment with Communication JSON vs Google Docs rendering in Gemini in Drive.
9. Build a Commons-consensus preparation fixture spanning Drive, Gmail and Slack evidence.

This slice validates provider independence before deeper Google Workspace automation is introduced.
