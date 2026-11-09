# signing/

署名鍵の置き場所です。**このフォルダの中身（この README と `*.example` 以外）は Git にコミットされません。**

- `debug.properties` / `spec-tracker-debug.p12` … debug 用（非公開）
- `release.properties` / `spec-tracker-release.p12` … release（公開版）用（debug とは別の鍵）

作り方と運用ルールは [../docs/SIGNING.md](../docs/SIGNING.md) を参照してください。
