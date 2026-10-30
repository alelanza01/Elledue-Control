# Consegna intervento1/3 — 0.4.11

## Repository

Base verificata0.4.10 `186a80e2d49b7a960eb07e2f3e9760c9902dcc5e`. Branch `feature/0.4.11-user-feedback`; implementazione nel commit `b7fa89d` e correzioni/QA nel successivo commit di consegna. Branch conservato per gli interventi2/3 e3/3. Nessun merge su main, nessuna Release pubblicata.

## Funzioni e file

- `BookingFinance`, `BookingDraft`, entità e repository: modalità OTA base+IVA, già inclusa, nessuna maggiorazione e diretta; snapshot dei parametri, effettivo prioritario, ripristino della regola percentuale quando si cancella l'effettivo.
- `TaxAdvance`, `FeedbackReconciliation`, riepiloghi: anticipo convenzionale automatico, riconciliazione con compensazioni parziali o complete, esclusione degli incassi esterni, eccezioni immediate e audit. Nessuna registrazione di cassa automatica.
- `ReportSnapshot`, `ReportPdf`, `ReportScreens`: stesso saldo nei due layout, ore e set aggregati per soggiorno, PDF ufficiale senza bozza e senza etichette tecniche; esportazioni non definitive riconoscibili.
- `DatabaseMigrations`, `BackupService`, `DraftStore` e savers: migrazione additiva, backup4–10 e recupero dei moduli precedenti senza perdita delle modifiche.
- Moduli prenotazione/immobile: parametri ereditati e correzioni facoltative. Corretto inoltre il ritorno dall'editor attività sul thread principale dopo un'operazione asincrona.

[Elenco completo dei file rispetto alla0.4.10](FILES.md). [Regole definitive e retroattività](ACCOUNTING.md). [Schema e migrazione](MIGRATION.md).

## Esempi verificati

| Caso | Risultato |
|---|---|
| Lordo1000€, OTA19,5% + IVA22% sulla commissione | Base195€, IVA42,90€, costo237,90€, netto762,10€ |
| Gestione20% sul netto762,10€ |152,42€; con25%190,53€ |
| Effettivo237,90€ già IVA inclusa |Costo237,90€, nessuna seconda IVA |
| Compenso150€ + spese26€ − anticipo36€ |Saldo140€ |
| Anticipo36€ con compensazione12€ già registrata |Quota automatica24€, totale riconosciuto36€ |
| Tre soggiorni con2+1+1 ospiti |3set/15€, pulizie6ore/48€ |
| Compenso161€ + spese26€ − anticipo36€ − pagamento200€ |Credito49€, esposto senza ambiguo segno negativo |

## Verifica finale

```bash
source scripts/cloud-env.sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
```

**BUILD SUCCESSFUL. 224 test PASS, 0 FAIL/errori/esclusi.** Tutta la suite precedente conservata; aggiunti15 casi economici/PDF/backup e una prova con schema9 pubblicato e snapshot storico. [Casi realmente eseguiti](qa/tests.json), [log finale](qa/build.log), [lint](qa/lint.json):0errori, 17warning.

Le verifiche leggono il testo effettivo dei PDF, confrontano il riepilogo tra i layout, controllano somme e contenuti vietati. La migrazione e la riconciliazione mantengono invariata la stringa JSON dei documenti confermati. Provati anche backup10, import9, backfill ripetuto e bozze precedenti.

Le prime esecuzioni hanno individuato un'attesa PDF non più pertinente (da2pagine a1 dopo la semplificazione), una fixture di backup9 contenente per errore il marcatore0411, la conservazione dei soggetti storici quando cambia soltanto il totale ospiti e un errore nel valore atteso del nuovo caso a credito. Corrette prima dell'esito finale, senza disabilitare test. L'ultimo giro ha rilevato inoltre il ritorno asincrono dall'editor attività fuori dal thread principale: ora è esplicitamente eseguito su Dispatchers.Main e il test rimane nella suite.

## PDF di prova

[6 PDF e relativi testi estratti](qa/README.md): coppie sintetico/dettagliato per quadratura140€, IVA e documento ufficiale confermato, tre soggiorni distinti. Dati esclusivamente sintetici; nessun documento originale o informazione personale pubblicati. Il PDF sintetico confermato è stato anche renderizzato e ispezionato visivamente.

## Build e limiti

APK debug compilato0.4.11/code11, package `it.elledue.control`, stessa firma dell'APK originale0.4.10. [Metadati effettivi](qa/build-verification.json). Il binario di questo checkpoint intermedio è conservato localmente in `/workspace/artifacts/Elledue-Control-0.4.11-intervento-1-debug.apk`; nessuna distribuzione pubblica o nuova Release è stata effettuata in questo primo intervento.

Non sono disponibili telefono fisico o emulatore avviabile: le prove Android sono Robolectric/native. Installazione reale, notifiche nelle condizioni OEM e biometria non sono attestate come verificate sul dispositivo. I 17warning lint residui sono elencati nel rapporto. Le esenzioni storiche ignote e le spese ambigue non vengono ricostruite per supposizione; richiedono una correzione esplicita. Le vecchie OTA effettive non sono riclassificate arbitrariamente; per gli immobili esistenti l'IVA va configurata secondo il caso reale. Nessun altro requisito economico di questo intervento è lasciato intenzionalmente da implementare. I successivi due interventi non sono dichiarati completati.
