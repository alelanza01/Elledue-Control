# Elledue Control0.4.11 — APK finale di test

[Scarica direttamente APK0.4.11](https://raw.githubusercontent.com/alelanza01/Elledue-Control/c36298c309aa360f238b4397c9de7a4e7f6d3a0b/deliverables/test-0.4.11/Elledue-Control-0.4.11-debug.apk)

- File originale: `/workspace/artifacts/Elledue-Control-0.4.11-debug.apk`.
- Versione/code: **0.4.11/11**, package **it.elledue.control**, Android8/API26 minimo, target35.
- Dimensione: **33,575,578byte**.
- SHA-256: `5976a2316bdb95bf0d8904ab977ee67548f36827d2988d550e474e86db72e7e5`.
- Firma APK v2 verificata; certificato SHA-256 `cc45fdd9dccf37a2d94fb35e142bfde73b25de3e46e3b8ce93d4d5b9a7b2065c`, identico al binario0.4.10.
- APK pubblico riscaricato via HTTPS senza autenticazione e confrontato byte/dimensione/hash con l'originale. [Verifica](intervento-3/qa/download-verification.json).
- Branch artefatti `artifacts/v0.4.11-test`, commit `c36298c309aa360f238b4397c9de7a4e7f6d3a0b`, contiene esclusivamente l'APK. Nessun backup, dato personale, chiave o credenziale.
- Branch sorgenti **feature/0.4.11-user-feedback**, implementazione finale **9c310cb**; checkpoint precedenti df8a67e(intervento1) e f23cdfe(intervento2) conservati. I commit successivi contengono soltanto evidenze e documentazione.

## Risultati della serie

1. **Motore economico:** IVA OTA configurabile applicata una sola volta; anticipo tassa convenzionale separato dagli incassi; saldo e PDF sintetico/dettagliato coerenti; migrazione9→10 additiva, snapshot confermati intatti. [Regole](intervento-1/ACCOUNTING.md).
2. **Operatività:** calendario con soggiorni per immobile e cambi ospiti, sezione Prenotazioni con ricerca e filtri, Home con Da fare, Economia organizzata e Manutenzioni semplificate. [UX](intervento-2/UX.md), [galleria](intervento-2/qa/README.md).
3. **Correzioni finali:** ELIMINA semplice con backup verificato e spiegazione dei requisiti; archiviazione alternativa per storico protetto. Spese annullabili senza motivo obbligatorio o con percorso rettifica; esclusione/ripristino delle previsioni senza creare duplicati. Modifiche locali separate dai documenti in bozza, tre scelte di uscita, scarto totale confermato e abbandono del solo documento non confermato. Diagnostica Android e prova programmata dei promemoria. [Bug e stato](intervento-3/BUGS.md).

## Verifica e limiti

Build, unit test/Robolectric e lint eseguiti realmente: **263test PASS,0fallimenti/skip, lint0errori/20warning**. I warning e tutti i casi sono elencati nella [QA](intervento-3/QA.md). Schema Room resta **10**, nessuna nuova migrazione nell'intervento3;9→10 additiva verificata partendo dallo schema9 distribuito, con storico confermato invariato e backup/ripristino testati.

[Screenshot finali e sei PDF con dati sintetici](intervento-3/qa/README.md). Esempi: [PDF sintetico](intervento-3/qa/iva-ufficiale-CONFERMATO-SUMMARY.pdf) e [dettagliato](intervento-3/qa/iva-ufficiale-CONFERMATO-DETAILED.pdf).

**Ricezione delle notifiche ancora da verificare sul telefono.** Sono verificati scheduling, receiver, boot, permessi, quiete/DST, modifica/cancellazione/dedup tramite Robolectric; nessun telefono o emulatore utilizzabile. L'accettazione di notify da Android non dimostra la visualizzazione sul dispositivo. Restano aperte anche la prova fisica di installazione e tastiera/OEM. [Checklist immediata e programmata](intervento-3/NOTIFICHE-TELEFONO.md). Non dichiariamo definitivamente risolto il bug fisico delle notifiche.

## Aggiornare conservando i dati

1. Nell'app attuale esportare un backup dalle impostazioni; conservarlo in una posizione privata e controllare il messaggio di esportazione riuscita. Non caricarlo su GitHub.
2. Scaricare l'APK dal link sopra sullo stesso telefono. Aprirlo e, se Android lo chiede, consentire l'installazione da questa sorgente.
3. Scegliere **Aggiorna**. **Non disinstallare**, non cancellare memoria/dati dell'app. Package, firma e versionCode sono compatibili con l'aggiornamento dalla0.4.10 ufficiale confrontata; l'installazione fisica non è stata eseguita qui. Se Android propone conflitto di firma/versione, fermarsi senza disinstallare.
4. Aprire l'app, verificare immobili/prenotazioni/rendiconti storici e avviare la checklist notifiche. Il database9 viene migrato in10 conservando i dati; non fare downgrade dopo la migrazione.

## Revisione senza merge

Tentativo reale `gh pr create --draft --base main --head feature/0.4.11-user-feedback`: **Post https://api.github.com/graphql: Forbidden**. Git push e download pubblico funzionano; API GraphQL bloccata. Non è stata creata una PR e non sono stati ripetuti tentativi indefiniti o richiesti privilegi illimitati.

[Apri confronto per creare la PR](https://github.com/alelanza01/Elledue-Control/compare/main...feature/0.4.11-user-feedback?expand=1). Verificare base `main` e compare `feature/0.4.11-user-feedback`, usare il [testo pronto](PR.md), quindi **Create draft pull request**. Nessun merge su main, nessuna pubblicazione della Release DRAFT.
