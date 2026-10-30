# Elledue Control

## 0.4.11 — consegna finale dei tre interventi

APK debug verificato,263test PASS, schema Room10 e firma precedente conservati. Eliminazioni comprensibili, bozze separate e diagnostica delle notifiche con prova programmata. La ricezione su telefono resta da verificare: non viene dichiarata risolta dai soli test Robolectric.

[APK, SHA, aggiornamento e consegna](docs/0.4.11/DELIVERY.md) · [QA finale](docs/0.4.11/intervento-3/QA.md) · [Checklist notifiche sul telefono](docs/0.4.11/intervento-3/NOTIFICHE-TELEFONO.md) · [PR pronta per revisione](docs/0.4.11/PR.md)

## 0.4.11 — intervento 2/3: calendario e navigazione operativa

Sul medesimo branch `feature/0.4.11-user-feedback`, dalla consegna `df8a67e`: calendario con barre per immobile e cambi ospiti espliciti, Prenotazioni con ricerca storica e filtri indipendenti, Home con portafoglio e Da fare, Economia con Spese/Rendiconti/Pagamenti e registro Manutenzioni semplificato. Motore economico, PDF e schema Room10 dell'intervento1 conservati.

[Brief](docs/0.4.11/intervento-2/BRIEF.md) · [Scelte UX](docs/0.4.11/intervento-2/UX.md) · [Consegna, test e screenshot](docs/0.4.11/intervento-2/DELIVERY.md)

Intervento3/3 ora consegnato sullo stesso branch: vedere la consegna finale sopra. Nessun merge su main.

## 0.4.11 — intervento 1/3: motore economico e rendiconti

Branch `feature/0.4.11-user-feedback` dalla 0.4.10, senza merge. Trattamento OTA/IVA configurabile e applicato una sola volta; anticipo convenzionale automatico della tassa separato dagli incassi reali; PDF proprietario semplificati con saldo uniforme. Schema Room10, migrazione additiva e dati storici confermati preservati.

[Regole ed esempi](docs/0.4.11/intervento-1/ACCOUNTING.md) · [Migrazioni](docs/0.4.11/intervento-1/MIGRATION.md) · [Consegna e QA](docs/0.4.11/intervento-1/DELIVERY.md)

Il branch resta disponibile per gli interventi2/3 e3/3; questo checkpoint non pubblica una nuova release.


## 0.4.10 — automazioni economiche e UX operativa

Base 0.4.9, branch `feature/0.4.10-ux-accounting`, senza merge su main. Standard retroattivi eleggibili, costi proposti nei rendiconti e approvazione atomica in blocco, riscossione rapida della tassa, calendario compatto e form semplificati. Versione/code 0.4.10/10, schema Room9.

[Regole contabili](docs/0.4.10/ACCOUNTING.md) · [Migrazione e aggiornamento](docs/0.4.10/MIGRATION.md) · [Requisiti](docs/0.4.10/REQUIREMENTS.md) · [QA: 208 test PASS](docs/0.4.10/QA.md) · [Screenshot e PDF sintetici](docs/0.4.10/qa/README.md) · [PR per revisione](docs/0.4.10/PR.md)


## 0.4.9 — aggiornamento consolidato (branch dedicato)

Nuovi problemi/interventi e Giornata operativa, prenotazioni più rapide, previsioni pulizie/lavanderia separate dai consuntivi, contabilità e PDF aggiornati, occupazione effettiva e riparazione delle notifiche programmate. Database 8 con migrazione additiva e application ID invariato. Nessun merge su main.

[Brief](docs/0.4.9/BRIEF.md) · [Requisiti e prove](docs/0.4.9/REQUIREMENTS.md) · [Migrazione e aggiornamento](docs/0.4.9/MIGRATION.md) · [Changelog](docs/0.4.9/CHANGELOG.md) · [Bug](docs/0.4.9/BUGS.md) · [QA](docs/0.4.9/QA.md) · [Backlog](docs/0.4.9/BACKLOG.md)


Applicazione Android nativa per Elledue Management, gestione di immobili per affitti brevi. Il repository è stato creato da zero in Kotlin e Jetpack Compose.

## 0.4.8 — stabilizzazione operativa (branch dedicato)

