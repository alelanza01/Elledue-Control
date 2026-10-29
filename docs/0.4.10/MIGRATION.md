# Schema Room 9 e aggiornamento

Application ID `it.elledue.control`, Android 8+ (minSdk 26), versione 0.4.10/versionCode 10. Base reale verificata su GitHub: 0.4.9, commit `28f0e6c7591bad55ec5100f6458a2216f0036559`. Branch dedicato `feature/0.4.10-ux-accounting`. Main e branch precedenti conservati.

## Migrazione 8 → 9 solo additiva

- `expenses`: `recognition` (ACTUAL per tutti i record precedenti), `originKey` facoltativo univoco, `approvalBatchId` facoltativo, `effectiveDateConfirmed` (true per i record precedenti).
- `money_movements`: `collector`, inizialmente ELLEDUE per i movimenti creati dai vecchi flussi, che registravano riscossioni Elledue.
- `maintenance_state`: completamento della riconciliazione e conteggio degli esiti.
- `standard_approvals`: audit delle approvazioni aggregate, con riferimento al documento.

Nessuna tabella eliminata, nessun fallback distruttivo, nessun ricalcolo SQL dei vecchi importi o JSON confermati. Schemi pubblicati 1–8 conservati. I test aprono fixture delle versioni precedenti attraverso la catena di migrazioni e verificano anche direttamente lo schema 8 pubblicato.

Backup formato 1: esportazione database 9, importazione 4–9. I vecchi backup ricevono soltanto i metadati mancanti prima della validazione in Room isolato. Dopo il ripristino viene completata la riconciliazione idempotente, senza inventare esenzioni, costi pagati o riscossioni. Batch di approvazione e collegamenti sono inclusi e validati.

## Installazione senza disinstallare

1. Esportare un backup privato dalla versione attuale e conservarne anche l'APK.
2. Scaricare l'APK di test 0.4.10 dal link GitHub verificato indicato nel report QA.
3. Aprire il file sul telefono e autorizzare l'installazione dalla sorgente scelta, se Android lo richiede.
4. Installare **sopra l'app esistente**, senza disinstallare e senza cancellare dati.
5. Aprire l'app, attendere l'aggiornamento locale e controllare immobili, prenotazioni, documenti confermati e almeno una bozza. I nuovi standard compaiono senza riaprire tutte le prenotazioni storiche.
6. Verificare un'esportazione di backup e i promemoria sul telefono prima dell'uso definitivo.

La compatibilità della firma è confrontata sul binario finale con gli APK originali precedenti, non dedotta dall'application ID. Un dispositivo che abbia installato una build firmata con una chiave diversa deve essere trattato separatamente: non disinstallare per aggirare il problema. Il downgrade diretto da schema 9 a una vecchia app non è supportato; conservare il backup precedente.
