# QA 0.4.10

Verifica finale eseguita sull'app 0.4.10/code10 con JDK21 e Android SDK35:

```bash
source scripts/cloud-env.sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
```

**PASS: 208 test, 0 errori, 0 fallimenti, 0 esclusi. BUILD SUCCESSFUL.** Lint: 0 errori, 17 warning; nessuna nuova soppressione per nascondere errori. [Conteggi e suite](qa/tests.json), [lint](qa/lint.json), [log build](qa/build.log).

I test comprendono logica economica, Room reale sotto Robolectric, migrazioni dai precedenti schemi e fixture pubblicata 8→9, backup/ripristino, idempotenza e rollback atomico, PDF e UI Compose. La suite precedente è conservata. Sono stati corretti anche il ritorno al calendario dopo l'eliminazione di un'attività (il completamento ora vive nello scope della destinazione) e l'attesa dei dati nella prova di copia calendario. Nessun test disabilitato.

Le prove UI verificano selettore unico senza terzo pulsante, mese compatto a 320dp e font 150%, form spesa, quantità/rate, riscossione parziale, approvazione in blocco e azioni principali in ogni contesto. Gli [screenshot](qa/README.md) sono rendering Android nativi Robolectric, non mockup. Dati e PDF sono sintetici, separati dalla modalità normale dell'app.

## Limiti effettivi

Non è disponibile un telefono/emulatore avviabile in questo ambiente. Non sono quindi dichiarate prove fisiche di aggiornamento, avvio, notifiche a schermo bloccato, risparmio energetico OEM o biometria. Compatibilità dell'aggiornamento verificata staticamente: stesso package, versione crescente, stessa firma del binario originale 0.4.8 e 0.4.9. Prima dell'installazione effettuare un backup esportato e custodirlo; aggiornare senza disinstallare. Il downgrade dopo migrazione allo schema9 non è supportato.

Le previsioni non attestano interventi né pagamenti. Le spese storiche ambigue senza collegamento non vengono associate automaticamente: richiedono verifica dell'operatore. I rendiconti confermati rimangono snapshot immutabili.

Le API GitHub non sono raggiungibili dal proxy del runtime (tunnel CONNECT: 403 Forbidden), anche fuori sandbox. Un file PR preparato non costituisce una PR creata; la verifica della consegna binaria usa il download pubblico effettivo da GitHub, indipendente dalle API.
