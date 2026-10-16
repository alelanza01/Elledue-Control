# Elledue Control 0.4.8 — verifica e consegna

**Stato: implementazione verificata automaticamente; consegna dell'APK non ancora completa.** Il caricamento dell'asset nella Release DRAFT è bloccato da HTTP 401 su uploads.github.com. Non viene dichiarata completata la milestone finché la consegna richiesta resta irrisolta. Restano inoltre da collaudare sul telefono i comportamenti dipendenti dall'hardware e dal produttore.

## Sorgenti e comando effettivo

Branch `feature/0.4.8-stabilization`, base 0.4.7 `ea7e654adc548e9d89160fb946a7e87df05fbe2d`. Checkpoint sorgenti `62bcaeb`, `b3772f1`, implementazione finale `757c351fbe1d2de4b4d6ec8d32f25da9cde004c4`; il commit successivo aggiunge il report e i manifesti di consegna. Nessun merge su main, che resta `c156458de9c7d90f3585a3f44a7ca23db540cb88`.

```bash
cd /workspace/Elledue-Control
source scripts/cloud-env.sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Esecuzione finale: **BUILD SUCCESSFUL**, 159 test, 0 fallimenti, 0 errori, 0 saltati. Lint: **0 errori, 15 avvisi non soppressi**. Esiti per suite in [test-results.json](qa/test-results.json), log in [validation.log](qa/validation.log), avvisi in [lint-results.txt](qa/lint-results.txt).

Sei avvisi sono preesistenti: ExifInterface piattaforma, tre helper TLS EST BouncyCastle non usati dall'app, due consigli Bitmap KTX. Nove nuovi avvisi sono suggerimenti KTX per URI e SharedPreferences. I commit sincroni delle preferenze verificano anche il risultato: non sono stati sostituiti con chiamate che perdono tale controllo. Nessuna baseline o soppressione aggiunta. L'avviso di inizializzazione delle metriche del toolchain riguarda una directory di telemetria del cloud e non ha impedito build, test o lint. Il messaggio del decoder di immagini in Robolectric non ha prodotto test saltati o fallimenti.

## Verifiche dimostrate

- Prenotazione: modifica di più di cinque campi, persistenza su SQLite chiuso/riaperto, compensi e proiezioni aggiornati; bozza ripresa dopo chiusura di tutte le Activity, nuovo avvio, salvataggio e ricreazione della scheda. Il processo Linux non è stato terminato fisicamente; la persistenza delle preferenze è controllata anche da un nuovo DraftStore.
- Navigazione: edit dal rendiconto torna al rendiconto; bozza recuperata apre la scheda aggiornata. Motivo economico guidato prima del commit e conferma dopo scrittura effettiva.
- Eliminazione: flusso Android CreateDocument con backup riletto e validato, pulsante bloccato prima della verifica, cancellazione reale e ritorno all'elenco; rollback iniettato, rimozione dipendenze/report, conservazione spese indipendenti e proprietari condivisi, rifiuto se backup scompare o dati cambiano.
- Propri/gestione: fee zero per nuove prenotazioni proprie, report esterni bloccati, risultato proprio separato da compenso/saldo gestiti; archiviati esclusi dalle query operative.
- Report: riga nominativa nascosta nei PDF reali di entrambi i formati senza modificare saldo 170€; esclusione economica coerente porta a 15€ mantenendo le spese indipendenti; gli incassi reali e lo snapshot originale non cambiano.
- Tassa: previsione con soggetti espliciti, tariffa e limite per immobile, aggiornamento delle sole proposte idonee; nessuna riscossione automatica o doppia compensazione.
- Room: schema pubblicato 6 migrato a 7 con dati, allocazioni, movimenti e JSON storico privo dei nuovi campi; documenti confermati conservati identici. Catene precedenti e backup controllati dai test di regressione.
- Notifiche: API Android eseguite con Robolectric, permesso negato/concesso, AlarmManager, perdita e riprogrammazione allarmi, NotificationManager, intent della prenotazione, deduplica, annullamenti, completate/non programmate, fuso e ora legale, silenzio. Non è una prova di consegna su telefono bloccato o in Doze.
- Accesso: nessuna credenziale disponibile, schermata protetta, credenziale Android annullata e risultato positivo, nuovo controller bloccato e timeout. Il sensore biometrico fisico non è stato usato.
- Grafica: Skia nativa API 34, 411dp; 320dp con testo 150%, accesso 320dp con testo 130%; landscape 891×411dp/testo 130%. Screenshot e PDF hanno esclusivamente dati sintetici.

## APK effettivo

File locale verificato: `/workspace/artifacts/Elledue-Control-0.4.8-debug.apk`. Questo percorso **non è un collegamento scaricabile** dalla chat o da GitHub.

- Application ID: `it.elledue.control`.
- versionName `0.4.8`, versionCode `8`, minSdk 26, targetSdk 35.
- SHA-256 file: `59b06bbbf22a6167614b8b7b63b465c5212f6c9ef86477eaaefdb13ea69e593a`.
- Dimensione: 32.796.445 byte.
- Firma APK v2 valida; certificato SHA-256 `cc45fdd9dccf37a2d94fb35e142bfde73b25de3e46e3b8ce93d4d5b9a7b2065c`, confrontato realmente con l'APK originale 0.4.7 e identico.
- Vecchio APK originale intatto, SHA-256 `1ef63908d9b6a3c324641a00ffc5473fc44b315d199926cd66e1495a31b30d19`.
- Nessun permesso INTERNET, foto fixture test-only, classe Robolectric o keystore nel nuovo APK.

La compatibilità di package/firma/versione è verificata; l'installazione di aggiornamento sul telefono non è stata eseguita in questo ambiente. Seguire [UPDATE.md](UPDATE.md), con backup e senza disinstallazione.

## Blocco concreto della distribuzione

Release GitHub **DRAFT**, nome/tag `v0.4.8-test`, ID `414920995`:

https://github.com/alelanza01/Elledue-Control/releases/tag/untagged-a4042084046d2f65d056

È una bozza visibile all'account autorizzato del repository, **senza APK allegato**. `gh release upload` ha restituito HTTP 401 Bad credentials per `https://uploads.github.com/repos/alelanza01/Elledue-Control/releases/414920995/assets`. La successiva lettura GitHub conferma `isDraft=true` e `assets=[]`. L'autenticazione integrata consente lettura, commit e creazione della bozza tramite api.github.com; non viene accettata dall'endpoint di upload. La rete raggiunge entrambi gli host: non è un blocco dovuto all'assenza del file o a un repository vuoto.

