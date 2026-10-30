# Schema10 e dati precedenti

Migrazione Room9→10 esclusivamente additiva. Schemi pubblicati1–9 conservati invariati; nuova configurazione registrata in10.json.

- `properties`: trattamento OTA (default storicoNONE), aliquota IVA2200 basis points, anticipo convenzionale automatico abilitato.
- `platform_commissions`: trattamento opzionale ereditato dall'immobile (`INHERIT`).
- `reservations`: trattamento e aliquota OTA applicati, anticipo automatico, eventuale importo anticipo rettificato, indicatori di scelta manuale. Valori iniziali storici: NONE, aliquota2200, anticipo non ancora applicato, nessuna rettifica.
- Nessuna tabella economica svuotata o ricreata, nessun movimento di cassa generato dalla migrazione.

`FeedbackReconciliation` applica successivamente le regole ai soli soggiorni eleggibili, all'avvio dopo la riconciliazione0410 e al ripristino di un vecchio backup. Marcatore separato `economics-0411`; ripetere l'operazione non cambia nuovamente i dati già coerenti e non crea righe di spesa o incasso.

Le scelte dell'operatore e le prenotazioni consolidate vengono preservate. Una modifica della configurazione immobile riconcilia soltanto quel suo insieme modificabile. Tutti gli snapshot confermati conservano esattamente la stringa JSON precedente: i nuovi flag mancanti nei vecchi snapshot hanno semantica storica, senza aggiungere retroattivamente IVA o anticipo alla loro lettura.

Backup formato1: esporta schema10, importa schemi4–10 con arricchimento delle sole colonne mancanti e validazione isolata prima del ripristino. Parametri nuovi, revisioni, movimenti e impostazioni restano nel backup. Le bozze di modulo precedenti ricevono i soli nuovi campi mancanti alla lettura senza perdere ciò che era stato digitato.

Prima di un aggiornamento reale esportare e custodire un backup. Package `it.elledue.control` e firma debug preesistente sono preservati; non disinstallare per aggiornare. Nessun downgrade automatico dopo la migrazione10. Questo intervento1/3 conserva il branch per i prompt successivi e non pubblica una nuova release.
