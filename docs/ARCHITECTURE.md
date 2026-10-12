# Architettura e regole di dominio

## Base Android

Kotlin, Compose/Material 3, Navigation Compose, Room, Coroutines, ViewModel e StateFlow. Composizione delle dipendenze esplicita in `ElledueApplication`: nessun framework DI è necessario per questa prima scala. Separazione tra UI, dominio e persistenza nello stesso modulo Gradle; i confini possono essere estratti in moduli senza cambiare il dominio.

`PropertyRepository` valida prima del salvataggio; `PropertyDao.save` salva proprietario, immobile, tariffe portali e testi in una transazione. Gli errori lasciano intatti i dati precedenti. L’archiviazione è un flag, mai una cancellazione, ed è reversibile. I campi del modulo rimangono durante la ricreazione dell’Activity; uscire con modifiche richiede conferma.

## Modello persistente

| Tabella | Relazioni e stato |
| --- | --- |
| owners | UUID del proprietario, nome, telefono, email. Ogni nuova scheda crea un record; nessuna fusione automatica di omonimi. |
| properties | UUID stabile; ownerId obbligatorio; identità, tariffe, commissioni in basis point, tassa e orari; archived; createdAt/updatedAt. |
| platform_commissions | Chiave immobile/portale; percentuale specifica. |
| message_templates | Chiave immobile/tipologia/lingua; testi reali inseriti dall’operatore, senza codici inventati. |
| reservations | Modulo attivo; immobile obbligatorio, nominativo e ospiti, portale, date epoch-day, orari previsti/effettivi, stato, note, importi in centesimi e percentuali applicate fotografate sulla prenotazione. |
| expenses | Modulo attivo: una riga per acquisto/intervento, immobile obbligatorio, prenotazione opzionale dello stesso immobile, data/competenza, categoria e tipo storici, quantità/tariffa, costo calcolato/effettivo, flag anticipo/rimborso, note, versione e annullamento. |
| activities | Attività locali utilizzabili; immobile opzionale, prenotazione opzionale coerente, completamento, istante e fuso Europe/Rome. Ricorrenze e notifiche future. |
| reports | Modulo attivo: immobile, periodo/versione univoci, bozza/confermato, predecessore, motivo rettifica, note, emissione e snapshot immutabile. |

La tabella activities supporta ora l’operatività locale; ricorrenze, notifiche e collaborazione richiedono le milestone successive. Le rispettive milestone aggiungeranno campi e migrazioni per gli ulteriori requisiti (ospiti tassabili, orari effettivi, riscossioni, pagamenti, quantità, allegati, attribuzioni ai rendiconti, ricorrenze). Le foreign key RESTRICT impediscono orfani; l’eliminazione immobile è esposta soltanto con conferma e verifica transazionale dell’assenza di storico. Archiviare non modifica prenotazioni o spese.

Room è alla versione 5, con schemi 1, 2, 3, 4 e 5 esportati e committati. `MIGRATION_1_2` aggiunge campi alle prenotazioni e ricostruisce gli importi economici dai parametri già salvati; preserva le tabelle e i dati della Milestone 1. Un test crea il database dallo schema originale e verifica la migrazione reale con validazione Room. Non usare `fallbackToDestructiveMigration`. Ogni cambio successivo deve incrementare la versione, avere migrazione esplicita, schema esportato e test da ogni versione già distribuita.

`ReservationRepository` salva in transazione, verifica immobile, capacità e conflitti sull’intervallo [check-in, check-out). Annullare mantiene lo storico e libera l’intervallo. Le prenotazioni annullate sono escluse dai riepiloghi economici e dalle occupazioni. Il consenso a sovrapposizioni deve corrispondere agli ID e timestamp correnti dei conflitti: nuovi conflitti o modifiche richiedono una nuova conferma. I campi fiscali e l’attribuzione contabile sono predisposti, senza flussi operativi della Milestone 3 o regole di competenza attive.

UUID e timestamp preparano la futura sincronizzazione; la versione attuale è esclusivamente locale per singolo operatore. Non è implementata alcuna sincronizzazione.

## Importi, commissioni e contabilità futura

