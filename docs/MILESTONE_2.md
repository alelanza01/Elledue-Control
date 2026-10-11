# Milestone 2 — consegna verificata

Versione **0.2.0**, application ID invariato **it.elledue.control**, minimo API 26, target/compile API 35. Progetto Kotlin/Compose/Room esistente preservato. La consegna si ferma prima della Milestone 3.

## Funzioni utilizzabili

- Prenotazioni: creazione, dettaglio, modifica e annullamento conservando lo storico; immobile reale obbligatorio, ospite, capienza, piattaforma, date, orari previsti/effettivi, note e stato.
- Calendario mensile ed elenco: navigazione dei mesi, filtro immobile, legenda colori, arrivi/partenze, soggiorni, apertura dettagli e visualizzazione separata delle annullate.
- Controllo conflitti per immobile in transazione; intervallo [check-in, check-out), quindi cambio ospiti nello stesso giorno consentito. Sovrapposizioni salvabili solo dopo conferma esplicita sui conflitti correnti.
- Anteprima e snapshot economici: lordo, OTA, netto proprietario e compenso Elledue; importo OTA effettivo prioritario, anche zero. Default per immobile/portale e aliquote storiche preservate. Esempio verificato: **1.000 − 195 = 805 €; 20% di 805 = 161 €**.
- Dashboard operativa e riepilogo economico da dati reali, senza dati dimostrativi permanenti, incassi fittizi o attribuzioni mensili inventate.
- Editor con selettore date italiano, stato conservato dopo ricreazione dell’Activity, conferma uscita con modifiche, errori e conferme; temi chiaro/scuro esistenti.

## File e architettura

Nuovi componenti principali: `BookingDraft`, `BookingDates`, `BookingFinance`; `ReservationDao` e `ReservationRepository`; `ReservationViewModel`, `BookingEditorScreen`, `BookingScreens`, `BookingDraftSaver`; `DatabaseMigrations` e schema Room 2. Dipendenze esplicite in `ElledueApplication`, repository → ViewModel/StateFlow → Compose e Navigation. Importi Long in centesimi, percentuali in basis point, BigDecimal e arrotondamento HALF_UP per prenotazione.

Schema originale 1 conservato. `MIGRATION_1_2` additiva, con backfill degli importi dai parametri salvati; nessuna migrazione distruttiva. Predisposti conteggi fiscali, tassa prevista/riscossa e futura attribuzione contabile; relazioni spese/prenotazioni già presenti, senza flussi della Milestone 3.

## Verifiche eseguite

Comando effettivo: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`, dopo `source scripts/cloud-env.sh`.

- **Build riuscita**; APK generato e firma debug APK Signature Scheme v2 verificata con apksigner.
- **44 test, 0 falliti, 0 errori, 0 saltati**. Include 20 test della base precedente e 24 nuovi test: economia (6), date/DST (4), repository prenotazioni (10), migrazione reale (1), UI prenotazioni/calendario (3).
- Test di creazione/modifica/annullamento, persistenza con database a file riaperto, immobili diversi, aliquote e OTA effettiva, arrotondamenti, date errate, conflitti/consenso/stale approval, cambio ospiti, parametri storici, editor dopo ricreazione, date picker e calendario filtrato.
- Migrazione provata aprendo con Room 2 un database creato dallo schema originale 1 con dati: immobili, proprietari, impostazioni e prenotazione preservati; validazione Room superata.
- **Lint: No issues found**, nessun errore o avviso. App Bundle configurato per mantenere le risorse linguistiche necessarie alla UI italiana.
- Rendering UI reale via Robolectric API 34 con grafica nativa; screenshot generati in `app/build/reports/screenshots/m2-calendar.png` e `m2-booking-finance.png` e ispezionati. Dati di test sintetici.

APK: `app/build/outputs/apk/debug/app-debug.apk`, copia di consegna `/workspace/artifacts/Elledue-Control-milestone-2-debug.apk`.

SHA-256: `108b9639374e63651712702b09069753713e2f74af9d6f68f7bdd25957684e53`.

I risultati XML sono in `app/build/test-results/testDebugUnitTest`; report HTML in `app/build/reports/tests/testDebugUnitTest`; lint in `app/build/reports/lint-results-debug.txt`. Artefatti e chiavi debug rimangono fuori da Git; tutti i sorgenti, test e schemi sono versionati.

## Limiti e seguito

Nessun test su telefono fisico/emulatore: l’ambiente non offre KVM né un dispositivo collegato. L’APK debug è preliminare e non una release di produzione. L’aggiornamento senza perdita dati richiede stessa firma e application ID; non disinstallare per conservare il database locale. Backup esportabile, contabilità operativa/tassa, PDF, notifiche e sincronizzazione non sono implementati.

Le immagini ufficiali del logo sono visibili nella conversazione ma non disponibili come file binari nel checkout: il segnaposto non è stato dichiarato logo ufficiale. Per integrazione fedele servono originali PNG/SVG scaricabili; nessuna variante è stata ridisegnata.

Il PDF di riferimento non è importato né versionato. Le incongruenze OTA, la competenza temporale e gli arrotondamenti sono documentati in `REFERENCE_REPORT_REVIEW.md`. Da chiarire prima della Milestone 4. La Milestone 3 non è iniziata.
