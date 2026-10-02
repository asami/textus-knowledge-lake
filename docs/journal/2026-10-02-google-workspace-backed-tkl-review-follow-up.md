# Google Workspace-backed TKL Review Follow-up Decisions

- Date: 2026-10-02
- Status: Current design refinement; implementation pending
- Review source: [Design review of f12542e](2026-10-02-google-workspace-backed-tkl-review.md)
- Scope: Logical context, execution, persistence, input retention and TKW handoff

## Assessment

レビューの基本方針を採用する。TKL の保存基盤、継続的な知識処理の文脈、一回の実行、TKW の候補ライフサイクルを分離する。Drive のフォルダ群を初期保存基盤にし、Drive Project / Notebook を外部の処理文脈とする既存判断は維持する。

レビュー原文は提案時点の記録として保持し、以下の補足を現在の notes に反映する。

## Adopted model separation

- KnowledgeProcessingContext は、目的・現在の資料集合・既存知識と零個以上の externalContextBindings を持つ継続的な TKL Entity とする。
- ProcessingRun は一回の処理要求、入力 snapshot、processor / actor、成果・結果・部分失敗を記録する。文脈更新により過去の Run の入力・来歴を書き換えない。
- PreparedMaterial の各生成版は producing Run と版付き Evidence へ紐づける。
- Resource と Evidence は区別する。Evidence は特定の処理範囲における資料の役割・選択版を表す。
- RawCandidateProposal は TKL の提案であり、TKW Candidate の identity / lifecycle は TKW が持つ。

具体的な DTO、CML、状態遷移、API は未確定であり、概念名と不変条件の採用を実装済み契約とは扱わない。

## Persistence and concurrency clarification

schema version、Entity revision、provider version と入力 snapshot の識別子を分ける。小さな独立 JSON record を基本とし、全資料・Run を一つの巨大 aggregate にまとめない。

JSON の revision を増やすだけでは同時更新を防げない。読んだ版の確認と書き込みを一体として保証する契約を adapter で検証する。Drive に条件付き更新能力があることを推測で前提にしない。具体的な競合制御の方式は仕様化対象とする。

内容ハッシュの一律禁止は採用しない。canonical identity と排他制御から区別し、snapshot や転送内容の整合性確認に利用する。

外部成果物の生成と管理情報更新の部分失敗を記録し、既に生成された成果物と未完了の取り込み・記録を区別する。

## Input retention and observable provenance

固定した入力 manifest、provider revision への参照、実際に内容を保持した snapshot を区別する。Drive の古い版の自動削除や Docs 等の履歴統合を考慮し、版 ID の記録だけで永続的な再取得を保証しない。

必要な入力は、保持方針に応じて materialization / export または検証済みの provider 保持機能を利用する。保存先、取得時点、表現形式と内容の整合性情報を残す。内容を保持しない入力は、その限界を明示する。既存知識の入力版も固定し、KnowledgeHub の更新や投影 rebuild により過去の基準を置き換えない。

TKL が外部処理に提供した集合と、外部 processor が実際に利用したことを確認できる集合を区別する。非公開の入力や処理は未確認とし、手動処理も取得可能な事実を Run に記録する。

## TKW receipt, retries and correlation

送信元 TKL と proposalId を受け渡しの冪等性キーとする。同じキー・同じ内容の再送は同じ受領結果と TKW Candidate 対応へ解決する。同じキーで異なる内容は競合とし、新たな提案には新しい proposalId を使う。

TKW の重複判定・候補への適用・受領結果の保存には一貫した保証が必要である。TKL は受領状態と proposalId → TKW Candidate 対応を保持する。timeout は未確認として扱い、新しいキーで別候補を作るのではなく、同じキーで照会・再送する契約を設計する。

CandidatePreparationRequest には requestId と tkwCandidateRef を持たせ、Run、返却 PreparedMaterial と provenance を対応付ける。再送時の重複処理・取り込み防止も TKW と共同で仕様化する。既存の TKW API がこの保証を実装済みとはみなさない。

## Next specification order

1. README / Phase 1 / CML scaffold の旧記述を現在の責務と Drive-first の経路へ整合させる。
2. Resource / Evidence / Context / Run / PreparedMaterial の ID、不変条件とライフサイクルを定義する。
3. JSON schema、更新競合、部分失敗、入力保持と provenance を定義する。
4. TKW と提案・受領・冪等性・既存候補準備の相関契約を定義する。
5. 小さな Drive source set による経路を実装する。Drive Project の手動処理を許容し、Notebook / Gmail / Slack の連携は後続とする。

今回の反映は notes と補足 journal の更新であり、README / Phase 1 / CML の整合作業や連携実装はまだ行っていない。

## References

- [Current backend and context design](../notes/google-workspace-backed-tkl.md)
- [Architecture](../notes/architecture.md)
- [Application model](../notes/application-model.md)
- [Drive revision management](https://developers.google.com/workspace/drive/api/guides/manage-revisions)
- [Drive changes and revisions](https://developers.google.com/workspace/drive/api/guides/change-overview)
