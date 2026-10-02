# Google Workspace-backed TKL and Knowledge Processing Contexts

- Date: 2026-10-02
- Status: Current design decision; implementation pending
- Scope: Initial Google Workspace backend, persistence, processing contexts and TKW boundary

## 基本構成

最初のターゲットは Google Workspace をバックエンドにした Textus Knowledge Lake (TKL) とする。

TKL の永続的な実体は Drive のフォルダまたはフォルダ群で表現する。TKL アプリケーションは、その上に Resource、Evidence、Preparation、PreparedMaterial、provenance と知識処理の文脈を管理する。

Google Workspace は最初の物理バックエンドであり、TKL の意味モデルは provider-neutral とする。既存の Drive 資料は参照を基本とし、TKL フォルダへ一律に移動・複製しない。

```text
TKL Drive フォルダ群
  資料 / メタデータ / 既存知識の投影 / 準備成果
        |
        +--> Drive Project ------+
        |                        |
        +--> Gemini Notebook ----+--> Gemini / 人による知識処理
                                      |
                                  準備成果
                                      |
                                 TKL へ取り込み
                                      |
                               PreparedMaterial
                                      |
                                Raw Candidate
                                      |
                                     TKW
                                      |
                          形成 / 編集 / レビュー / 承認
                                      |
                                KnowledgeHub
                                      |
                         TKL へ既存知識の投影を更新
```

ここで Gemini Notebook は、会話で合意した notebook 型の知識処理環境を指す設計上の呼称である。具体的な製品・API・連携方法は adapter 設計時に確認する。

## 永続化基盤と処理文脈の分離

| 概念 | 責務 |
| --- | --- |
| TKL フォルダ群 | 資料、TKL 管理情報、準備成果とその来歴を保存する |
| KnowledgeProcessingContext | 目的、現在の参照資料集合、既存知識、外部文脈との対応を持つ継続的な論理文脈 |
| ProcessingRun | 一回の処理要求、入力 snapshot、processor、結果・失敗と成果物の対応を記録する |
| Drive Project | 知識処理の文脈を Google 側で実現する選択肢 |
| Gemini Notebook | 準備・分析・統合・成果物生成に用いる別の文脈の選択肢 |
| TKL アプリケーション | 保存基盤、処理文脈、成果の取り込みと TKW への引き渡しを管理する |

Drive Project と Notebook は TKL の永続的な管理情報の正本にしない。Notebook を presentation 専用に限定せず、知識処理の文脈として扱う。ただし両環境の機能や自動化 API が同一であるとは仮定しない。

TKL と処理文脈は一対一に固定しない。同じ資料を、合意事項の分析、技術調査、知識候補の抽出など、異なる文脈から参照できる。一つの文脈が複数の TKL フォルダや外部の既存資料を参照することも許容する。

## Drive 上の保存構成

以下は論理的な保存役割の例であり、フォルダ名・階層を必須仕様にはしない。

```text
TKL/
├── Metadata/          # Resource、Evidence、文脈、出典、版、処理履歴
├── Evidence/          # 管理対象の原資料、または既存資料への参照
├── PreparedMaterial/ # 正本 JSON と人・LLM 向けの投影
└── KnowledgeContext/ # KnowledgeHub の既存知識の投影
```

既存の NICT の `00_Inbox / 10_Evidence / 20_Artifacts / 90_Archive` 構成も、同じ論理役割へ対応付けて利用できる。フォルダ名・パスを TKL Resource の識別子にはしない。

初期実装では Drive 上の JSON を TKL 管理情報の正本とし、別の TKL DB を必須にしない。PreparedMaterial は引き続き stable ID と版・ライフサイクルを持つ TKL Entity であり、その正本を JSON として永続化する。

将来 DB を導入する場合は、検索用索引・キャッシュか、正本の移行先かを明確にする。Drive JSON と DB の双方を独立した正本として更新しない。原資料の正本は原 provider、成立した知識の正本は KnowledgeHub にある。

ファイルの `appProperties` は TKL ID など短い対応情報に利用できる。複雑な構造や長い処理履歴は JSON に置く。必要に応じて Gemini が参照しやすい文書へ投影するが、その投影を管理情報の正本にしない。

### 管理情報の版と更新競合

Resource、Evidence、Context、Run、PreparedMaterial は、独立して管理できる小さな JSON record として設計する。一つの巨大な KnowledgeLake aggregate に全資料と実行履歴を含めない。schema の版、TKL Entity の revision、provider の版、および処理入力 snapshot の識別子を区別する。

更新時には、読んだ revision が現在も有効であることの確認と書き込みを、一体として保証する契約が必要である。JSON 内の revision を単に増やす方式や、読み取り後に無条件で書き戻す方式では更新競合を防げない。条件付き書き込みが利用できるかを adapter ごとに検証し、利用できない場合も書き込みの調停方法を明示する。Drive の ETag / revision 相当の値がそのまま利用できるとは仮定しない。具体的な競合制御方式は未確定とする。

