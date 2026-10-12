# Milestone 4 — rendiconti, marchio, backup e firma esterna

Consegna **0.4.0**, versionCode **4**, application ID invariato **it.elledue.control**, minimo API 26, target/compile API 35. Il progetto della Milestone 3 è stato conservato; il caricamento dei loghi dell’utente (commit 9c6ceb1) è stato integrato tramite fast-forward. La Milestone 5 non è iniziata.

## Consegna effettivamente scaricabile

Gli artefatti sono versionati insieme ai sorgenti in [deliverables/milestone-4](../deliverables/milestone-4/README.md): APK debug, PDF sintetico da 170 €, PDF rettificato, prova multipagina e screenshot chiari/scuri. Il README contiene link di download GitHub, non semplici percorsi workspace. [Manifest](../deliverables/milestone-4/manifest.json) con dimensioni e SHA-256.

APK SHA-256: `46fbf62afd645610f507abcc9d4a2917e005f527f02768453879db045ca4f218`.

## Flusso utilizzabile

Economia → Rendiconti mensili → immobile → periodo → Genera / apri bozza. La bozza raccoglie tutti i dati registrati pertinenti. Riepilogo con saldo evidente; Dettagli con prenotazioni, spese, taxa e movimenti; Verifica con criticità e note. Le prenotazioni non attribuite sono individuate; l’app non può conoscere quelle non ancora inserite dai portali, che restano da confrontare manualmente.

Dalla bozza si correggono i sorgenti reali (prenotazioni, spese, tassa/movimenti) senza creare una seconda contabilità PDF. Al ritorno o con Aggiorna bozza, totali e righe vengono ricalcolati. Note persistite con l’aggiornamento. L’anteprima PDF è marcata BOZZA. Errori/incoerenze bloccano la conferma; OTA stimate e altri avvisi richiedono riconoscimento esplicito. La conferma verifica anche che i dati non siano cambiati rispetto alla bozza controllata.

Conferma manuale → snapshot immutabile → esportazione PDF via selettore documenti Android, apertura nel lettore scelto dall’utente. Nessun invio automatico. Versione confermata sempre rigenerabile dai dati storici, anche dopo modifiche correnti. Per correggerla: Crea versione rettificativa, motivo obbligatorio, rettifica sorgenti, aggiorna/controlla e conferma nuova versione. La vecchia non viene sovrascritta. Una voce ha una sola attribuzione immobile/periodo: le versioni dello stesso periodo non si sommano.

I sorgenti già confermati si modificano solo con versione rettificativa aperta. Non si spostano liberamente ad altro immobile/periodo dopo conferma: un trasferimento richiede una riconciliazione esplicita, non viene improvvisato un doppio conteggio. Le nuove riscossioni sono ammesse dopo la conferma, conservando i documenti precedenti. Gli storni di compensazioni già attribuite richiedono rettifica.

## Regole economiche rispettate

- Prenotazione interamente nel mese/anno del CHECK-IN, anche tra mesi o anni; nessuna divisione automatica.
- Spese per competenza assegnata; compensazioni e altri pagamenti per competenza dei movimenti.
- Tassa esterna al lordo e alla base commissionabile; nessun’altra esclusione inventata dal prezzo registrato.
- OTA effettiva prioritaria, compreso zero. Netto = lordo − OTA; compenso = netto × aliquota storica.
- HALF_UP per singola prenotazione, poi somma delle righe. Totali mai indipendenti dal dettaglio.
- Spese rimborsabili nel saldo solo se anticipate da Elledue e rimborsabili dal proprietario; pulizie/lavanderia sono singole righe contabili.
- Riscossione non equivale a compensazione. Riversamento separato, senza seconda sottrazione dal saldo.
- Credito negativo mostrato come importo da riconciliare, senza ordine di bonifico.
- Spese/compensazioni collegate a soggiorni di altri mesi conservano ospite e riferimenti nello snapshot, senza conteggiare di nuovo il compenso della prenotazione.

Esempi verificati: **1000 − 195 = 805 €; 20% = 161 €**. **161 + 24 + 10 + 5 − 30 = 170 €**. La sola riscossione, senza compensazione riconosciuta, lascia il saldo a 200 €.

## PDF e logo autentico

Originali PNG effettivamente recuperati da GitHub, conservati inalterati nella radice del repository. Logo trasparente orizzontale e simbolo del verticale riutilizzati, senza ridisegno/deformazione; alpha/proporzioni preservate e dorato normalizzato al #DBBF8B richiesto, su blu #111B32. [Origini e hash](brand/ORIGINALS.md). Icona adattiva, variante monocromatica Android, splash e intestazioni integrate. Test dell’inflazione su API 26 e rendering su API 34, sfondi chiari/scuri.

PDF A4 professionale generato con PDFBox Android e font DejaVu incorporati/licenziati, non screenshot dell’app. Intestazione con logo/immobile/proprietario/periodo/emissione/ID/versione, elenco soggiorni completo, lordo/OTA/netto/compenso, pulizie, lavanderia, altre spese, tassa/movimenti e saldo conclusivo evidente. Ritorno a capo, intestazioni di tabella ripetute e continuazione di righe molto lunghe. Il PNG del logo è conservato nello snapshot storico.

Artefatto semplice e rettificato: 3 pagine ciascuno. Prova lunga: **18 pagine**, 85 ospiti sintetici, nominativo e set molto lunghi, nota estesa. Verifica esterna con PyMuPDF: tutti gli 85 identificativi presenti, nota finale presente, coordinate testuali entro i margini. L’originale personale (2 pagine) resta in allegati, fuori dal repository; non è importato né pubblicato.