Non sono disponibili strumenti di allegato file in uscita nella chat corrente. Non è stato inventato un link sandbox e non è stato pubblicato l'APK su un branch pubblico come alternativa non autorizzata.

Per completare solo l'upload serve una credenziale GitHub valida per questo repository con permesso Contents: write, utilizzabile dal proxy su api.github.com e uploads.github.com. La richiesta sicura `GH_RELEASE_TOKEN` è predisposta nelle impostazioni dell'ambiente: inserire il valore lì, mai in chat o nel repository. Dopo l'attivazione della credenziale si riusa questo APK, si verifica lo stesso SHA-256 e lo si allega alla bozza, senza nuova build o pubblicazione. Il documento non afferma che la credenziale sia già stata configurata.

## Da verificare sul dispositivo

Aggiornamento sopra 0.4.7 con dati reali e backup preventivo; recapito degli allarmi con telefono bloccato, riavvio, batteria/Doze e personalizzazioni del produttore; sensore biometrico e recupero della credenziale Android; accessibilità e tastiera del telefono. Gli allarmi inesatti possono essere ritardati; dopo arresto forzato l'app deve essere riaperta. La protezione dell'accesso non cifra database o backup. Restano fuori dalla 0.4.8 i collegamenti PMS/OTA, sincronizzazione multiutente, invio automatico di documenti/messaggi, scheduler ricorrente completo e allegati scontrini: non sono stati dichiarati implementati.