内容ハッシュは、保存・転送した内容の整合性確認に利用できる。ただし canonical Entity ID、Entity revision、または検証されていない排他制御の代わりにはしない。

外部成果物の生成と TKL JSON の更新は一つの transaction として完了するとは仮定しない。処理状態と成果物参照を記録し、部分失敗後に完了済みの外部操作と未完了の記録・取り込みを判別できるようにする。

## 知識処理の文脈モデル

`KnowledgeProcessingContext` は TKL が ID と永続状態を持つ継続的な論理文脈とする。DTO/CML の具体的な仕様は未確定である。

- stable context ID、目的、テーマ、処理範囲;
- 現在選択している Resource / Evidence の集合;
- 既存知識の ID・版と KnowledgeContext の基準時点;
- Drive Project / Notebook 等の外部文脈の種別、識別子または参照先を持つ externalContextBindings;
- 処理方針と、この文脈に対する ProcessingRun の参照;
- 文脈の状態、および obsolete / superseded な資料の扱い。

外部文脈への binding は零個以上を許容し、外部 Project / Notebook の識別子を TKL contextId と同一視しない。

### ProcessingRun

個々の実行は、文脈とは別の ProcessingRun として記録する。文脈の現在の設定を更新しても、過去の Run の入力と来歴を書き換えない。

- runId、contextId、処理要求と時刻;
- inputSnapshot、および利用した既存知識の snapshot;
- processor / model / tool、人・agent の情報（確認できる範囲）;
- requested / running / succeeded / failed 等の状態と、失敗・部分完了の情報;
- 外部成果物の参照、取り込み結果、生成した PreparedMaterial の ID・版。

PreparedMaterial の各生成版は producedByRunId と sourceEvidenceRefs を持ち、次の関係を追跡できるようにする。

```text
Resource(version) -> Evidence(snapshot) -> ProcessingRun
 -> PreparedMaterial(version) -> RawCandidateProposal
```

Run の完了後は入力・出力・結果とその来歴を固定し、後日の運用注記や訂正は元の事実を消さずに追記する。具体的な状態遷移と訂正方式は仕様化対象とする。

人が Drive Project / Notebook で実行する処理も、処理要求、渡した資料、取得した成果と取り込みを Run に記録する。TKL が提供・取得した資料集合と、外部 processor が実際に利用したことを確認できる集合を区別する。非公開の参照や処理詳細は未確認とし、完全な入力把握・再現性を推測で主張しない。

Gemini に渡すのは、その処理に必要な Evidence、既存知識、説明文書とする。TKL の機械向けメタデータ全体を自動的に推論コンテキストへ含めない。旧版や無関係な資料を active context へ蓄積しない。

### 入力 snapshot の識別と保持

版への参照と、その版の内容を保持した snapshot を区別する。入力 manifest の固定だけで、外部資料の内容が永続的に再取得できるとはみなさない。

Drive の古い版には自動削除があり、Docs 等の版履歴は統合されることもある。provider revision の参照だけで長期保持を保証せず、保持要件に応じて内容の materialization、export、または検証済みの provider 保持機能を選ぶ。保存先、表現形式、取得時点、内容ハッシュと保持方針を記録する。外部資料は引き続き参照を基本とし、必要な入力だけを保持する。

既存知識についても、その Run が利用した KnowledgeProjection / KnowledgeContextSnapshot の版を固定する。KnowledgeHub の後続更新や context rebuild によって過去の Run の基準を書き換えない。

内容を保持できない場合は、その事実と再取得可能性の限界を記録する。入力内容を確実に保持する必要のある処理では、保持できないままその要件を満たしたとは扱わない。

## TKW との関係

TKL は資料と準備処理を管理し、TKW は候補のライフサイクル、形成、編集、レビュー、承認、Admission を管理する。

二つの経路を持つ。

1. Discovery preparation: 外部 Evidence → TKL → PreparedMaterial → Raw Candidate Proposal → TKW。
2. Candidate preparation: TKW の既存候補に紐づく写真・音声等 → TKL で準備処理 → PreparedMaterial を元の TKW 候補へ返す。

TKW の CandidateSource / RawDataIndex は候補中心の索引であり、TKL Resource ID や外部資料を参照する。同じ資料を TKL と TKW がそれぞれの責務で参照でき、ファイル本体の二重保存は要求しない。

TKW を経由して KnowledgeHub に成立した知識は TKL の KnowledgeProjection に戻し、次の処理文脈で Existing Knowledge + New Evidence として利用する。

### 提案と再送の契約

