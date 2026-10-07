# Notizblock

Ein gelber Notizblock. Eine Seite, nichts weiter.

Kein Header, keine Sidebar, keine Tabs, keine Navigation — ein einziges
scrollbares, liniertes Notizblatt. Alles, was getippt wird, wird sofort
persistiert (`shared_preferences`), überlebt also App-Kill und Neustart
auch ohne dass die App zwischenzeitlich im Speicher war.

Wegwerfprojekt. Keine Tests, kein CI-Gate, kein Anspruch auf Dauerhaftigkeit.

## Build

GitHub Actions, manuell getriggert (`workflow_dispatch`) über
`.github/workflows/build-apk.yml`. Baut eine Android-Release-APK
(debug-signiert, kein eigenes Keystore nötig) und legt sie als
GitHub-Release `latest` in diesem Repo ab.

## Lokal

```bash
flutter pub get
flutter run
```
