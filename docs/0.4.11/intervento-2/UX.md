# Intervento2/3 — decisioni UX

Base: `df8a67ecef4e06dc3def9b5548dba66ab87fbf0d`, branch `feature/0.4.11-user-feedback`. Nessuna modifica al motore economico, ai report/PDF, a Room10, alle migrazioni o ai repository finanziari dell'intervento1.

## Calendario

Le occupazioni sono segmenti continui, allineati alla colonna delle date. Le estremità cadono a metà del giorno di arrivo/partenza; il cambio nello stesso giorno separa due barre senza sovrapporle. Una prenotazione lunga continua nelle settimane successive senza essere persa; la partenza del primo giorno del mese rimane visibile.

Nella vista multiproprietà ogni corsia riporta il nome dell'immobile, oltre al colore. In un solo immobile il nome è indicato sopra la griglia senza ripeterlo per ogni settimana. Le sovrapposizioni effettive nello stesso immobile occupano corsie distinte; non vengono nascoste. Un cambio ospiti ha un comando testuale con data e immobile. Le attività programmate indicano esplicitamente i giorni, senza usare puntini o frecce come unico linguaggio.

Il mese è ripetuto nell'intestazione della griglia perché resti identificabile anche dopo aver fatto scorrere i filtri. Non si impone la vecchia altezza420dp a prescindere dai dati: il calendario cresce secondo le corsie effettive. Il test a320dp/font150% mantiene un limite520dp per il caso singolo della regressione; il caso con tre immobili viene verificato separatamente con scorrimento e screenshot. Una barra troppo breve non mostra puntini al posto di un nome illeggibile: resta cliccabile e conserva nome completo e date nella descrizione accessibile. Nomi estesi e dettagli sono leggibili nella giornata o nella scheda aperta direttamente dalla barra.

Tocco su data/cambio/attività → giornata organizzata in Arrivi, Partenze, Soggiorni attivi, Pulizie, Attività e manutenzioni, con orari, immobile e numero ospiti. Il filtro immobile del calendario è conservato nella destinazione giornaliera. Tocco su barra → prenotazione direttamente. Agenda, annullate, copia calendario, navigazione mesi e Oggi sono conservati.

## Prenotazioni

Accesso diretto fisso dalla Home e visibile dal Calendario; rimangono quattro destinazioni nella barra inferiore. La sezione ha una route autonoma e usa le medesime prenotazioni persistenti, senza duplicarle.

Ricerca su tutto lo storico, indipendente dal mese del calendario; ignora maiuscole, accenti e differenze fra apostrofo tipografico e semplice. Supporta nome/cognome in qualunque ordine. Le annullate hanno un filtro dedicato; Tutte comprende le prenotazioni non annullate. Filtri Prossime/In corso/Passate considerano gli orari effettivi se presenti; una prenotazione esplicitamente completata è passata. Ordinamento check-in crescente/decrescente con spareggio stabile.

Immobile e periodo sono facoltativi: tutto lo storico, mese oppure intervallo. Il periodo include arrivi, partenze e soggiorni che lo toccano; non cambia la competenza economica. Gli archiviati sono esclusi per default e consultabili con un'opzione esplicita. Filtri e ricerca sopravvivono all'apertura della scheda e al ritorno. Nessun terzo pulsante viene aggiunto al selettore check-in/check-out dell'editor.

## Economia

Una panoramica con accessi Spese, Rendiconti, Anticipi e pagamenti. Eliminata la macrosezione generica Movimenti e i pannelli duplicati. Spese apre l'elenco con filtri e registrazione rapida; Anticipi e pagamenti apre il pagamento del proprietario con contesto immobile, elenco e storni esistenti. Gli strumenti contabili e fiscali completi restano esplicitamente nei dettagli avanzati; la contabilità sottostante è conservata. Le manutenzioni si raggiungono nelle aree operative anziché sotto i rendiconti.

## Home e manutenzioni

Panoramica → Il tuo portafoglio → Azioni rapide → Da fare → Oggi e prossimi soggiorni/partenze. Per più di due immobili è disponibile l'accesso all'elenco completo. Nessun avviso viene collocato sopra il portafoglio. Da fare raccoglie attività incomplete, manutenzioni aperte, bozze rendiconti ed eccezioni fiscali reali.

Una tassa con anticipo automatico e conteggio valido non produce un sollecito ordinario di riscossione. Restano avvisi per conteggi da rivedere/ignoti e per residui da gestire senza automatismo, già iniziati. Gli avvisi aprono direttamente la relativa scheda.

Registro rinominato Manutenzioni, accessibile da Home, Attività, immobili e azioni rapide. Modulo principale: immobile, titolo/descrizione, stato, soggiorno facoltativo, spesa già registrata facoltativa. Programmazione, priorità, incarico, note e storico sono espandibili. Collegare una spesa non la duplica; aprire una segnalazione non genera una spesa. Il ritorno dopo il salvataggio è eseguito sul thread principale.
