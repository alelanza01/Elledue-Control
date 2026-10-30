# Difetti verificati e correzioni

## Notifica programmata che scompare

Il percorso AlarmManager consegnava la notifica, poi `reconcile()` vedeva il job già consegnato e quindi assente dai prossimi allarmi. Lo stesso ramo cancellava sia il vecchio allarme sia la notifica visibile, che poteva scomparire immediatamente.

La riconciliazione ora rimuove l'allarme terminato, ma cancella la notifica soltanto se il job non è più pertinente (annullamento, modifica o attività completata). I promemoria differiti ancora pertinenti e già programmati possono essere recuperati; la versione consegnata impedisce duplicazioni. La prova automatica invoca il PendingIntent realmente registrato e attraversa il receiver, senza chiamare il pulsante di prova. Dopo la consegna viene riconciliato nuovamente e si verifica che la notifica rimanga visibile.

Allarmi esatti utilizzati soltanto con autorizzazione Android; fallback `setAndAllowWhileIdle` altrimenti. Nessuna garanzia sui blocchi del produttore, arresto forzato, permessi revocati o telefono spento.

## Regole operative aggiornate

- Il primo immobile non viene più selezionato automaticamente nelle nuove prenotazioni.
- Default OTA iniziali espliciti, senza cancellare override manuali o valori storici.
- Durata fiscale e calcoli delle notti reali distinti dalla competenza economica del check-in.
- Copia calendario non limitata al mese visualizzato e comprensiva delle partenze di oggi.
- Pulizie/lavanderia proposte in tabella separata dai costi effettivi, conferma univoca e rettifiche della stessa spesa.
- PDF sintetico con OTA nella tabella prenotazioni e categorie di rimborso distinte. Nel caso sintetico compatto di regressione resta verificata l'impaginazione a una pagina; i documenti lunghi continuano su più pagine.
- L'eliminazione definitiva di dati con rendiconti confermati è bloccata; l'archiviazione conserva tutti i documenti.

## Aggiornamento dei test precedenti

Nessun test eliminato o disabilitato. Sono state adeguate le aspettative direttamente cambiate dal brief: selezione esplicita immobile, date in un solo picker, tap data verso Giornata operativa, numero di categorie dopo Trasporti, limite fiscale iniziale 4, notti del mese effettivo, riconoscimento esplicito degli avvisi sui consuntivi e blocco dell'eliminazione dei rendiconti confermati. Le fixture storiche dei test di migrazione continuano a usare gli schemi pubblicati originali.