EUR in centesimi `Long`; percentuali in basis point `Int` (20% = 2000). Parsing con BigDecimal, al massimo due decimali; percentuali da 0 a 100%; arrotondamento commissioni HALF_UP al centesimo. Nessun Float/Double nei calcoli contabili. Le tariffe dell’immobile sono solo valori predefiniti: le prenotazioni conservano uno snapshot dei parametri e degli importi applicati.

Priorità OTA: importo effettivo manuale > percentuale della prenotazione > tariffa portale/immobile > tariffa immobile. Lordo − OTA = base commissionabile; compenso Elledue = base × aliquota gestione. Esempio verificato dal test: 1000 € − 195 € = 805 €; 20% = 161 €.

Il proprietario riceve i pagamenti OTA. Elledue matura il compenso e riceve un bonifico successivo: lordo, netto proprietario, compenso, anticipi e pagamenti devono restare distinti.

Tassa: 3 €/persona/notte soggetta, dai 16 anni compiuti inizialmente, limiti/esenzioni configurabili per immobile. Nessuna età individuale richiesta; conteggi soggetti/esenti, notti e rettifica manuale motivata sono operativi. La tassa rimane da incassare fino alla conferma manuale. Riscossione, compensazione e riversamento sono stati separati; la tassa non è ricavo né spesa. Ogni compensazione è collegata alla riscossione sorgente; la somma delle compensazioni attive non può eccedere la riscossione. Il riversamento ha un soggetto da configurare e disponibilità verificata separatamente; non genera una seconda sottrazione dal saldo.

Spese: immobile obbligatorio, quantità e tariffa storiche, costo effettivo confermato, anticipo Elledue e rimborsabilità distinti. Pulizie iniziali 8 €/ora; lavanderia 5 €/set matrimoniale. Una sola registrazione contabile per intervento; la vista pulizie non crea una seconda spesa.

Saldo rendiconto = commissioni Elledue + spese anticipate rimborsabili − tassa riscossa e riconosciuta in compensazione − anticipi/pagamenti già riconosciuti. Esempio obiettivo milestone 4: 161 + 24 + 10 + 5 − 30 = 170 €. Un saldo negativo è un credito da riconciliare, mai un ordine automatico di bonifico.

## Competenza definitiva: mese del check-in

Decisione dell’utente nella Milestone 3: ogni prenotazione appartiene interamente al mese e anno del check-in. Esempio 29 novembre → 3 dicembre: novembre; 30 dicembre → 5 gennaio: dicembre dell’anno precedente. Nessuna divisione automatica e una sola commissione. `competenceMonth` è salvato e ricalcolato quando cambia il check-in; la migrazione lo ricostruisce per le prenotazioni esistenti. Le spese hanno una competenza indipendente, normalmente il mese della data effettiva, modificabile esplicitamente. Compensazioni e altri anticipi hanno una competenza assegnata dal movimento.

La Milestone 4 implementa bozze, snapshot confermati e attribuzioni univoche. Le versioni storiche non vengono sommate: per lo stesso immobile/periodo si usa la versione confermata più recente, mantenendo le precedenti consultabili.

Rendiconti: bozza modificabile con ricalcolo, conferma con snapshot immutabile, rettifica come nuova versione. PDF multipagina con blu/dorato, logo sostituibile, tabelle leggibili e nessun invio automatico.

## Date, notifiche, sicurezza

Date soggiorno `LocalDate`/epoch-day; orari `LocalTime`; scadenze istanti con fuso Europe/Rome, conversioni esplicite e test di ora legale già presenti per le date; notifiche nelle milestone successive. Minimo operativo due notti configurabile, non vincolo irreversibile dello schema.

Notifiche future: WorkManager per lavoro differibile e AlarmManager solo quando giustificato; permessi runtime, riavvio, aggiornamento/annullamento, identificativi unici e attività completate. Nessuna garanzia assoluta su force-stop o risparmio energetico. Comparsa di un promemoria non prova un adempimento. Nessun collegamento automatico ad Airbnb/Booking, WhatsApp, email o portali pubblici.