Salvataggi verificati e bozze locali recuperabili, eliminazione completa con backup preventivo, calendario a barre, immobili propri e gestione separati, previsioni tassa, opzioni indipendenti nei rendiconti, notifiche Android e accesso protetto facoltativo. Room 7 con migrazione additiva; package ID e firma di aggiornamento conservati. Nessun merge su main o pubblicazione della Release.

Documentazione: [architettura](docs/0.4.8/ARCHITECTURE.md), [requisiti](docs/0.4.8/REQUIREMENTS.md), [bug e cause](docs/0.4.8/BUGS.md), [QA e stato consegna](docs/0.4.8/QA.md), [aggiornamento senza disinstallazione](docs/0.4.8/UPDATE.md), [screenshot nativi](docs/0.4.8/qa/README.md). Il report QA distingue controlli automatici, prove sul telefono e disponibilità effettiva del download.

## 0.4.7 — rifinitura operativa (branch dedicato)

Copertine Home selezionabili, attività complete, copia calendario per appartamento, portali e OTA esplicite, messaggi ospiti IT/EN, analisi e PDF sintetico raffinati. Design0.4.6 mantenuto. Room6 con migrazione additiva e import backup4/5/6; ID Android invariato. Nessun merge o APK pubblico della0.4.7.

Documentazione: [changelog](docs/0.4.7/CHANGELOG.md), [matrice e verifiche](docs/0.4.7/REQUIREMENTS.md), [aggiornamento senza disinstallazione](docs/0.4.7/UPDATE.md), [asset](docs/0.4.7/ASSETS.md).

## Milestone 1–4.5 e redesign vincolato al concept — base 0.4.6

Modulo Immobili: creazione, elenco con ricerca e filtri, dettaglio, modifica, archiviazione e riattivazione. Sono persistenti proprietario e recapiti, posti letto/ospiti, orari, note, commissioni, tariffe pulizie/lavanderia, tassa di soggiorno, commissioni diverse per portale e istruzioni di accesso IT/EN. Nessun dato dimostrativo viene inserito nell’app.

Prenotazioni: inserimento, dettaglio, modifica e annullamento senza cancellare lo storico. Immobile obbligatorio, ospiti, portale, date, orari previsti/effettivi, note e stato. Calendario mensile ed elenco con filtro immobile, arrivi/partenze, soggiorni e accesso ai dettagli. Le sovrapposizioni richiedono una conferma esplicita; il cambio ospiti nella stessa giornata è consentito.

Le commissioni sono calcolate in centesimi, con anteprima immediata e importo OTA effettivo prioritario. Aliquote e importi applicati sono conservati nella prenotazione: cambiare le impostazioni dell’immobile non modifica lo storico. Dashboard ed Economia mostrano dati registrati, distinguendo lordo/netto del proprietario dal compenso Elledue. Non sono incassi confermati di Elledue né rendiconti mensili.

Contabilità operativa nella sezione **Economia**: riepilogo per mese/immobile, inserimento rapido spese, categorie personalizzabili, pulizie e lavanderia come singole registrazioni contabili. Quantità e tariffe storiche, costo effettivo prioritario, anticipo e rimborsabilità distinti. Rettifiche motivate con snapshot e annullamento senza cancellazione. Definizione dei set modificabile per intervento.

Tassa di soggiorno: conteggi soggetti/esenti e notti, parametri dell’immobile fotografati alla conferma, previsione rettificabile con motivo. Riscossioni manuali anche parziali, compensazioni collegate univocamente alle riscossioni, riversamento separato con soggetto configurabile, storico e storni motivati. Nessun incasso automatico al check-out. Registrabili anche anticipi/pagamenti del proprietario.

Competenza definitiva: ogni prenotazione appartiene interamente al mese/anno del check-in; ogni spesa ha il proprio mese di competenza modificabile. Il saldo operativo deriva dal dettaglio e distingue le compensazioni dalla riscossione. Non è ancora un rendiconto confermato. Le rettifiche economiche delle prenotazioni richiedono motivazione e conservano snapshot prima/dopo.

Navigazione: Home, Immobili, Calendario, Economia, con + centrale contestuale. Profilo locale, centro notifiche futuro e impostazioni nell’intestazione. Attività locali dalla Home o dalla prenotazione; scheduler ricorrente e notifiche non ancora implementati. Economia separa Analisi, Contabilità e Rendiconti. [Mappa completa](docs/product/NAVIGATION_4_5.md).

