# Verifica Milestone 1 — 11 ottobre 2026

Eseguito nel checkout `/workspace/Elledue-Control`, Linux x86_64, JDK Temurin 21.0.10+7, Gradle 8.11.1, SDK/target Android 35 e Build Tools 35.0.0.

```sh
bash scripts/setup-cloud.sh
source scripts/cloud-env.sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
```

Esito finale: **BUILD SUCCESSFUL**, APK generato, **20 test eseguiti**, zero fallimenti/errori/test saltati. Android Lint: zero errori e zero avvisi. Il setup è stato eseguito sul cloud ed è ripetibile con SDK/JDK/cache già presenti; le installazioni iniziali sono state effettuate verificando i checksum ufficiali. La riproduzione su una nuova macchina o snapshot pubblicato non è stata verificata.

| Suite | Test passati | Falliti |
| --- | ---: | ---: |
| AmountsTest | 4 | 0 |
| AppNavigationTest | 3 | 0 |
| PropertyDraftTest | 3 | 0 |
| PropertyRepositoryTest | 5 | 0 |
| SettingsRepositoryTest | 4 | 0 |
| SettingsViewModelTest | 1 | 0 |

I test Room/Robolectric usano Android API 34. Verificano creazione/modifica/archiviazione/riattivazione, identità e dati conservati, database a file chiuso e riaperto, recapiti/tariffe/testi collegati, foreign key e protezione degli orfani, validazione dei campi e conservazione dei testi non modificati. Le suite UI eseguono realmente la MainActivity con Compose, visitano tutte le sezioni, salvano un immobile, lo modificano e archiviano/riattivano, e ricreano l’Activity. Il tema scuro è verificato anche sul colore di sfondo renderizzato `#111B32`. Un test verifica che gli errori di lettura delle impostazioni siano mostrati senza inventare dati iniziali.

Parsing e arrotondamento contabile: centesimi interi/basis point, HALF_UP, caso 1000 € → 195 € OTA → 805 € netto → 161 € Elledue. Importi invalidi, percentuali fuori intervallo e overflow sono rifiutati.

Screenshot generati dai test, ispezionati: `app/build/reports/screenshots/dashboard-empty.png`, `dashboard-dark.png`, `property-detail.png`. Gli immobili raffigurati sono fixture dei test e non dati preinstallati nell’app.

APK preliminare: `app/build/outputs/apk/debug/app-debug.apk` (firma debug, verificata con `apksigner verify --verbose`, schema v2).

SHA-256: `812c5a73447ea24be93a94cc2594e4e6ce38d28c30b8c0b08d5a7de0be1709f9`.

Nessun keystore, password, token o dato personale reale è stato inserito nei sorgenti. L’APK e i report sono output ignorati da Git; tutti i sorgenti, wrapper e schema Room sono tracciati.

Non eseguiti: installazione su telefono/emulatore, test strumentati su dispositivo, firma release e milestone 2–7. Il cloud non dispone di `/dev/kvm`; le verifiche UI sono Robolectric con rendering nativo. Backup esportabile/ripristino, migrazioni da versioni distribuite, notifiche, rendiconti/PDF e sincronizzazione non sono funzioni di questa milestone.
