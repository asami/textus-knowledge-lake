# Google Workspace-backed TKL and Processing Contexts

- Date: 2026-10-02
- Status: Current design decision; implementation pending

## Context

最初のターゲットを Google Workspace をバックエンドにした TKL とする。Drive Project と Drive フォルダ内メタデータの役割を検討し、TKL の永続化基盤と知識処理の文脈を分離することで合意した。さらに Gemini Notebook も知識処理の文脈として扱う。

## Decision

1. TKL の永続的な実体は Drive のフォルダまたはフォルダ群とする。TKL アプリケーションはその上で Resource、Evidence、Preparation、PreparedMaterial、provenance を管理する。
2. 初期バックエンドでは Drive 内の JSON を TKL メタデータおよび PreparedMaterial の正本とし、別の TKL DB は必須にしない。既存資料は参照を基本とする。
3. Drive Project と Gemini Notebook を Knowledge Processing Context の Google 側の実現として扱う。Notebook は準備・分析・統合にも利用する選択肢であり、presentation 専用に限定しない。
4. TKL と文脈は一対一に固定しない。同じ資料を目的の異なる複数の文脈で利用し、一つの文脈が複数フォルダの資料を参照できる設計とする。
5. 文脈の目的、参照資料・版、既存知識、外部文脈の参照先、処理履歴と成果物の対応を TKL メタデータに記録する。現在の参照集合と個々の処理で使用した集合を区別する。
6. 外部環境の処理成果を TKL に取り込み、PreparedMaterial と出典・処理来歴を管理する。
7. TKW が候補の形成・編集・レビュー・承認・Admission を所有する。TKL は新しい Raw Candidate の提案と、既存 TKW 候補に紐づく資料の準備処理を担う。
8. KnowledgeHub の成立済み知識を TKL に投影し、次の文脈で Existing Knowledge + New Evidence として利用する。成立した知識の正本は KnowledgeHub とする。

## Initial implementation direction

Drive の小さな資料セットから、Resource / Evidence → Drive Project による準備 → 成果の取り込み → PreparedMaterial → Raw Candidate の TKW 引き渡しを最初の経路とする。

文脈モデルには Notebook を含めるが、Notebook 連携の実装は最初の経路の完了条件にはしない。TKW からの candidate preparation、Gmail / Slack も後続とする。

## Superseded assumptions and retained boundaries

- 2026-09-25 の journal にある「TKL DB が管理情報を保存する」は、初期バックエンドでは Drive 上の JSON へ置き換える。原資料の provider と TKL の資料管理、TKW の候補中心索引という責務分離は維持する。
- Notebook の presentation 専用という旧位置付けを広げる。Drive Project 優先と Notebook を必須にしない初期実装範囲は維持する。
- TKL の意味モデルは引き続き provider-neutral とする。フォルダ名・Drive ID を canonical TKL identity と同一視しない。
- JSON は管理情報・PreparedMaterial の正本、Gemini 用文書は投影とする。将来 DB を追加する場合も正本を二重化しない。

## Open design work

- メタデータ schema、ID、版、更新競合・部分失敗の扱い;
- 文脈の参照集合、処理時の資料版、成果物と provenance の対応;
- Notebook の具体的な製品・API・連携方法と各環境の自動化能力;
- PreparedMaterial と Raw Candidate の TKW 受け渡し契約;
- README / Phase 1 / CML scaffold の旧記述の整合。

この journal は設計判断の記録であり、実装・外部連携・Phase 1 の完了を主張しない。

## Current notes

- [Google Workspace-backed TKL](../notes/google-workspace-backed-tkl.md)
- [Architecture](../notes/architecture.md)
- [Application model](../notes/application-model.md)
