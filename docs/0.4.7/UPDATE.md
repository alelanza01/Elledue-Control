# Aggiornamento sopra 0.4.6, senza disinstallare

1. Nella 0.4.6 apri Impostazioni → Esporta backup. Conserva il file JSON in una posizione privata accessibile anche fuori dall’app. Contiene dati personali ed economici: non caricarlo su GitHub. Il checksum controlla integrità, non cifra i dati.
2. Verifica che l’app precedente abbia application ID it.elledue.control e il medesimo certificato della nuova build. La build debug0.4.6 consegnata c156458 usa SHA256 certificato cc45fdd9dccf37a2d94fb35e142bfde73b25de3e46e3b8ce93d4d5b9a7b2065c. Android richiede la stessa firma: una build release firmata diversamente non è un aggiornamento di questa debug.
3. Installa l’APK0.4.7 (versionCode7, superiore a6) come aggiornamento. Da PC: `adb install -r Elledue-Control-0.4.7-debug.apk`. Sul telefono apri l’APK e scegli Aggiorna. Non disinstallare, non cancellare dati. Se Android segnala firma incompatibile, fermati e verifica l’APK installato; non usare disinstallazione come tentativo di correzione.
4. Il primo avvio migra Room5→6 con sole aggiunte. Controlla immobili/foto, prenotazioni, spese, riscossioni/compensazioni e rendiconti confermati. Controlla anche un soggiorno a cavallo mese: resta interamente nel mese del check-in.
5. Esporta un nuovo backup database6. La 0.4.7 importa backup database4/5/6, previo controllo e conferma della sostituzione. La 0.4.6 non importa il formato database6: non tentare un downgrade mantenendo il nuovo database. Conserva il backup precedente separato.

Verifiche riproducibili PC:

```bash
$ANDROID_HOME/build-tools/35.0.0/apksigner verify --verbose --print-certs Elledue-Control-0.4.7-debug.apk
$ANDROID_HOME/build-tools/35.0.0/aapt dump badging Elledue-Control-0.4.7-debug.apk
sha256sum Elledue-Control-0.4.7-debug.apk
```

La firma viene confrontata con l’APK effettivamente consegnato0.4.6; la migrazione e la conservazione di snapshot/allocazioni/movimenti vengono testate su SQLite popolato dallo schema pubblicato5. L’installazione sopra una versione presente sul telefono fisico richiede collaudo sul dispositivo e non è simulata da questi test. Nessuna chiave privata è inclusa nei sorgenti. Per una firma release personale segui docs/RELEASE_SIGNING_WINDOWS.md.

La build0.4.7 rimane locale: nessun APK è pubblicato sul branch o in una release senza ulteriore autorizzazione, secondo il brief.
