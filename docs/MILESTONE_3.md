# Milestone 3 — contabilità operativa verificata

Consegna **0.3.0**, application ID invariato **it.elledue.control**, minimo API 26, target/compile API 35. Continuazione del progetto esistente dal commit 95b53ab; tutte le modifiche presenti prima della ripresa sono state conservate e completate. La Milestone 4 non è iniziata.

## Come usare le funzioni

Aprire **Economia**: selezionare immobile e mese, poi Riepilogo / Spese / Tassa. Nel Riepilogo compaiono compensi, spese complessive, anticipi, anticipi rimborsabili, pulizie, lavanderia, tassa prevista/riscossa/residua e compensazioni/pagamenti riconosciuti. Le tasse da configurare o da rivedere sono segnalate; la mancanza di dati non è interpretata come previsione zero. Il saldo è operativo e da riconciliare, non un rendiconto confermato; se negativo viene mostrato come credito.

**Spese:** + Spesa → categoria → importo → Salva. Immobile obbligatorio; data reale e mese di competenza indipendenti; prenotazione facoltativa dello stesso immobile; descrizione/nota e quantità/tariffa facoltative. Anticipata da Elledue e rimborsabile sono flag distinti: entra nel rimborso solo una spesa con entrambi attivi. Le categorie si aggiungono/rinominano/archiviano dal pulsante Categorie spese; i nomi storici non vengono riscritti. Rettifica e annullamento richiedono un motivo e conservano versioni consultabili. Nessuna cancellazione contabile silenziosa.

**Pulizie/Lavanderia:** accessi rapidi dalla scheda Spese. Tariffe dell’immobile (inizialmente 8 €/ora, 5 €/set), quantità e costo calcolato; importo effettivo manuale prioritario. Set interi positivi con definizione testuale personalizzabile per intervento. Esempi verificati 3 × 8 = 24 €, 2 × 5 = 10 €. Una sola riga della tabella expenses per intervento: riepiloghi e viste dedicate non generano una seconda spesa.

**Tassa:** dalla scheda Tassa o dalla prenotazione. Confermare soggetti, esenti e notti; soggetti + esenti = ospiti totali. Tariffa, soglia inclusiva dai 16 anni compiuti, limiti/esenzioni provengono dall’immobile e sono fotografati alla conferma. Nessuna età individuale viene memorizzata. L’operatore conferma il conteggio, senza presumere automaticamente le esenzioni. Una previsione manuale diversa richiede motivazione. I cambi di prenotazione pertinenti richiedono revisione dei conteggi.

**Movimenti:** Conferma riscossione registra soltanto quanto effettivamente incassato, anche parzialmente. Riscossione, compensazione e riversamento sono registrazioni distinte. Ogni compensazione si collega a una riscossione attiva e non può superarne la disponibilità residua; nessuna doppia sottrazione. Il riversamento richiede soggetto configurato separatamente (Da definire / Proprietario / Elledue / Altro) e non riduce nuovamente il saldo. Sono registrabili altri anticipi/pagamenti riconosciuti del proprietario, per immobile e mese.

Lo storico dei movimenti è append-only: lo storno motivato referenzia univocamente l’originale. Prima di stornare una riscossione occorre stornare compensazioni/riversamenti collegati. ID idempotenti e transazioni proteggono da doppi salvataggi. Le prenotazioni annullate conservano tasse e movimenti storici; non richiedono nuovi incassi nei riepiloghi e le somme già riscosse restano visibili da riconciliare.

## Competenza e calcoli

Regola definitiva dell’utente: **intera prenotazione nel mese e anno del CHECK-IN**. 29 novembre → 3 dicembre: novembre; 30 dicembre → 5 gennaio: dicembre dell’anno precedente. Una sola commissione, nessuna divisione automatica. Le spese seguono il mese assegnato, normalmente la data reale; compensazioni e altri pagamenti il mese assegnato al movimento. La tassa prevista/riscossa/residua del riepilogo riguarda i soggiorni con check-in nel mese, anche se riscossa successivamente.

Gli incassi OTA appartengono al proprietario. Le aliquote economiche restano storiche; l’OTA effettiva prevale sulla percentuale, compreso zero. Arrotondamento HALF_UP per singola prenotazione, poi somma dei dettagli. Rettificare lordo/OTA/aliquote richiede una motivazione e conserva snapshot prima/dopo nella scheda prenotazione. Non esistono totali OTA manuali separati e incompatibili con il dettaglio.

Saldo verificato: **161 + 24 + 10 + 5 − 30 = 170 €**, soltanto dopo il riconoscimento esplicito della compensazione. Prima del riconoscimento il saldo è 200 €. La riscossione da sola non lo sottrae. Tassa non classificata come ricavo o spesa.

