# Elledue Control 0.4.5 — evoluzione del prodotto

Base preservata: commit b1ecb100e1359d0af69e55517196197b20a62b95. Application ID
`it.elledue.control`, nessuna chiave release generata nel cloud.

## Collocazione delle funzioni

[La mappa completa](product/NAVIGATION_4_5.md) precede la modifica della navigazione.
Le sei destinazioni precedenti diventano quattro: Home, Immobili, Calendario,
Economia. Il + centrale presenta al massimo tre azioni pertinenti. L'intestazione
contiene il marchio originale, il centro notifiche esplicitamente non attivo,
il profilo locale e le impostazioni. Economia distingue analisi, contabilità
operativa e documenti per i proprietari. Backup nelle impostazioni.

## Design e operatività

Superfici profonde, blu notte e oro ufficiali, titoli serif, gerarchia dei numeri,
righe compatte per l'agenda e immagini locali nei portfolio. Tema chiaro con
superfici grigio azzurro e intestazione navy; tema scuro senza card bianche.
Il concept è una direzione artistica: nessuno sfondo, logo, nome, foto o metrica
inventati della reference è inserito nell'archivio normale.

Home limitata ad agenda odierna, ospiti presenti, attenzione contabile, attività,
bozze, tre soggiorni futuri e due immobili. Profilo operatore con nome/cognome e
foto facoltativa, nessun account remoto. Attività locali persistenti, collegabili
alla prenotazione e completabili; non costituiscono uno scadenzario ricorrente o
una sincronizzazione staff. Nessuna notifica Android della Milestone 5.

La prenotazione apre tassa, spesa, pulizia, lavanderia e attività con immobile e
prenotazione preselezionati. I salvataggi usano i repository esistenti e una sola
registrazione per intervento. Il menu immobile rende accessibili modifica,
archiviazione, riattivazione ed eliminazione con conferma; quest'ultima viene
bloccata transazionalmente se esiste storico. Nessuna cancellazione economica
in cascata.

## Immagini e archivio

Le immagini selezionate con Android SAF vengono copiate in JPEG locale in Room:
EXIF applicato, massimo 1200 px e 180 KiB, qualità progressivamente compressa.
Sostituzione/rimozione non dipendono da un URI esterno temporaneo. La visualizzazione
copertina usa Crop; l'anteprima permette l'immagine intera senza ritagliare i byte.
La copia compressa è l'immagine durevole dell'app, non un archivio degli originali
fotografici ad alta risoluzione. Nessuna autorizzazione generale alla galleria.
Le foto non sono conservate nel bundle Android di stato del form.

Room 5: migrazione additiva 4→5 per copertina, profilo e collegamento delle attività
alle prenotazioni. Schemi pubblicati 1–4 intatti; nessun fallback distruttivo.
Backup formato 1/database 5, tutte le 16 tabelle incluse con immagini e snapshot.
I backup database 4 vengono aggiornati in staging con campi nuovi vuoti, senza
alterare gli snapshot storici. Checksum, formato, immagini, relazioni e contabilità
validati prima della sostituzione atomica. Limite totale 32 MiB verificato: oltre
il limite l'esportazione fallisce esplicitamente, senza archivio parziale. Molte
foto e molte versioni possono raggiungerlo; nessuna promessa di archivio illimitato.
Il backup rimane non cifrato: custodirlo in una destinazione privata.

## Analisi e rendiconti

Analisi per immobile/mese con lordo, compensi, numero prenotazioni, notti e durata
media dei soggiorni di competenza, differenza in euro sul mese precedente e grafico
reale degli ultimi sei mesi. Occupazione calendario = giorni distinti occupati nel
mese / giorni del mese, includendo soggiorni iniziati prima. Non è disponibilità
commerciale: blocchi/indisponibilità non sono ancora registrati. Sovrapposizioni
non gonfiano i giorni occupati, annullamenti esclusi. I calcoli monetari restano
centesimi interi; i float sono usati soltanto per dimensionare i grafici.

PDF sintetico e dettagliato derivano dal medesimo snapshot immutabile; entrambi
rigenerabili per versioni storiche. Nel documento esterno la tassa mostra riscosso
e altri anticipi; una nota chiarisce l'eventuale differenza dalla compensazione
riconosciuta. Solo questa compensazione riduce il saldo. Nessuna contabilità
parallela, nessuna modifica alla competenza check-in o al calcolo delle commissioni.

Miniatura e visualizzatore completo leggono il PDF effettivamente generato con
PDFBox, con tutte le pagine, scorrimento e ingrandimento/adattamento. Lo stesso
motore e layout generano l'esportazione SAF; BOZZA resta visibile prima della
conferma. Non serve un visualizzatore esterno per l'anteprima. Le immagini RGB
vengono convertite esplicitamente per mantenere identici i colori del marchio
anche nel rendering Android/host. Nessun invio automatico.

## Verifiche e consegna

Risultati definitivi e checksum: `deliverables/milestone-4.5/manifest.json`.
Build e test: `source scripts/cloud-env.sh`, quindi
`./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:minifyReleaseWithR8`.
Gli screenshot sono di componenti Android nativi con soli dati sintetici; i PDF
pubblici non derivano dal vecchio rendiconto personale.

Restano aperti collaudo su telefono e aggiornamento fisico. La firma release
definitiva va creata e custodita sul PC dell'utente come descritto in
[RELEASE_SIGNING_WINDOWS.md](RELEASE_SIGNING_WINDOWS.md). Staff/sync, manutenzioni
con assegnazione, archivio documentale e catalogo partner sono architettura futura,
non schermate fittizie. Non iniziare la Milestone 5 senza richiesta esplicita.
