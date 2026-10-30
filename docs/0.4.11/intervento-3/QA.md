# Intervento 3/3 — verifica finale 30 ottobre 2026

Base conservata: `f23cdfeda19114cafed90ccf666634f29b6b961e`, branch `feature/0.4.11-user-feedback`. Implementazione verificata: `9c310cb`. Nessun nuovo branch sorgenti, nessun merge. APK debug 0.4.11/code11, application ID `it.elledue.control`, minSDK26/target35.

## Risultati effettivi

Comando eseguito con JDK21, SDK35 e Gradle8.11.1:

```sh
source scripts/cloud-env.sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
```

**BUILD SUCCESSFUL, 263 test PASS, zero failure/error/skipped.** I 240 test dei primi due interventi restano nella suite; 23 nuovi test: FinalSafetyTest8, FinalNotificationTest7, FinalFlowsUiTest8. Nessun test disabilitato. I controlli di eliminazione preesistenti sono aggiornati alla conferma ELIMINA; backup, rilettura, scadenza, fingerprint e rollback restano verificati. Il controllo delle sovrapposizioni e il relativo dialogo sono conservati e verificati.

Lint: zero errori, 20 warning non soppressi:16 UseKtx,1 ExifInterface preesistente e3 TrustAllX509TrustManager nella dipendenza BouncyCastle1.72. Questi ultimi non sono implementazioni TLS aggiunte dall'app; il manifest finale non richiede INTERNET. Non si dichiara una verifica generale di sicurezza della dipendenza.

Evidenze: [log](qa/build.log), [tutti i casi](qa/tests.json), [lint](qa/lint.json), [metadati APK](qa/build-verification.json), [firma](qa/apk-signature.txt), [manifest/versione](qa/apk-badging.txt), [download pubblico verificato](qa/download-verification.json).

## Copertura e limiti

- Eliminazione immobile consentita: backup SAF riletto e verificato, conferma con spazi e maiuscole indifferenti, cancellazione transazionale delle dipendenze. Rollback iniettando un errore. Immobile con rendiconto confermato: cancellazione vietata, archivio disponibile, JSON storico identico.
- Spese: annullamento semplice senza motivazione obbligatoria; motivo audit predefinito, idempotenza; rendicontato protetto con apertura della rettifica. Proposta standard esclusa con override0 senza creare un consuntivo o movimento di cassa; ripristino senza doppioni.
- Bozze locali: ripresa, scarto singolo e totale con conferma, nessuna modifica al record originale; uscita Salva/Conserva/Scarta e nessun dialogo per form invariato. Abbandono bozza di rendiconto separato, anche di revisione, senza cambiare storico/allocazioni confermate.
- Notifiche: permission/channel disabilitati, attivazione immediata, prova immediata e prova programmata persistita, AlarmManager e receiver, broadcast di boot, annullamento/modifica, recupero allarmi persi, deduplicazione, Europe/Rome/ora legale e fascia di silenzio. La tassa convenzionale automatica non genera inutili solleciti di incasso reale ordinario; le eccezioni restano distinte.
- Suite di regressione: prenotazioni, selettore date, calendario, ricerca storica, Home, Economia, manutenzioni, importi effettivi/proposti, IVA OTA una sola volta, tassa automatica, PDF sintetico/dettagliato, rettifiche, backup/ripristino e migrazioni.
- Migrazione rappresentativa di versione distribuita: FeedbackMigrationTest crea il database dalle colonne dello schema9 pubblicato (0.4.10), applica9→10 e verifica snapshot confermato byte per byte. Test mantenuti anche per gli schemi precedenti. Backup precedenti e nuovi campi verificati. Non è un test sul database personale dell'utente.

**Schema Room resta10**: nessuna migrazione ulteriore nell'intervento3;9→10 additiva già nell'intervento1. Nessuna migrazione distruttiva. Diagnostica notifiche e bozze usano preferenze private, non nuove tabelle. [Dettagli migrazione](../intervento-1/MIGRATION.md).

Gli screenshot sono rendering nativi Compose/Robolectric con dati sintetici, non foto di un telefono. I sei PDF sono stati rigenerati dai test con il renderer finale, estratti e verificati; nessun documento originale privato è pubblicato.

**Prova fisica ancora necessaria:** nessun telefono o emulatore utilizzabile in questo ambiente; KVM/emulator assenti. ADB non ha consentito una sessione dispositivo (directory Android predefinita non scrivibile). Non sono stati provati installazione sul telefono, tastiera/OEM, notifica a schermo bloccato/Doze, autostart, riavvio fisico o biometria reale. La correzione delle notifiche NON è dichiarata definitivamente risolta. Il timestamp «affidata ad Android» dimostra solo che notify non ha fallito, non la visualizzazione reale. [Checklist pratica](NOTIFICHE-TELEFONO.md).
