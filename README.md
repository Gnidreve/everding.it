# Notepad--

Ein gelber Notizblock. Eine Seite, nichts weiter.

Natives Android (Kotlin, kein Flutter, keine Runtime-Engine) — eine Activity,
ein `EditText` mit selbstgezeichneten linierten Zeilen, `SharedPreferences`
für sofortige Persistenz bei jedem Tastendruck.

Wegwerfprojekt. Keine Tests, kein CI-Gate.

## Build

GitHub Actions, manuell getriggert (`workflow_dispatch`) über
`.github/workflows/build-apk.yml`. Baut die Release-APK direkt mit
`gradle :app:assembleRelease` (debug-signiert, kein eigenes Keystore) und
legt sie als GitHub-Release `latest` in diesem Repo ab.

## Lokal

```bash
./gradlew :app:assembleRelease
```

(Gradle-Wrapper ist nicht eingecheckt — `gradle wrapper` einmal lokal
laufen lassen, oder direkt mit installiertem `gradle` bauen.)
