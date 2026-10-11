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
| reservations | Base per milestone 2; immobile obbligatorio, date epoch-day, importi in centesimi e percentuali applicate fotografate sulla prenotazione. |
| expenses | Base per milestone 3; immobile obbligatorio, prenotazione opzionale, importo effettivo e flag anticipo/rimborso separati. |
| activities | Base per milestone 6; immobile opzionale, istante di scadenza e fuso Europe/Rome. |
| reports | Base per milestone 4; immobile obbligatorio, periodo/versione univoci e snapshot. |

Le ultime quattro tabelle sono fondamenta di schema, non moduli già utilizzabili. Le rispettive milestone aggiungeranno campi e migrazioni per gli ulteriori requisiti (ospiti tassabili, orari effettivi, riscossioni, pagamenti, quantità, allegati, attribuzioni ai rendiconti, ricorrenze). Le foreign key RESTRICT impediscono orfani; non è esposto alcun comando di cancellazione immobile. Archiviare non modifica prenotazioni o spese.

Room parte da schema versione 1, esportato e committato. Non usare `fallbackToDestructiveMigration`. Ogni cambio successivo deve incrementare la versione, avere migrazione esplicita, schema esportato e test da ogni versione già distribuita. Non esiste una migrazione precedente per questa prima installazione.

UUID e timestamp preparano la futura sincronizzazione; la versione attuale è esclusivamente locale per singolo operatore. Non è implementata alcuna sincronizzazione.

## Importi, commissioni e contabilità futura

EUR in centesimi `Long`; percentuali in basis point `Int` (20% = 2000). Parsing con BigDecimal, al massimo due decimali; percentuali da 0 a 100%; arrotondamento commissioni HALF_UP al centesimo. Nessun Float/Double nei calcoli contabili. Le tariffe dell’immobile sono solo valori predefiniti: le prenotazioni dovranno conservare uno snapshot dei parametri applicati.

Priorità OTA: importo effettivo manuale > percentuale della prenotazione > tariffa portale/immobile > tariffa immobile. Lordo − OTA = base commissionabile; compenso Elledue = base × aliquota gestione. Esempio verificato dal test: 1000 € − 195 € = 805 €; 20% = 161 €.

Il proprietario riceve i pagamenti OTA. Elledue matura il compenso e riceve un bonifico successivo: lordo, netto proprietario, compenso, anticipi e pagamenti devono restare distinti.

Tassa: 3 €/persona/notte soggetta, età minima 16 iniziale, limiti/esenzioni configurabili per immobile. Nessuna età individuale richiesta; in milestone 3 conteggi soggetti/esenti e rettifica manuale. La tassa rimane da incassare fino alla conferma manuale. Riscossione, compensazione e riversamento sono stati separati; la tassa non è ricavo né spesa. Le allocazioni di compensazione dovranno essere univoche per impedire duplicazioni.

Spese: immobile obbligatorio, quantità e tariffa storiche, costo effettivo confermato, anticipo Elledue e rimborsabilità distinti. Pulizie iniziali 8 €/ora; lavanderia 5 €/set matrimoniale. Una sola registrazione contabile per intervento; la vista pulizie non crea una seconda spesa.

Saldo rendiconto = commissioni Elledue + spese anticipate rimborsabili − tassa riscossa e riconosciuta in compensazione − anticipi/pagamenti già riconosciuti. Esempio obiettivo milestone 4: 161 + 24 + 10 + 5 − 30 = 170 €. Un saldo negativo è un credito da riconciliare, mai un ordine automatico di bonifico.

## Competenza rendiconti proposta

Attribuire l’intera prenotazione al mese del check-out in Europe/Rome. Un soggiorno a cavallo di due mesi entra solo nel mese di partenza: nessuna ripartizione che raddoppi le voci. Spese nel mese della loro data; compensazioni secondo le allocazioni riconosciute. Ogni prenotazione ha una sola attribuzione corrente a un rendiconto; le versioni rettificate fanno riferimento alla stessa attribuzione, senza sommare gli snapshot precedenti. Evidenziare prenotazioni non attribuite e bozza per dati ancora incompleti. La regola va esposta nell’anteprima, prima della conferma.

Rendiconti: bozza modificabile con ricalcolo, conferma con snapshot immutabile, rettifica come nuova versione. PDF multipagina con blu/dorato, logo sostituibile, tabelle leggibili e nessun invio automatico.

## Date, notifiche, sicurezza

Date soggiorno `LocalDate`/epoch-day; orari `LocalTime`; scadenze istanti con fuso Europe/Rome, conversioni esplicite e test ora legale nelle milestone successive. Minimo operativo due notti configurabile, non vincolo irreversibile dello schema.

Notifiche future: WorkManager per lavoro differibile e AlarmManager solo quando giustificato; permessi runtime, riavvio, aggiornamento/annullamento, identificativi unici e attività completate. Nessuna garanzia assoluta su force-stop o risparmio energetico. Comparsa di un promemoria non prova un adempimento. Nessun collegamento automatico ad Airbnb/Booking, WhatsApp, email o portali pubblici.

Nessun permesso Internet nell’app. Backup automatico Android disabilitato per evitare copie non controllate dei dati personali. Export/import manuale e verificato tramite Storage Access Framework nella milestone di qualità, prima dell’uso con dati irrecuperabili. Documenti ospiti/allegati devono stare in storage privato e avere politiche di accesso e inclusione backup esplicite. Credenziali e keystore sono esclusi da Git.
