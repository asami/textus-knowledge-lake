# Contextual Retrieval Integration Notes

Exploratory, non-normative. See [Phase 3](../phase/phase-3.md) and [SIE plan](https://github.com/asami/semantic-integration-engine/blob/HEAD/docs/phase/phase-1.md).

TKL supplies authorized source identity, version, original content and available relationship/provenance references. SIE or its delegated retrieval backend owns chunking, contextualization, lexical/vector ranking and context assembly. TKL should manage durable derived-artifact references and lifecycle; physical vector/RDF store placement remains a contract decision, not a requirement to duplicate SIE stores.

Proposed artifact metadata: source ID/version/span, dependency IDs/versions, owner/tenant, policy binding, chunking strategy, contextualizer model/prompt version, generated text, embedding model/dimension, generation time, digest and lifecycle state. Preserve raw chunk separately from generated explanation. RDF provenance may describe derivation but generated assertions are not approved knowledge. Material/Candidate/Information ownership and review gates remain unchanged.

Evaluate document context first, then bounded graph expansion, Commons terminology and pattern context. Enforce authorization before generation and at retrieval; derived text inherits restrictions of all inputs. Rebuild indexes on dependency change; handle deletion/revocation and retries. Use asynchronous processing and caching with tenant/policy isolation. Open questions: physical storage, versioning granularity, provider routing and retention policy.
