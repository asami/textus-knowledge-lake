# Material 公開 ABI のソース宣言

開発ランタイム準備に必要な `src/main/car/abi-manifest.json` が未配置だったため、
既存 CML が定義する Material の公開契約をソース ABI として宣言する。
正規座標は `org.simplemodeling.textus:KnowledgeLake:0.1.0-SNAPSHOT`、
形式は `cozy.car.abi-manifest.v2`、ABI バージョンは 1 とする。

既存の `searchMaterials`、`getMaterial`、`readOriginal`、
`createMaterialRegistration`、`getMaterialRegistration`、`materializeMaterial`
の六操作について、種別、入力、出力および公開型のフィールドと多重度をそのまま宣言する。
API、振る舞い、SQL、セキュリティ、依存関係、バージョンの変更はない。

ネイティブな開発ランタイム準備と、親ワークフローが所有する検証・独立レビューは引き続き必要である。