Rendiconti: da Economia → Rendiconti mensili, selezionare immobile e periodo. Generazione bozza completa, aggiornamento dai sorgenti, criticità/OTA stimate, note, conferma manuale con snapshot immutabile, rettifiche come nuove versioni e attribuzioni univoche. Le correzioni economiche avvengono nei sorgenti: non esistono totali PDF indipendenti dai dettagli. Versioni storiche rigenerabili; PDF professionale A4 con font incorporati, logo originale e tabelle multipagina. Nessun invio automatico.

Backup manuale da Impostazioni: esportazione dell’intero database applicativo (incluse revisioni, movimenti, rendiconti e snapshot); validazione in database isolato e ripristino atomico dopo conferma. File JSON con checksum SHA-256, non cifrato: conservare privatamente. Nessun upload automatico. Gli allegati/scontrini non sono ancora implementati.

Identità visiva: blu notte `#111B32`, dorato `#DBBF8B`, tema chiaro/scuro. Originali PNG caricati dall’utente su GitHub, integrati senza ridisegno né deformazione in icona adattiva/splash, intestazione e PDF. Derivazioni e hash descritti in [docs/brand/ORIGINALS.md](docs/brand/ORIGINALS.md).

Consegna precedente0.4.6 scaricabile in [deliverables/redesign-0.4.6](deliverables/redesign-0.4.6): APK debug, PDF esclusivamente sintetici, screenshot e manifest con checksum. Il rendiconto personale originale non è nel repository.

Foto copertina e profilo conservate localmente e incluse nei backup. Portfolio con menu azioni e cancellazione definitiva bloccata in presenza di storico. Analisi mensili reali con confronti, trend e occupazione del calendario, distinta dalla disponibilità commerciale. Due PDF (sintetico/dettagliato) dallo stesso snapshot, miniatura reale e visualizzatore interno multipagina con ingrandimento. [Consegna e limiti M4.5](docs/MILESTONE_4_5.md).

Redesign del concept: dark principale anche con tema Android chiaro, DM Serif Display e Inter incorporati, hero fotografico, KPI 2×2, azioni a icone, portfolio con testo sovrapposto, calendario a pill, timeline e contenitori/dialoghi dedicati. Database, dominio e backup invariati. [Corrispondenze e adattamenti](docs/design/CONCEPT_BINDING.md).

## Build e test

- JDK 17 o 21 completo (non solo JRE).
- Android SDK 35, Build Tools 35.0.0; minimo Android 8.0 / API 26.
- Gradle 8.11.1 tramite wrapper con checksum SHA-256.
- Application ID stabile: `it.elledue.control`.

Aprire in Android Studio oppure impostare `ANDROID_HOME` sul proprio SDK e poi:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

APK installabile: `app/build/outputs/apk/debug/app-debug.apk`. Firma debug locale; nessuna chiave è versionata. Il debug APK è preliminare e non una release per la produzione. Certificato debug confrontato con M4; aggiornamento sul telefono ancora da provare. APK release con firma privata esterna al repository nella milestone finale.

I test comprendono persistenza su database a file riaperto, CRUD/archiviazione immobili, prenotazioni, annullamenti, conflitti e consenso esplicito, cambio ospiti, snapshot economici, date/DST, arrotondamento, migrazione Room 1 → 2 con dati preesistenti e navigazione Compose, contabilità operativa, movimenti/storni, competenza al check-in e migrazioni fino alla versione 5, bozze/conferme/versioni, ripristino e PDF reali multipagina. I test Android sono eseguiti con Robolectric; non equivalgono a una verifica su telefono fisico.

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
- `docs/MILESTONE_2.md`: consegna storica della Milestone 2.
- `docs/MILESTONE_3.md`: consegna storica della contabilità operativa.
- `docs/MILESTONE_4.md`: consegna e verifiche correnti.
- `docs/RELEASE_SIGNING_WINDOWS.md`: keystore definitivo sul PC, copia privata recuperabile e aggiornamenti.
- `docs/REFERENCE_REPORT_REVIEW.md`: incongruenze del PDF di riferimento, senza dati personali.

I dati rimangono sul dispositivo nel sandbox Android. Non sono presenti sincronizzazione, documenti ospiti, scontrini/allegati o notifiche. Prima di disinstallare o cambiare dispositivo/firma, esportare e provare il ripristino del backup. Le nuove versioni devono mantenere lo stesso application ID e la stessa firma per aggiornare l’installazione senza cancellarli.
