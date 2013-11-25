# NFC Host-based Card Emulation のサンプル

## NdefCard
Host-based Card Emulation(以下、HCE)を用いた、NDEFタグ(NFC Forum Tag Type 4)の振りをさせるサンプルアプリです。

HCE対応の端末(Nexus5や2013年版Nexus7)でなければ動作しません。

## NfcReader
NFCのReader Modeを用いた、NDEFタグの読み取りサンプルアプリです。

タッチしたNDEFタグに含まれるURIレコードを抽出して、一覧を表示します。項目をタップすると、URIに対応するアプリ(ブラウザなど)が呼び出されます。