Nessun permesso Internet nell’app. Backup automatico Android disabilitato per evitare copie non controllate dei dati personali. Export/import manuale tramite Storage Access Framework anticipato e verificato nella Milestone 4. Documenti ospiti/allegati devono stare in storage privato e avere politiche di accesso e inclusione backup esplicite. Credenziali e keystore sono esclusi da Git.

## Contabilità operativa e audit della Milestone 3

`AccountingRepository` opera in transazione. `expense_categories` contiene categorie reali personalizzabili: il seed idempotente non sovrascrive nomi modificati e non crea dati dimostrativi. La categoria distingue OTHER/CLEANING/LAUNDRY; il tipo non cambia retroattivamente. Pulizie e lavanderia sono viste di `expenses`, non registrazioni duplicate; definizione set modificabile per intervento. Importi effettivi prevalgono sui calcoli, quantità decimali precise e set interi positivi. Le rettifiche richiedono motivazione e revisione corrente; `expense_revisions` conserva ogni snapshot, anche per annullamenti. Spese annullate restano consultabili ma non vengono sommate.

`tax_assessments` conserva tariffa, soglia di età, limiti/esenzioni e conteggi confermati della prenotazione, con storico `tax_assessment_revisions`. Nuove impostazioni dell’immobile non riscrivono i parametri confermati. Cambiare date/capienza/immobile richiede revisione della tassa; trasferire a un altro immobile è bloccato finché esistono movimenti attivi. Una revisione dopo un trasferimento senza movimenti usa le regole del nuovo immobile. Non viene mai generata una riscossione al check-out o all’annullamento.

`money_movements` è append-only: COLLECTION, COMPENSATION, REMITTANCE, OWNER_PAYMENT e REVERSAL. Gli ID sono idempotenti; uno storno univoco referenzia l’originale, richiede motivazione e non elimina lo storico. Prima di stornare una riscossione occorre stornare le compensazioni/riversamenti collegati. Le riscossioni eccedenti il previsto sono bloccate fino a rettifica motivata della previsione; ridurre una previsione già riscossa segnala l’eccedenza da riconciliare. Annullare una prenotazione conserva i movimenti e consente gli storni.

I riepiloghi si ricavano dal dettaglio: commissioni per check-in, spese per competenza, compensazioni e anticipi per competenza del movimento. Tassa prevista/riscossa/residua riguarda i soggiorni con check-in nel mese, anche se riscossa successivamente. La vista include riscossioni storiche di prenotazioni annullate per non nascondere denaro effettivamente raccolto; previsioni/residui e commissioni annullate sono esclusi dai totali operativi. Le tasse non configurate e da rivedere sono segnalate, senza interpretare la mancanza di dati come previsione zero. Saldo operativo = compensi + anticipi rimborsabili − compensazioni riconosciute − altri pagamenti. Saldi negativi sono crediti da riconciliare, non bonifici negativi.

Le rettifiche economiche delle prenotazioni richiedono motivazione; `booking_financial_changes` conserva snapshot prima/dopo. I riepiloghi sommano i nuovi valori correnti, non snapshot vecchi e nuovi insieme. Scheda prenotazione mostra le rettifiche: nessun totale separato incompatibile con le righe.

La migrazione `MIGRATION_2_3` è additiva, conserva categorie testuali delle vecchie spese e tutte le tabelle precedenti. Non inventa movimenti dalle colonne fiscali della versione 2, che non aveva un flusso di riscossione. I test aprono database dallo schema originale 1 e 2, verificano la validazione Room 3 e la conservazione di dati e impostazioni.

## Rendiconti, PDF e backup — Milestone 4

`ReportRepository` genera una sola bozza attiva per immobile/periodo e raccoglie prenotazioni per check-in, spese e riconoscimenti per competenza. `ReportSnapshot` conserva proprietà/proprietario, aliquote e dettagli economici, note, movimenti, emissione, versione e PNG del logo usato: non riferisce solamente gli ID correnti. Gross/OTA/netto/compensi sono somme delle righe; tassa sempre esterna al lordo. `ReportSnapshot.issues` rileva incoerenze, OTA stimate, dati fiscali mancanti/da rivedere e somme non valide. Gli avvisi devono essere riconosciuti; gli errori bloccano la conferma.

