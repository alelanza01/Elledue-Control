# Aggiornamento 0.4.7 → 0.4.8 senza disinstallazione

1. Nell'app installata apri Impostazioni → Esporta backup. Conserva il JSON in una posizione privata accessibile anche fuori dall'app. Contiene dati personali, non è cifrato; non caricarlo nel repository.
2. Confronta firma e identificativo. L'APK debug 0.4.8 ha application ID `it.elledue.control`, versionName `0.4.8`, versionCode `8`; il certificato SHA-256 è `cc45fdd9dccf37a2d94fb35e142bfde73b25de3e46e3b8ce93d4d5b9a7b2065c`, verificato uguale all'APK 0.4.7 originale (SHA-256 file `1ef63908d9b6a3c324641a00ffc5473fc44b315d199926cd66e1495a31b30d19`). Il checksum del nuovo file è nel QA e nel manifesto artefatti.
3. Apri l'APK sul telefono e scegli Aggiorna. Da PC: `adb install -r Elledue-Control-0.4.8-debug.apk`. Non disinstallare e non cancellare dati. Se Android segnala firma incompatibile, conserva entrambi gli APK e il backup e verifica la firma dell'installazione; non procedere a tentativi distruttivi.
4. Il primo avvio applica Room 6→7, con sole aggiunte. Tutti gli immobili preesistenti iniziano In gestione; i soggiorni mantengono le percentuali applicate e i documenti confermati rimangono immutati. Controlla immobili, foto, prenotazioni, spese, tasse, compensazioni e report. Un soggiorno fra due mesi resta interamente nel mese del check-in.
5. Per gli immobili propri imposta esplicitamente la tipologia. Il cambiamento non azzera le commissioni dei soggiorni storici: i casi discordanti vengono segnalati. Le nuove prenotazioni proprie hanno compenso zero; non viene inventata una ripartizione fra soci.
6. Esporta un nuovo backup database 7. Import ammesso da versioni database 4/5/6/7, previa validazione e conferma di sostituzione. La vecchia app non è compatibile con il database 7: non eseguire downgrade conservando il nuovo database. Conserva separatamente il backup precedente.
7. Attiva le notifiche nelle impostazioni dell'app e concedi il permesso Android. Controlla il canale e invia la notifica di prova. Imposta orari, anticipo e silenzio; prova sul telefono acceso, bloccato e dopo riavvio. Android può ritardare gli allarmi, soprattutto con risparmio energetico; dopo arresto forzato riapri l'app.
8. Accesso protetto è facoltativo: configura un blocco schermo sicuro Android e attivalo nell'app dopo autenticazione. Prova prima la credenziale alternativa e il timeout. Non cancellare i dati se un riconoscimento biometrico fallisce. Il blocco non cifra i backup; per recuperare la credenziale del telefono usa le procedure ufficiali del dispositivo.

La verifica della firma, le migrazioni e i test Android del progetto non equivalgono a un'installazione fisica già eseguita sul tuo telefono. Il collaudo di aggiornamento e i vincoli energetici del produttore richiedono il dispositivo reale.

```bash
sha256sum Elledue-Control-0.4.8-debug.apk
"$ANDROID_HOME/build-tools/35.0.0/apksigner" verify --verbose --print-certs Elledue-Control-0.4.8-debug.apk
"$ANDROID_HOME/build-tools/35.0.0/aapt" dump badging Elledue-Control-0.4.8-debug.apk
```
