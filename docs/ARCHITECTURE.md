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
| activities | Base per milestone 6; immobile opzionale, istante di scadenza e fuso Europe/Rome. |
| reports | Base per milestone 4; immobile obbligatorio, periodo/versione univoci e snapshot. |

Le tabelle activities e reports sono fondamenta di schema, non moduli già utilizzabili. Le rispettive milestone aggiungeranno campi e migrazioni per gli ulteriori requisiti (ospiti tassabili, orari effettivi, riscossioni, pagamenti, quantità, allegati, attribuzioni ai rendiconti, ricorrenze). Le foreign key RESTRICT impediscono orfani; non è esposto alcun comando di cancellazione immobile. Archiviare non modifica prenotazioni o spese.

Room è alla versione 3, con schemi 1, 2 e 3 esportati e committati. `MIGRATION_1_2` aggiunge campi alle prenotazioni e ricostruisce gli importi economici dai parametri già salvati; preserva le tabelle e i dati della Milestone 1. Un test crea il database dallo schema originale e verifica la migrazione reale con validazione Room. Non usare `fallbackToDestructiveMigration`. Ogni cambio successivo deve incrementare la versione, avere migrazione esplicita, schema esportato e test da ogni versione già distribuita.

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

Nessun PDF o rendiconto confermato è implementato. La Milestone 4 dovrà aggiungere attribuzioni univoche a snapshot/versioni e impedire di sommare versioni precedenti; i repository impediscono modifiche dirette ai record con attribuzione già valorizzata.

Rendiconti: bozza modificabile con ricalcolo, conferma con snapshot immutabile, rettifica come nuova versione. PDF multipagina con blu/dorato, logo sostituibile, tabelle leggibili e nessun invio automatico.

## Date, notifiche, sicurezza

Date soggiorno `LocalDate`/epoch-day; orari `LocalTime`; scadenze istanti con fuso Europe/Rome, conversioni esplicite e test di ora legale già presenti per le date; notifiche nelle milestone successive. Minimo operativo due notti configurabile, non vincolo irreversibile dello schema.

Notifiche future: WorkManager per lavoro differibile e AlarmManager solo quando giustificato; permessi runtime, riavvio, aggiornamento/annullamento, identificativi unici e attività completate. Nessuna garanzia assoluta su force-stop o risparmio energetico. Comparsa di un promemoria non prova un adempimento. Nessun collegamento automatico ad Airbnb/Booking, WhatsApp, email o portali pubblici.

Nessun permesso Internet nell’app. Backup automatico Android disabilitato per evitare copie non controllate dei dati personali. Export/import manuale e verificato tramite Storage Access Framework nella milestone di qualità, prima dell’uso con dati irrecuperabili. Documenti ospiti/allegati devono stare in storage privato e avere politiche di accesso e inclusione backup esplicite. Credenziali e keystore sono esclusi da Git.

## Contabilità operativa e audit della Milestone 3

`AccountingRepository` opera in transazione. `expense_categories` contiene categorie reali personalizzabili: il seed idempotente non sovrascrive nomi modificati e non crea dati dimostrativi. La categoria distingue OTHER/CLEANING/LAUNDRY; il tipo non cambia retroattivamente. Pulizie e lavanderia sono viste di `expenses`, non registrazioni duplicate; definizione set modificabile per intervento. Importi effettivi prevalgono sui calcoli, quantità decimali precise e set interi positivi. Le rettifiche richiedono motivazione e revisione corrente; `expense_revisions` conserva ogni snapshot, anche per annullamenti. Spese annullate restano consultabili ma non vengono sommate.

`tax_assessments` conserva tariffa, soglia di età, limiti/esenzioni e conteggi confermati della prenotazione, con storico `tax_assessment_revisions`. Nuove impostazioni dell’immobile non riscrivono i parametri confermati. Cambiare date/capienza/immobile richiede revisione della tassa; trasferire a un altro immobile è bloccato finché esistono movimenti attivi. Una revisione dopo un trasferimento senza movimenti usa le regole del nuovo immobile. Non viene mai generata una riscossione al check-out o all’annullamento.

`money_movements` è append-only: COLLECTION, COMPENSATION, REMITTANCE, OWNER_PAYMENT e REVERSAL. Gli ID sono idempotenti; uno storno univoco referenzia l’originale, richiede motivazione e non elimina lo storico. Prima di stornare una riscossione occorre stornare le compensazioni/riversamenti collegati. Le riscossioni eccedenti il previsto sono bloccate fino a rettifica motivata della previsione; ridurre una previsione già riscossa segnala l’eccedenza da riconciliare. Annullare una prenotazione conserva i movimenti e consente gli storni.

I riepiloghi si ricavano dal dettaglio: commissioni per check-in, spese per competenza, compensazioni e anticipi per competenza del movimento. Tassa prevista/riscossa/residua riguarda i soggiorni con check-in nel mese, anche se riscossa successivamente. La vista include riscossioni storiche di prenotazioni annullate per non nascondere denaro effettivamente raccolto; previsioni/residui e commissioni annullate sono esclusi dai totali operativi. Le tasse non configurate e da rivedere sono segnalate, senza interpretare la mancanza di dati come previsione zero. Saldo operativo = compensi + anticipi rimborsabili − compensazioni riconosciute − altri pagamenti. Saldi negativi sono crediti da riconciliare, non bonifici negativi.

Le rettifiche economiche delle prenotazioni richiedono motivazione; `booking_financial_changes` conserva snapshot prima/dopo. I riepiloghi sommano i nuovi valori correnti, non snapshot vecchi e nuovi insieme. Scheda prenotazione mostra le rettifiche: nessun totale separato incompatibile con le righe.

La migrazione `MIGRATION_2_3` è additiva, conserva categorie testuali delle vecchie spese e tutte le tabelle precedenti. Non inventa movimenti dalle colonne fiscali della versione 2, che non aveva un flusso di riscossione. I test aprono database dallo schema originale 1 e 2, verificano la validazione Room 3 e la conservazione di dati e impostazioni.