## Database e file

Room **3**, schemi 1 e 2 originali invariati, schema 3 esportato. `MIGRATION_2_3` additiva conserva tutte le tabelle e ricostruisce le competenze; aggiunge categorie, revisioni spese, previsioni fiscali/versioni, movimenti e audit delle rettifiche economiche. Nessun fallback distruttivo. I campi fiscali di base della v2 non vengono trasformati in incassi inventati.

Componenti nuovi: AccountingEntities/Dao/Repository/Defaults, OperationalAccounting, AccountingViewModel, AccountingScreens, ExpenseScreens, TaxScreen e saver dei moduli. Architettura data → repository → ViewModel/StateFlow → Compose esistente conservata. Date locali Europe/Rome; importi Long in centesimi e BigDecimal, senza Float/Double contabili.

## Verifiche eseguite

Comando effettivo dopo `source scripts/cloud-env.sh`:

`./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`

- **Build riuscita**: APK 0.3.0 generato; firma debug APK Signature Scheme v2 verificata con apksigner.
- **71 test, 0 falliti, 0 errori, 0 saltati**: 44 test precedenti più 27 della Milestone 3 (dominio 9, repository contabile 14, migrazione v2 1, UI 3).
- Spese su immobili diversi, categorie personalizzate, anticipo/rimborso distinti, competenza spese, pulizie con costo corretto, set multipli, singola contabilizzazione, audit/storni e salvataggi idempotenti.
- Tassa con esenti, soglia inclusiva dei 16 anni, limiti notti, rettifica motivata, parametri storici, riscossione parziale/completa, nessuna riscossione automatica, compensazioni duplicate respinte, riversamento indipendente e storni con dipendenze.
- Prenotazioni a cavallo di mesi/anni, attribuzione esclusiva al check-in, cambi che richiedono revisione fiscale, dati annullati e crediti da riconciliare.
- Persistenza su database a file chiuso e riaperto per spese/previsioni/movimenti/storico.
- Migrazioni reali **1 → 2 → 3** e **2 → 3**, costruendo database dagli schemi pubblicati: validazione Room superata; immobili/proprietari, prenotazioni, spese e impostazioni preservati, inclusi tariffe portali, testi, attività e report preesistenti.
- Test UI Compose/Robolectric API 34 con rendering nativo: creazione spesa rapida/categoria, pulizia dopo ricreazione dell’Activity, rettifica/annullamento, tassa manuale e compensazione esplicita. Screenshot ispezionati in `app/build/reports/screenshots/m3-accounting-summary.png`, `m3-cleaning.png`, `m3-tourist-tax.png`.
- **Lint: No issues found**, nessun errore o avviso.

Risultati XML in app/build/test-results/testDebugUnitTest; HTML in app/build/reports/tests/testDebugUnitTest; lint in app/build/reports/lint-results-debug.txt. Dati dei test sintetici, nessun dato dimostrativo permanente nell’app.

APK: app/build/outputs/apk/debug/app-debug.apk, copia `/workspace/artifacts/Elledue-Control-milestone-3-debug.apk`.

SHA-256: `85180101dd75143a7d3cee6028b9ad7f766a010d6c92364e5c3cc72f3996cc75`.

## Limiti e questioni aperte prima della Milestone 4

L’APK è preliminare con firma debug; nessuna verifica su dispositivo fisico/emulatore, soltanto Robolectric. Aggiornare senza perdita dati richiede stessa firma e application ID; non disinstallare. Backup esportabile, notifiche e PDF non sono implementati. Allegati/scontrini sono rinviati: questa milestone non copia immagini con permessi persistenti non verificati e non espone storage pubblico.

Verifica allegati: il PDF originale è materialmente accessibile in /workspace/attachments e rimane fuori da Git. Gli strumenti espongono identificativi scaricabili per PDF e testi; per i loghi mostrati nella conversazione non è esposto un identificativo Sediment scaricabile né un file locale originale. La ricerca degli allegati locali non trova PNG/SVG o altri originali del logo. Non è stata dedotta una versione binaria dalle immagini né alterata l’identità. L’icona è ancora un segnaposto, non un logo ufficiale integrato; palette #111B32 / #DBBF8B preservata.

Competenza risolta: mese del check-in. Restano da riconciliare le OTA del vecchio PDF (346,99 € dettagliati contro 423,33 €, differenza 76,34 €) e da chiarire componenti del lordo/base commissionabile. Nessuna cifra originale corretta arbitrariamente; vedi REFERENCE_REPORT_REVIEW.md. Il documento personale originale non è pubblicato. **Fermata prima della Milestone 4**.
