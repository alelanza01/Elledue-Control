# Verifica e consegna 0.4.9

## Risultato automatico

Eseguito realmente il 20 ottobre 2026:

```sh
source scripts/cloud-env.sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
```

**BUILD SUCCESSFUL. 182 test eseguiti, 182 PASS, 0 fallimenti, 0 errori, 0 saltati.** Nessun test precedente rimosso o disabilitato. [Elenco classi e casi](qa/tests.json), [log finale](qa/validation.log), [mappa requisiti](REQUIREMENTS.md).

Lint: **0 errori, 17 warning**: 13 suggerimenti `UseKtx`, 3 segnalazioni `TrustAllX509TrustManager` nella dipendenza PDF preesistente e 1 `ExifInterface` preesistente. Nessuna soppressione aggiunta. L'app non richiede INTERNET; non è stata introdotta alcuna deroga alla verifica TLS per le build. [Esito lint](qa/lint.json).

Le prove comprendono calcoli contabili, migrazioni dagli schemi pubblicati, backup anche versione 7, immutabilità dei report confermati, prevenzione dei duplicati, bozze dopo chiusura delle Activity, navigazione, registro problemi, quantità effettive, occupazione tra mesi/anni e ripristino. Le notifiche sono provate attraverso l'allarme registrato e il suo receiver, compresi riarmo, deduplica, consegna differita e testo protetto; non soltanto attraverso il comando di prova.

## APK di test

- File: `Elledue-Control-0.4.9-debug.apk` — 33.053.167 byte.
- Application ID: `it.elledue.control`.
- VersionName 0.4.9, versionCode 9; minSdk 26, targetSdk 35.
- SHA-256: `bef74c1dc39d926c571a2ad9258734e7ea93db82b54ce562fb2774995c50aa7f`.
- Firma APK v2 verificata. Certificato SHA-256 `cc45fdd9dccf37a2d94fb35e142bfde73b25de3e46e3b8ce93d4d5b9a7b2065c`, identico al binario originale 0.4.8.
- Codice applicativo nel commit `fc0e4fc8f2c4f07730412c5a421efe628079976e`. I commit successivi della consegna riguardano solo documentazione/prove.
- APK originale 0.4.8 preservato con SHA-256 `59b06bbbf22a6167614b8b7b63b465c5212f6c9ef86477eaaefdb13ea69e593a`.
- Il controllo del pacchetto esclude keystore, chiavi private, risorse dei test Robolectric e fotografie delle fixture. Nessun database o backup reale incluso.

[Scarica APK da GitHub](https://raw.githubusercontent.com/alelanza01/Elledue-Control/ff13dcbb4bb8f1a27f2769dc904be6bb3d7bf349/deliverables/test-0.4.9/Elledue-Control-0.4.9-debug.apk).

Il branch `artifacts/v0.4.9-test` contiene un solo APK, senza sorgenti alternativi, dati utente o credenziali. Nessun merge su main, nessuna pubblicazione di Release definitiva. [Verifica binario](qa/apk-verification.json), [verifica download pubblico](qa/download-verification.json).

## QA visivo e PDF

[18 screenshot](qa/README.md), acquisiti dalle view Android in Robolectric con rendering nativo, usando esclusivamente dati sintetici. Coprono nuova prenotazione, picker di intervallo, calendario, giornata, problemi, riepilogo soggiorno, spese, pulizie, report, analytics, accesso e messaggi da notifica, incluse viewport 411 e 320 dp. Le regressioni precedenti conservano anche prove a caratteri ingranditi e in orizzontale.

I due PDF dimostrativi nella stessa cartella sono generati dai test, non dal rendiconto reale fornito dall'utente. Gli importi sono riconciliati con un solo motore e il test sintetico compatto resta su una pagina; i casi lunghi e i testi estesi sono verificati multipagina con PDFBox/PdfRenderer.

## Limiti effettivi

- Non è disponibile un telefono fisico né un emulatore con accelerazione hardware. Le prove sono JVM/Robolectric e non dimostrano consegna reale in Doze su uno specifico modello o comportamento del suo risparmio energetico. Un tentativo ADB non ha potuto avviare il server perché la home predefinita è in sola lettura; non è stato modificato il profilo del sistema.
- Android può ritardare allarmi non esatti; quelli esatti richiedono l'autorizzazione facoltativa dedicata. L'arresto forzato blocca i componenti finché l'app non viene riaperta; nessuna garanzia contraria viene promessa.
- L'uso del sensore biometrico e delle credenziali del dispositivo richiede la prova sul telefono; l'app non conosce né conserva il PIN Android. Il blocco è un controllo di accesso e non cifra i backup esportati.
- I costi proposti non sono spese sostenute. I consuntivi non confermati sono segnalati e restano esclusi dalle somme delle spese effettive. Le vecchie tariffe fiscali non vengono cambiate silenziosamente dalla migrazione.
- Il limite delle notti disponibili è il calendario degli immobili attivi: blocchi commerciali o chiusure non sono modellati. Le quote dei comproprietari e i trasporti condivisi tra soggiorni restano fuori da questa versione.

## Verifica sul telefono prima dell'uso definitivo

1. Esportare e custodire un backup dalla 0.4.8, quindi installare il nuovo APK sopra l'app senza disinstallarla.
2. Controllare dati storici e almeno un rendiconto confermato; verificare una prenotazione modificata e una bozza ripresa.
3. Concedere notifiche e, se desiderato, allarmi precisi. Programmare un'attività futura a breve, chiudere normalmente l'app e bloccare il telefono; verificare consegna e apertura del contenuto. Ripetere dopo riavvio del dispositivo e modifica/annullamento del promemoria.
4. Provare la protezione facoltativa con credenziale Android e biometria, anche aprendo una notifica mentre l'app è bloccata.
5. Confermare un consuntivo e verificare l'assenza di doppioni; esportare e controllare il PDF prima di condividerlo manualmente.

La consegna è una build di test verificata automaticamente. L'accettazione su un dispositivo reale resta esplicitamente da completare.