## Backup e ripristino reali

Impostazioni → Esporta backup / Importa. Include tutte le 16 tabelle applicative: proprietari, immobili/impostazioni, portali/testi, prenotazioni/parametri storici, categorie/spese/interventi, tassa, riscossioni/compensazioni/riversamenti/pagamenti, rettifiche, attività già presenti, rendiconti e ogni snapshot/versione/attribuzione. Include il logo incorporato negli snapshot. Esclusi APK/PDF esportati (rigenerabili), SDK, cache e keystore. Allegati/scontrini non sono implementati, quindi non esistono file omessi silenziosamente.

Export transazionale JSON, formato 1/database 4, SHA-256 del payload, limite 32 MiB. Il checksum rileva corruzione, non autentica il mittente: usare una copia attendibile. Il file non è cifrato dall’app e contiene dati personali: conservarlo privatamente. Nessuna trasmissione automatica; destinazione scelta tramite Storage Access Framework.

Prima del ripristino: validazione formato/versione/checksum/struttura e caricamento in database Room isolato; verifica foreign key, importi, attribuzioni e versioni confermate. File corrotto, incompatibile o incoerente respinto senza modificare i dati esistenti. Conferma esplicita della sostituzione; copia dei dati attuali esportabile prima di procedere. Ripristino in un’unica transazione, non merge parziale. Gli ID e gli snapshot storici sono conservati; prova su database a file chiuso/riaperto superata.

## Firma futura sotto controllo dell’utente

Nessuna chiave release generata nel cloud. Gradle usa keystore esterno al repository, variabili ELLEDUE_RELEASE_* o parametri temporanei del wizard Android Studio; pacchetto release bloccato senza configurazione. [Procedura Windows](RELEASE_SIGNING_WINDOWS.md): creazione locale guidata, stessa chiave per aggiornamenti, seconda copia privata/cifrata, credenziali separate e confronto certificati APK.

Minificazione release R8 eseguita e riuscita, senza firmare un APK release. Regole specifiche per codec JPEG-2000 opzionale non incluso/usato; nessun controllo contabile o test disabilitato. La verifica del blocco firma ha respinto come previsto l’assenza di keystore. Certificato debug dell’APK M4 confrontato con M3: **identico**. L’aggiornamento su telefono fisico non è stato eseguito. Il passaggio debug → release con certificato diverso richiede backup e ripristino verificato prima di disinstallare.

## Verifiche effettivamente eseguite

Dopo `source scripts/cloud-env.sh`:

`./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:minifyReleaseWithR8`

- **Build riuscita**, APK 0.4.0 generato, firma v2 verificata con apksigner.
- **98 test passati, 0 falliti, 0 errori, 0 saltati**: 71 precedenti più 27 nuovi (rendiconti 13, backup 6, PDF 2, migrazione v3 1, UI 2, marchio 3).
- Bozze/ricalcolo, OTA percentuale/effettiva/zero, aliquote storiche, arrotondamenti per riga, esempio 170 €, competenza tra mesi/anni, annullamenti, spese/anticipi, riscossione senza compensazione, riversamento indipendente, pagamenti/credito, nessun doppio conteggio, conferma/versioni/immutabilità, dati storici e prenotazioni collegate fuori periodo.
- Backup esportato/ripristinato, relazioni e ID, file corrotti/versioni non supportate, snapshot senza attribuzioni respinti, dataset preesistente sostituito solo dopo conferma e persistenza dopo riapertura.
- Migrazioni **1 → 2 → 3 → 4**, **2 → 3 → 4**, **3 → 4** da schemi pubblicati popolati: validazione Room e conservazione contabilità/audit riuscite. Schemi 1/2/3 originali invariati; schema 4 tracciato. Nessuna migrazione distruttiva.
- UI reale Compose/Robolectric API 34: generazione/controllo/conferma, rettifica del lordo sorgente con ricalcolo, versione precedente immutabile, backup manuale esposto, tema chiaro/scuro. Screenshot ispezionati.
- PDF veri generati dai medesimi componenti dell’app; rigenerazione storica dopo rettifica, impaginazione e contenuti controllati.
- **Android Lint: 0 errori, 3 avvisi**, tutti in classi EST TLS della dipendenza transitiva BouncyCastle. Non soppressi; l’app non usa tali helper e non ha permesso Internet.

XML/HTML test in app/build/test-results/testDebugUnitTest e app/build/reports/tests/testDebugUnitTest; lint in app/build/reports/lint-results-debug.txt. Artefatti verificati e copiati nella consegna GitHub.

## Limiti dichiarati

APK preliminare/debug; nessuna prova su telefono fisico/emulatore. Il sistema documenti Android è integrato, mentre il ripristino e i PDF sono verificati automaticamente nel backend/Robolectric: resta da provare il selettore file e l’aggiornamento su dispositivo reale. Chiave release ancora da creare sul PC e recuperabilità privata da verificare; non dichiarata distribuzione aziendale definitiva pronta. Backup non cifrato, limite 32 MiB/versione database 4; non si fondono dataset diversi. Spostamenti di voci già confermate fra periodi non automatici. Notifiche, scadenzario completo, sincronizzazione, OTA e invii automatici non sviluppati.

La composizione del lordo è risolta dall’utente; resta aperta soltanto la discrepanza OTA storica di **76,34 €**, senza alterare il vecchio documento. Il nuovo motore deriva i totali dai dettagli registrati. **Fermata prima della Milestone 5**.
