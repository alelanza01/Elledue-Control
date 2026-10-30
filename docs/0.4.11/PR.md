## Problema e risultato

Le commissioni OTA/IVA e l'anticipo convenzionale della tassa richiedevano una gestione coerente nei rendiconti; calendario e sezioni operative erano difficili da consultare. Sul telefono, la conferma ELIMINA restava bloccata e le notifiche non arrivavano senza una diagnostica utile.

Questa serie0.4.11 configura IVA OTA e anticipo tassa senza confonderli con cassa effettiva, conserva gli snapshot confermati e semplifica i PDF. Riorganizza calendario per immobile, Prenotazioni con ricerca storica, Home, Economia e Manutenzioni. La cancellazione consentita richiede ora soltanto ELIMINA e un backup verificato, con condizioni mancanti visibili; lo storico protetto propone archiviazione. Le spese distinguono annullamento, previsione esclusa e rettifica. Modifiche locali, rendiconti in bozza e record salvati hanno percorsi distinti.

Le notifiche hanno attivazione immediata, diagnostica di permessi/canale/allarmi/batteria, conteggio dei job accettati, prova immediata e programmata, ricezione e riprogrammazione dopo boot o ritorno nell'app. Non dichiariamo risolta definitivamente la ricezione su telefono: serve la checklist fisica.

## Validazione

- Build debug0.4.11/code11, unit test e lint eseguiti:263PASS,0fallimenti/skipped; lint0errori/20warning documentati.
- Room10,9→10 additiva; test da schema9 pubblicato e backup precedenti, storico confermato preservato byte per byte. Nessun nuovo schema nell'intervento3.
- Package it.elledue.control e certificato debug confrontati con l'APK0.4.10; SHA e download pubblico senza autenticazione verificati.
- Screenshot nativi e6PDF sintetici rigenerati; nessun dato personale, backup o credenziale incluso.
- Nessun dispositivo utilizzabile: installazione fisica, tastiera/OEM, Doze, lockscreen e notifiche reali ancora da verificare.

[Consegna e APK](docs/0.4.11/DELIVERY.md), [QA finale](docs/0.4.11/intervento-3/QA.md), [checklist telefono](docs/0.4.11/intervento-3/NOTIFICHE-TELEFONO.md).

Revisione richiesta senza merge automatico su main. APK di test, nessuna pubblicazione della Release DRAFT.
