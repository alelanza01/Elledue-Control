# Aggiornamento 0.4.9

Application ID `it.elledue.control`, versionCode 9, versionName 0.4.9.
Base: `d898765` (0.4.8). Branch di lavoro `feature/0.4.9-consolidated`.

## Database 7 → 8

La migrazione è additiva. Schemi pubblicati 1–7 conservati; nessun fallback distruttivo.

- `properties`: scaglione lavanderia (2 ospiti/set), costo abituale trasporti facoltativo. La migrazione conserva le impostazioni fiscali preesistenti, compreso l'eventuale limite notti assente. Il default 4 vale per i nuovi immobili; verificare la disciplina effettiva prima di cambiare immobili già utilizzati.
- `stay_cost_forecasts`: proposte separate dai costi sostenuti; nessun costo né incasso creato dalla migrazione. I nuovi soggiorni gestiti propongono 2 ore alla tariffa dell'immobile e `ceil(ospiti/scaglione)` set. Gli override manuali persistono; il consuntivo ha un ID stabile ed è registrato una sola volta.
- `property_issues`, `issue_history`: problema appartenente all'immobile, prenotazione facoltativa, spesa già esistente facoltativa, storico delle modifiche. Nessun costo automatico. Eliminando una prenotazione senza documenti confermati il problema resta e il collegamento diventa NULL. La cancellazione di un immobile senza documenti confermati elenca ed elimina esplicitamente anche i relativi problemi.
- `report_booking_options`: motivazione interna e storico JSON delle inclusioni/esclusioni. Il PDF non mostra tale motivazione né lo storico interno. Le opzioni restano per singolo documento/versione.
- Categoria Trasporti disponibile senza inventare importi.

Backup portabile formato 1: esportazione schema 8; importazioni 4–8 con conversione dei campi mancanti, verifica checksum, staging Room isolato e controllo delle relazioni prima del ripristino. Le righe contabili e gli snapshot confermati non vengono ricalcolati durante il ripristino.

La cancellazione di fonti associate a rendiconti confermati è bloccata. Usare l'archiviazione per conservarne storico e relazioni. Nessuna modifica alla convenzione contabile: soggiorno interamente nel mese del check-in; costi nella competenza registrata. L'occupazione usa le notti effettive, esclude il check-out e deduplica le sovrapposizioni.

## Installazione

Esportare un backup privato con la versione attuale; conservare anche il suo APK. Installare l'aggiornamento sopra l'app esistente, senza disinstallare e senza cancellare dati. La compatibilità della firma deve essere verificata sul binario finale prima della consegna. Il downgrade diretto del database 8 a 7 non è supportato: conservare il backup precedente prima dell'aggiornamento.

Il report originale dell'attività, documenti personali, database reali, keystore e credenziali non vengono inclusi nel repository né nell'APK.
