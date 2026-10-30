# Consegna intervento2/3 — 0.4.11

Base `df8a67ecef4e06dc3def9b5548dba66ab87fbf0d`, stesso branch `feature/0.4.11-user-feedback`. Implementazione nel commit `fe71073`, seguita dal commit conclusivo con QA e verifica dei cambi di stato manutenzione. Nessun nuovo branch, merge o pubblicazione della versione definitiva.

## Funzionalità effettive

- Calendario mensile con barre per soggiorno, nomi degli immobili sulle corsie, continuazioni tra settimane, sovrapposizioni su corsie separate, cambi ospiti e attività indicati in testo. Apertura della scheda dalla barra o della giornata dalla data, mantenendo l'immobile selezionato.
- Giornata divisa per Arrivi, Partenze, Soggiorni attivi, Pulizie, Attività e manutenzioni, con orari, ospiti e immobili. Le schede sono apribili direttamente.
- Prenotazioni raggiungibile direttamente dalla Home e dal Calendario: ricerca storica per nome/cognome, accenti/apostrofi normalizzati, stati, immobile, mese/intervallo, ordinamento check-in, archivio esplicito e nuova prenotazione. Nessuna dipendenza dal mese del calendario. Ritorno dalla scheda con ricerca e filtri conservati.
- Economia: panoramica con Spese, Rendiconti e Anticipi e pagamenti; dettagli contabili/fiscali avanzati separati. Eliminata la macrofunzione generica Movimenti. Conservati registrazioni, categorie, riscossioni, riversamenti e storni.
- Manutenzioni: denominazione coerente nelle aree operative, modulo principale breve e programmazione/storico espandibili. Collegamento a una spesa esistente senza generare ulteriori costi. Testato il passaggio Aperto → In lavorazione → Risolto, con data di risoluzione e importi invariati. L'operazione asincrona sopravvive all'aggiornamento del record e termina sul thread principale, evitando bozze obsolete dopo un salvataggio.
- Home: panoramica, portafoglio, azioni rapide, Da fare, prossime operazioni. Gli avvisi aprono le relative schede; una tassa ordinaria riconosciuta automaticamente non richiede la riscossione manuale. Le anomalie vere restano visibili.

[Decisioni UX, comportamento e compromessi](UX.md) · [File modificati](FILES.md)

## Protezione dell'intervento1

Nessuna differenza rispetto a `df8a67e` nei repository dati, Room10, migrazioni, backup, motore OTA/IVA, tassa automatica, regole contabili, snapshot e renderer PDF. [Verifica dei percorsi protetti](qa/preservation.json). Il selettore unico check-in/check-out è conservato. Non sono stati introdotti dati dimostrativi permanenti nella modalità normale.

Tutti i224 casi precedenti sono ancora nella suite finale; aggiunti6 test di ricerca/intervalli/segmenti e10 test Compose/navigazione. Nessun test disabilitato o escluso.

## Risultato della verifica

```bash
source scripts/cloud-env.sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
```

**BUILD SUCCESSFUL — 240 test PASS, 0 FAIL, 0 errori, 0 esclusi. Lint0 errori, 17 warning.**

[Log effettivo](qa/build.log) · [Nomi di tutti i test](qa/tests.json) · [Lint](qa/lint.json) · [Metadati della build](qa/build-verification.json)

Le prove coprono mese vuoto, un immobile, tre immobili, turnover, larghezza320dp/font150%, apertura schede e giornata, ricerca storica e stati, annullate, filtro immobile/mese, accesso all'archivio, creazione di una prenotazione, navigazione Economia/Spese/Pagamenti/contabilità avanzata, manutenzione con spesa collegata e avvisi fiscali pertinenti. Gli intervalli e l'ordinamento sono verificati anche nei test indipendenti dalla UI.

Le prime esecuzioni hanno individuato test di navigazione ancora legati ai pulsanti nelle posizioni precedenti, elementi non ancora composti fuori dal viewport delle liste e un tag della tassa errato nella nuova prova. Le prove ora scorrono fino ai comandi reali e mantengono tutte le verifiche funzionali. Sono stati aggiornati i testi attesi per Portafoglio, Manutenzioni e Dettagli contabili. La vecchia misura420dp della griglia è stata sostituita con un limite520dp nel caso singolo a320dp/font150%, coerente con la richiesta di privilegiare barre leggibili; la vista multiproprietà è verificata separatamente con scorrimento. Nessun limite temporale dei test è stato ampliato per nascondere errori.

## Prove visive

[14 screenshot Android nativi](qa/README.md), generati dai test con soli dati sintetici e ispezionati visivamente. Comprendono tutte le varianti mensili richieste, due stati di scorrimento a320dp con caratteri150%, giornata, ricerca storica, annullate, Home, Economia, pagamenti e manutenzione. Non sono mockup né immagini ritoccate. I tratti troppo brevi non sostituiscono un nome con puntini illeggibili: l'immobile resta identificato dalla corsia, con nome ospite/date completi nella semantica accessibile e nella scheda aperta al tocco.

## Limiti e prosecuzione

Non è disponibile un telefono fisico/emulatore avviabile: Robolectric/native non equivale alla validazione su dispositivo o con TalkBack reale. Restano17 warning lint già presenti nella base. Con molti soggiorni sovrapposti il calendario cresce e scorre, anziché nascondere occupazioni o comprimere il testo. Non si dichiara una validazione su dati reali dell'attività.

Schema10 e versione0.4.11/code11 conservati. La build debug è stata eseguita per la verifica; nessun APK o Release di questo intervento è stato pubblicato. Bug residui, notifiche native e consegna dell'APK definitivo rimangono nell'intervento3/3 richiesto dall'utente. Il branch resta pronto per quella prosecuzione e main rimane invariato.
