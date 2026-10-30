# Correzioni e stato

| Problema | Modifica | Verifica |
|---|---|---|
| ELIMINA non abilita il pulsante | Rimossa la richiesta nascosta del nome; trim/case-insensitive; condizioni mancanti visibili; tastiera chiusa prima del backup/conferma | Repository, UI e flusso SAF Robolectric; ultimo controllo tastiera sul telefono aperto |
| Storico protetto sembra impedimento inspiegabile | Archivia immobile evidente; nessun tentativo distruttivo sui rendiconti confermati | Snapshot invariato nei test |
| Spese non eliminabili | Annulla spesa senza motivo obbligatorio se non consolidata; azione rettifica esplicita se protetta; già annullata non ripetibile | Repository e UI, nessun doppio movimento |
| Proposta non consuntivata | Non applicare questa previsione / Ripristina previsione; override0 significa esclusione dal saldo proposto | Nessuna spesa o cassa creata, niente doppioni |
| Bozze ambigue | Modifiche locali separate da rendiconti DB e record; uscita a3 scelte, scarto totale confermato, abbandono documento non confermato | UI, persistenza e immutabilità storico |
| Notifiche silenziose | Switch applicato subito; contatore dei soli job accettati da AlarmManager, errori visibili, prova2minuti, receiver/boot e riprogrammazione forzata al ritorno | API simulate/receiver verificati; ricezione fisica ancora aperta |
| Riconciliazione economica interrompe osservazione promemoria | Errore visibile senza interrompere l'avvio del motore notifiche; CancellationException propagata | Build e suite; nessun cambio alle transazioni economiche |

Non attribuiamo al telefono una causa unica senza osservazioni del dispositivo. Permesso, canale, risparmio energetico e fascia di silenzio sono ora ispezionabili. Il bug resta da validare con la checklist fisica.
