# Elledue Control

Applicazione Android nativa per Elledue Management, gestione di immobili per affitti brevi. Il repository è stato creato da zero in Kotlin e Jetpack Compose.

## Milestone 1 e 2 — versione 0.2.0

Modulo Immobili: creazione, elenco con ricerca e filtri, dettaglio, modifica, archiviazione e riattivazione. Sono persistenti proprietario e recapiti, posti letto/ospiti, orari, note, commissioni, tariffe pulizie/lavanderia, tassa di soggiorno, commissioni diverse per portale e istruzioni di accesso IT/EN. Nessun dato dimostrativo viene inserito nell’app.

Prenotazioni: inserimento, dettaglio, modifica e annullamento senza cancellare lo storico. Immobile obbligatorio, ospiti, portale, date, orari previsti/effettivi, note e stato. Calendario mensile ed elenco con filtro immobile, arrivi/partenze, soggiorni e accesso ai dettagli. Le sovrapposizioni richiedono una conferma esplicita; il cambio ospiti nella stessa giornata è consentito.

Le commissioni sono calcolate in centesimi, con anteprima immediata e importo OTA effettivo prioritario. Aliquote e importi applicati sono conservati nella prenotazione: cambiare le impostazioni dell’immobile non modifica lo storico. Dashboard ed Economia mostrano dati registrati, distinguendo lordo/netto del proprietario dal compenso Elledue. Non sono incassi confermati di Elledue né rendiconti mensili.

Navigazione: Dashboard, Immobili, Calendario, Economia, Attività, Impostazioni. Attività e le funzioni delle milestone successive sono ancora predisposte, non operative.

Identità visiva: blu notte `#111B32`, dorato `#DBBF8B`, tema chiaro/scuro automatico. L’icona edificio è un segnaposto neutro, non il logo definitivo. Sostituire `app/src/main/res/drawable/ic_launcher.xml` con il logo ufficiale quando disponibile come file originale PNG/SVG; lo splash utilizza già il segnaposto. Le varianti ufficiali sono state mostrate nella conversazione, ma non sono disponibili nel checkout come file utilizzabili: non vengono ridisegnate né dichiarate integrate.

## Build e test

- JDK 17 o 21 completo (non solo JRE).
- Android SDK 35, Build Tools 35.0.0; minimo Android 8.0 / API 26.
- Gradle 8.11.1 tramite wrapper con checksum SHA-256.
- Application ID stabile: `it.elledue.control`.

Aprire in Android Studio oppure impostare `ANDROID_HOME` sul proprio SDK e poi:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

APK installabile: `app/build/outputs/apk/debug/app-debug.apk`. Firma debug locale; nessuna chiave è versionata. Il debug APK è preliminare e non una release per la produzione. APK release con firma privata esterna al repository nella milestone finale.

I 44 test comprendono persistenza su database a file riaperto, CRUD/archiviazione immobili, prenotazioni, annullamenti, conflitti e consenso esplicito, cambio ospiti, snapshot economici, date/DST, arrotondamento, migrazione Room 1 → 2 con dati preesistenti e navigazione Compose. I test Android sono eseguiti con Robolectric; non equivalgono a una verifica su telefono fisico.

## Ambiente cloud

```sh
bash scripts/setup-cloud.sh
source scripts/cloud-env.sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Il setup installa JDK e SDK con verifica dei checksum, accetta le licenze SDK, configura cache scrivibili e il proxy HTTPS della piattaforma mantenendo la verifica TLS. Il checkout esistente è già isolato: non creare worktree aggiuntivi. SDK e cache sono fuori dal repository. Nessun account remoto o API a pagamento è richiesto dall’app.

## Struttura

- `app/src/main/java/it/elledue/control/data`: Room, entità, DAO e repository.
- `app/src/main/java/it/elledue/control/domain`: validazione dei moduli e importi in centesimi.
- `app/src/main/java/it/elledue/control/ui`: design system, schermate, ViewModel e StateFlow.
- `app/schemas`: schema Room versionato; nessuna migrazione distruttiva.
- `app/src/test`: test JVM, database Android e UI Compose.
- `docs/ARCHITECTURE.md`: modello dati e vincoli economici delle prossime milestone.
- `docs/ROADMAP.md`: stato e criteri delle successive consegne.
- `docs/VALIDATION.md`: verifica storica della Milestone 1.
- `docs/MILESTONE_2.md`: consegna e verifiche della Milestone 2.
- `docs/REFERENCE_REPORT_REVIEW.md`: incongruenze del PDF di riferimento, senza dati personali.

I dati rimangono sul dispositivo nel sandbox Android. In questa milestone non sono presenti backup esportabile, sincronizzazione, documenti ospiti, notifiche o PDF: non disinstallare l’app per conservare i dati. Le nuove versioni devono mantenere lo stesso application ID e la stessa firma per aggiornare l’installazione senza cancellarli.