RawCandidateProposal は TKL の提案であり、TKW Candidate 自体ではない。少なくとも proposalId、候補の種別・提案内容、PreparedMaterial / Evidence の版付き参照、provenance、sourceContextId、sourceRunId と作成時刻を持つ。TKW は受領後に自分の Candidate ID を発行または解決する。

送信元 TKL の識別子と proposalId を受け渡しの冪等性キーとして扱う。同じキー・同じ提案内容の再送は、新たな Candidate を作らず、同じ受領結果と TKW Candidate への対応を返す。同じキーで内容が変わった場合は黙って上書きせず競合として扱い、新しい提案には新しい proposalId を割り当てる。内容ハッシュは整合性の確認に使えても、提案の canonical identity にはしない。

TKW 側の重複判定、候補への適用、および受領結果の保存には一貫した保証が必要である。TKL は受領状態と proposalId → TKW Candidate の対応を記録する。送信 timeout は受領失敗と断定せず、結果未確認として同じキーで照会・再送できる契約を設計する。具体的な API と保証の実装は TKW と共同で確定し、この note で既存 API があるとは仮定しない。

既存候補の準備処理には CandidatePreparationRequest を用い、requestId、tkwCandidateRef、resourceRefs、目的・要求処理を記録する。返却する PreparedMaterial と provenance は requestId、tkwCandidateRef と Run に対応付ける。この経路も再送時に重複処理・重複取り込みを起こさない契約を定義し、TKL に候補ライフサイクルを移さない。

## 最初の実装経路

Phase 1 は TKL プロジェクト内で完結する開発とする。Drive 資料を表すローカル fixture について、Resource 登録 → Evidence → Context・Drive Project binding の表現 → ProcessingRun と固定入力 → 準備成果の取り込み → PreparedMaterial → RawCandidateProposal export → TKL 側の結果処理・受領対応の記録を検証する。他プロジェクトの実装・稼働・契約合意と live Google 操作は完了条件にしない。ローカルの受領結果 fixture は実 TKW の Candidate 永続化・同時受領の冪等性を保証しない。

Google Workspace-backed TKL は最初の本番ターゲットとして維持する。本番経路は小さな Drive 資料セットと手動処理・成果取り込みから始められる。Drive の競合制御・保持や外部連携の保証は未確認として管理し、[連携台帳](phase-1-integration-register.md) に owner、契約案・参照版、検証・未確定事項を残す。Phase クローズ時に必要な外部開発を具体的な契約・fixture・受入例とともに提案する。

Drive Project を最初の adapter として優先するが、TKL の文脈モデルは Notebook も表現できるようにする。Notebook 連携、TKW からの candidate preparation、Gmail / Slack は後続実装とし、最初の経路の成立条件にはしない。

次の仕様化は以下の順に進める。

1. README、Phase 1、CML scaffold の旧記述を現在の責務・最初の実装範囲に整合させる。
2. Resource / Evidence / Context / Run / PreparedMaterial の ID と不変条件、Context と Run のライフサイクルを定義する。
3. 正本 JSON schema、版の区別、更新競合・部分失敗、入力 snapshot の保持と provenance を定義する。
4. Phase 1 の RawCandidateProposal と TKL 側の結果・相関契約を定義し、TKW の受領・冪等性、後続 CandidatePreparationRequest は外部への要求案として管理する。
5. 小さなローカル fixture 経路を実装・検証し、クローズ時に実 Drive-backed 経路と他プロジェクトへの連携提案を残す。

具体的な API 自動化、競合制御と受け渡しの実装は未実装・未確定とする。

## 既存文書との関係

この note は、TKL の独立 DB を必須とする保存先の想定、および Notebook を presentation 専用に限定する想定を更新する。provider-neutral な意味モデル、外部資料の参照優先、PreparedMaterial Entity、TKW 境界、KnowledgeHub feedback は維持する。

README、Phase 1、CML scaffold の責務・完了境界は [project-local scope decision](../journal/2026-10-02-phase-1-project-local-boundary.md) に従って整合した。CML はまだ scaffold であり、この記録は連携機能や Phase 1 の実装完了を意味しない。

## References

- [Architecture](architecture.md)
- [Application model](application-model.md)
- [Dual raw sources and Workbench index](../journal/2026-09-25-dual-raw-source-and-workbench-index.md)
- [Decision journal](../journal/2026-10-02-google-workspace-backed-tkl-contexts.md)
- [Design review](../journal/2026-10-02-google-workspace-backed-tkl-review.md)
- [Review follow-up decisions](../journal/2026-10-02-google-workspace-backed-tkl-review-follow-up.md)
- [Google Drive projects](https://support.google.com/drive/answer/16684520?hl=en)
- [Drive custom file properties](https://developers.google.com/workspace/drive/api/guides/properties)
- [Drive revision retention](https://developers.google.com/workspace/drive/api/guides/manage-revisions)
- [Drive changes and revisions](https://developers.google.com/workspace/drive/api/guides/change-overview)
