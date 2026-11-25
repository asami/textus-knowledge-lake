# 2026-11-25 — Contextual Retrieval

Discussed [Anthropic Contextual Retrieval](https://www.anthropic.com/research/contextual-retrieval) (2024-09-19). Chunk-specific document context is generated before embedding and BM25 indexing; reranking can further improve retrieval. Reported top-20 failure rates were 5.7% baseline, 3.7% contextual embeddings, 2.9% with contextual BM25, and 1.9% with reranking. These are external experimental results, not TKL measurements.

Decision direction: retrieval belongs in the SIE ecosystem; TKL preserves authorized originals, source identity and provenance, and coordinates durable derived-artifact lifecycle. Explore graph-aware context using Material/Candidate/Information relationships and Commons knowledge without bypassing admission or authorization. Generated context is derived information, not original evidence or approved knowledge.

Record implementation planning in [Phase 3](../../phase/phase-3.md) and [notes](../../notes/contextual-retrieval.md). Existing priorities remain unchanged.