Conferma in transazione, previa verifica SHA-256 della bozza rispetto ai sorgenti correnti: se cambiati, aggiornare e ricontrollare. `report_allocations` ha chiave (tipo, ID sorgente), un solo immobile/periodo per voce. Le versioni successive riutilizzano l’attribuzione, non la duplicano. `ReportGuard` permette di correggere una sorgente già attribuita solo dopo apertura motivata di una versione rettificativa del periodo; la nuova conferma chiude tale possibilità. Non sono ammessi spostamenti liberi fra periodi per voci già confermate: serve una riconciliazione esplicita, non un doppio conteggio. Riscossioni successive possono essere registrate senza riscrivere documenti storici; compensazioni già attribuite si stornano nel flusso rettificativo.

Il PDF è generato con PDFBox Android (Apache 2), font DejaVu incorporati e grafica vettoriale; non è uno screenshot. Tabelle con ritorno a capo, intestazioni ripetute, righe molto lunghe proseguite su più pagine e importi allineati. Ultima sezione: saldo o credito da riconciliare. Il PNG originale derivato conserva proporzioni e alpha, con normalizzazione al dorato ufficiale richiesto. La bozza PDF è marcata; la confermata si rigenera dallo snapshot storico. Il codec JPEG-2000 opzionale non è incluso né usato: soltanto PNG aziendali e font incorporati. Regole R8 limitate alle due classi opzionali del codec; nessun controllo contabile o test disabilitato.

Backup JSON versione formato 1/database 4: tutte le 16 tabelle applicative, inclusi audit, movimenti, attribuzioni e snapshot con logo. Export transazionale, checksum del payload UTF-8, limite 32 MiB. Import: formato/versione/checksum, whitelist tabelle/colonne/tipi, caricamento in Room separato, foreign-key check e validazioni economiche. Dopo conferma, sostituzione atomica, senza unione di dataset diversi. Un file non valido non altera il database corrente. Nessuna copia di WAL live, nessun upload automatico, nessun allegato implementato da escludere silenziosamente. APK/PDF esportati, SDK e keystore non fanno parte del backup. Il file non è cifrato: conservazione privata scelta dall’utente.

Release: keystore esterno, env ELLEDUE_RELEASE_* o parametri del wizard Android Studio; blocco della creazione del pacchetto senza configurazione. Nessuna generazione della chiave definitiva nel cloud. Procedura PC/Windows e verifica certificati in RELEASE_SIGNING_WINDOWS.md.

Per interventi e riconoscimenti collegati a soggiorni di un altro periodo, `linkedBookings` conserva riferimenti storici separati: il PDF mostra l’ospite, senza sommare di nuovo lordo/OTA/compenso o attribuire la prenotazione al mese della spesa.

## Evoluzione Milestone 4.5

Room 4→5 aggiunge esclusivamente foto e profilo locale e collegamento attività/prenotazione.
`LocalImages` normalizza orientamento, lato e peso delle immagini selezionate; i byte JPEG
base64 in Room sono durevoli e inclusi nel backup atomico. I vecchi backup database 4
sono aggiornati in staging con campi vuoti; snapshot e contabilità restano invariati.

`PortfolioAnalytics` separa la competenza finanziaria al check-in dall’occupazione reale
sul calendario [check-in, check-out), conteggiando giorni distinti anche in presenza di
sovrapposizioni autorizzate. Non conosce la disponibilità commerciale.

`ReportLayout` seleziona soltanto l’impaginazione, senza una seconda fonte economica.
`PdfPreviewDocument` legge i byte PDF effettivamente generati, dipinge tutte le pagine
su bitmap Android e le mostra pigramente nel visualizzatore Compose. Le immagini RGB
sono convertite tramite Color.rgb per evitare dipendenze dall’ordine dei byte del
motore grafico host. Nessun server, account, invio o visualizzatore esterno obbligatorio.

Navigazione e futura integrazione staff/documenti/partner: [mappa M4.5](product/NAVIGATION_4_5.md).
