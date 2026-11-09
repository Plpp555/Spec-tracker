# 署名鍵の運用

Android は「同じアプリ ID・同じ署名鍵」の APK でなければ上書き更新を受け付けません。
そのため、**鍵を失う・漏らすと、既存ユーザーへ更新を届けられなくなる／偽の更新を作られる**危険があります。
Spec-tracker では次のルールで運用します。

## ルール

1. **debug 用と release（公開）用で、別々の鍵を使う。** 鍵・パスワードは共有しない。
2. **鍵ファイルとパスワードは Git にコミットしない。**（`signing/` と `*.p12` / `*.jks` / `*.keystore` は `.gitignore` 済み）
3. **debug 鍵は非公開。** 一般の人が取得できる場所（リポジトリ・リリースページ・CI の公開ログ）に置かない。
4. **release 鍵は debug 鍵よりさらに厳重に保管する。** 紛失しないよう、暗号化したバックアップを別の場所にも保存する。
5. release ビルドは、release 鍵が無い場合は署名なしになる。debug 鍵で署名されることは無い。

## Gradle が鍵を読む場所

`app/build.gradle.kts` が、次の順に探します（環境変数が優先）。

| 設定項目 | 環境変数（debug の場合） | `signing/debug.properties` のキー |
| --- | --- | --- |
| 鍵ファイル | `SPEC_TRACKER_DEBUG_STORE_FILE` | `storeFile`（`signing/` からの相対パス or 絶対パス） |
| 鍵ファイルの形式 | `SPEC_TRACKER_DEBUG_STORE_TYPE` | `storeType`（省略時 `PKCS12`） |
| ストアのパスワード | `SPEC_TRACKER_DEBUG_STORE_PASSWORD` | `storePassword` |
| 鍵のエイリアス | `SPEC_TRACKER_DEBUG_KEY_ALIAS` | `keyAlias` |
| 鍵のパスワード | `SPEC_TRACKER_DEBUG_KEY_PASSWORD` | `keyPassword`（省略時は `storePassword`） |

release は `DEBUG` を `RELEASE` に、`signing/debug.properties` を `signing/release.properties` に読み替えます。
書き方の見本は `signing/debug.properties.example` / `signing/release.properties.example` です。

## debug 鍵を作る

```bash
mkdir -p signing && cd signing
keytool -genkeypair -storetype PKCS12 -keystore spec-tracker-debug.p12 -alias spec-tracker-debug \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=Spec-tracker Debug, OU=Debug, O=Spec-tracker, C=JP"
# パスワードは対話入力。PKCS12 では、ストアと鍵のパスワードは同じにする。
cp debug.properties.example debug.properties   # 内容を実際の値に書き換える
chmod 600 debug.properties spec-tracker-debug.p12
```

## release（公開版）用の鍵を作る

**debug とは別の鍵として**作ります。公開版を作る段階になってから作成してください。

```bash
cd signing
keytool -genkeypair -storetype PKCS12 -keystore spec-tracker-release.p12 -alias spec-tracker-release \
  -keyalg RSA -keysize 4096 -validity 36500 \
  -dname "CN=Spec-tracker, O=Spec-tracker, C=JP"
cp release.properties.example release.properties   # 内容を実際の値に書き換える
chmod 600 release.properties spec-tracker-release.p12
```

- 有効期間は十分長く（25 年以上）してください。期限が切れると更新できなくなります。
- Google Play で配布する場合は「Play App Signing」の利用も検討してください（Play 側が署名鍵を保管し、手元の鍵はアップロード鍵になります）。

## 更新するときの注意

- アプリ ID（`io.github.plpp555.spectracker`）と release 鍵を変えない。
- 更新のたびに `versionCode`（`app/build.gradle.kts`）を増やす。
- debug 版のアプリ ID は `.debug` 付きなので、公開版と同じ端末に共存でき、鍵が違っても競合しません。

## 署名の確認

```bash
$ANDROID_HOME/build-tools/36.0.0/apksigner verify --print-certs app/build/outputs/apk/debug/Spec-tracker-V1-debug.apk
```

`Signer #1 certificate SHA-256 digest` が、`keytool -list -v -keystore <鍵ファイル>` の `SHA256` と一致すれば、その鍵で署名されています。

## 鍵が漏れたとき

- **debug 鍵**: 新しい debug 鍵を作り直す（既存の debug 版は一度アンインストールが必要）。公開版には影響しません。
- **release 鍵**: 公開版の更新が危険になります。直ちに配布を止め、Play App Signing の鍵ローテーション等の対応を検討してください。
