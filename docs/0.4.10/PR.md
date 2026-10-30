# Titolo

0.4.10: automazioni economiche retroattive, approvazione costi in blocco e UX operativa

# Descrizione per la revisione

Le prenotazioni già presenti potevano rimanere senza standard e le previsioni di pulizie/lavanderia non entravano nel rendiconto. Questa versione riconcilia le fonti eleggibili e include i costi proposti nelle bozze/PDF, senza registrare pagamenti. La conferma del report permette all'operatore di approvare esplicitamente il gruppo in un'unica transazione, con origine univoca, audit e revisioni. Consuntivi, override e rendiconti storici confermati restano preservati.

Per un soggiorno 29 novembre–3 dicembre, i nuovi standard seguono la competenza di novembre e mantengono separata la data prevista del 3 dicembre. Una spesa manuale già attribuita a dicembre non viene spostata: novembre ne mostra il collegamento informativo, dicembre la contabilizza una sola volta.

Il form spese nasconde i dettagli meno frequenti; entrambi i campi data aprono lo stesso selettore; il calendario torna a una griglia compatta. Dal soggiorno si può confermare una riscossione parziale o totale e, soltanto per Elledue/accordo pertinente, creare contestualmente l'anticipo da riconoscere. Home e `+` conservano il design premium e aprono direttamente le operazioni.

## Integrità e migrazione

- Package `it.elledue.control`, minSdk 26, versione 0.4.10/code10.
- Schema Room 9, migrazione 8→9 solo additiva; schemi 1–8 invariati.
- Backup formato 1, import 4–9 con staging e validazione.
- Standard approvato e pagamento/consuntivo distinti; una rettifica non attesta da sola l'esecuzione del servizio.
- Nessun login, backend, tracking o nuovo permesso Internet.
- Nessun merge automatico su main; la base dello sviluppo è la 0.4.9 già verificata.

## Verifica

Comando: `source scripts/cloud-env.sh` e `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.

Il report QA allegato al branch contiene l'esito finale, i nomi dei test, lint, screenshot Android nativi con dati sintetici, PDF dimostrativi e verifica dell'APK/firma/download. Le prove coprono anche rollback di un'approvazione interrotta, doppia riconciliazione, backup precedente, mesi diversi e riscossioni ripetute senza duplicazioni. Nessun test su telefono fisico viene dichiarato.

## Limiti

Le restrizioni energetiche dei produttori, l'installazione sul dispositivo reale e il sensore biometrico richiedono ancora la verifica sul telefono. Le spese storiche prive di prenotazione non vengono associate per somiglianza: il report segnala i casi ambigui.

Le API GitHub risultano bloccate dal proxy del runtime (`CONNECT api.github.com` → `403 Forbidden`), anche dopo il tentativo fuori sandbox. Il push Git e la consegna APK attraverso un branch artefatti sono invece verificabili. Questo file prepara la revisione, ma non equivale a una PR GitHub creata.
