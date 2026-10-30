# CODEX — L2 Control — Intervento 3/3
## Correzioni critiche, eliminazioni, notifiche e consegna verificabile

Continua il lavoro nel repository `alelanza01/Elledue-Control`, utilizzando lo stesso branch dei due interventi precedenti.

Non sovrascrivere le modifiche economiche e UX già implementate.

Questo è l'intervento conclusivo della serie: al termine devi consegnare un APK di test aggiornato, verificabile e installabile.

## A. Correggere definitivamente l'eliminazione degli immobili

L'utente segnala che, nella schermata di eliminazione immobile, viene richiesto di digitare "ELIMINA", ma il pulsante per confermare rimane disabilitato.

Analizza in particolare:

- `DeletionScreen.kt`;
- `DeletionRepository.kt`;
- flusso backup preventivo;
- dipendenze fra immobili, prenotazioni, spese e rendiconti;
- stato del pulsante di conferma;
- comportamento della tastiera Android;
- eventuali condizioni non comunicate all'utente.

Nella versione 0.4.10 il controllo richiede la stringa esatta `ELIMINA ${p.label}` e un backup verificato.

Questa UX è troppo ambigua.

### Soluzione

Rendi la procedura comprensibile.

Per immobili eliminabili senza dati storici protetti:

- mostra chiaramente ciò che verrà eliminato;
- utilizza una conferma semplice, preferibilmente digitando `ELIMINA`;
- accetta la parola indipendentemente da spazi iniziali/finali e differenze maiuscole/minuscole;
- non richiedere il nome completo dell'immobile nella stringa;
- se è obbligatorio un backup preventivo, mostra con chiarezza che quello è il passaggio che manca;
- spiega visivamente perché il pulsante non è ancora attivo.

Non eliminare in maniera distruttiva un immobile con rendiconti confermati o collegamenti storici protetti.

In quei casi proponi in modo evidente **Archivia immobile**, spiegando che l'archiviazione lo toglie dagli immobili operativi senza cancellare i documenti.

Verifica che l'utente possa completare davvero l'eliminazione nei casi legittimamente consentiti.

Non aggirare le protezioni dei dati per ottenere un pulsante apparentemente funzionante.

## B. Eliminazione e annullamento delle spese

L'utente segnala di non riuscire a eliminare alcune spese.

Analizza il flusso attuale.

Distingui:

- spesa non ancora rendicontata, che può essere annullata/eliminata in sicurezza;
- spesa già inclusa in un rendiconto confermato, per cui deve essere prevista la rettifica appropriata;
- spesa automatica proposta e non ancora consuntivata;
- spesa creata per errore;
- spesa già annullata.

Offri un comando comprensibile **Elimina** o **Annulla spesa**, a seconda della situazione reale.

Se l'eliminazione non è possibile, non limitarti a disabilitare un pulsante: spiega la ragione e proponi l'azione corretta.

Non richiedere passaggi tecnici inutili per annullare una semplice spesa non ancora consolidata.

Non creare duplicati nelle operazioni di annullamento e rettifica.

## C. Gestione delle bozze

La gestione attuale delle bozze è poco intuitiva.

Analizza `DraftStore`, `PersistentDraft`, i banner delle bozze e i relativi percorsi UI.

Distingui in maniera rigorosa:

1. **Bozza locale non salvata** di una prenotazione/spesa/attività.
2. **Rendiconto in bozza** salvato nel database.
3. **Record effettivamente salvato**.

Non confondere queste tre categorie.

### Modifiche richieste

Per le bozze locali:

- pulsante "Riprendi";
- pulsante "Scarta";
- possibilità di scartare tutte le bozze locali, con conferma chiara;
- rimozione completa della bozza scartata;
- nessuna perdita del record originale già salvato.

Quando l'utente torna indietro dopo una modifica, usa un dialogo comprensibile, ad esempio:

"Vuoi salvare la modifica, conservarla come bozza o scartarla?"

Evita però dialoghi superflui quando non esistono modifiche.

Per i rendiconti in bozza:

- mostra chiaramente che si tratta di documenti non ancora confermati;
- permetti di abbandonare una bozza senza alterare rendiconti storici;
- preserva tutte le versioni confermate;
- non chiamare semplicemente "bozza" due oggetti con comportamenti differenti senza spiegazione.

## D. Diagnosticare perché le notifiche non arrivano

Questo è un problema reale segnalato sul telefono e NON deve essere considerato risolto soltanto perché i test automatici passano.

Analizza:

- `ReminderEngine.kt`;
- `ReminderPlanner.kt`;
- `DeviceSettings.kt`;
- manifest;
- receiver;
- AlarmManager;
- canali notifiche;
- permessi Android 13+;
- allarmi esatti;
- fasce di silenzio;
- risparmio energetico;
- riavvio del telefono;
- riprogrammazione;
- cancellazione e modifica delle attività;
- condizioni che impediscono la generazione dei promemoria.

### Requisiti

Nelle impostazioni notifiche introduci una diagnostica chiara:

- notifiche abilitate/disabilitate;
- permesso Android concesso/negato;
- disponibilità degli allarmi esatti;
- stato della fascia di silenzio;
- numero dei promemoria futuri realmente programmati;
- eventuali condizioni che ne impediscono la consegna.

Prevedi:

**1. Notifica di prova immediata**

Deve dimostrare se Android può mostrare una notifica.

**2. Test di promemoria programmato**

Deve permettere di programmare un promemoria di prova a breve, verificando tutto il percorso dallo scheduling alla ricezione.

**3. Diagnostica degli errori**

Se la notifica non può essere programmata, mostra la causa e l'azione per risolverla.

Se il problema dipende da impostazioni del dispositivo, collega alle impostazioni Android pertinenti, quando possibile.

Non richiedere permessi inutili, accesso Internet o servizi cloud.

### Test

Aggiungi test automatici per:

- notifiche attivate/disattivate;
- permessi mancanti;
- promemoria modificati;
- attività cancellate;
- promemoria duplicati;
- riavvio del dispositivo;
- fuso orario e ora legale;
- fasce di silenzio;
- scheduling e receiver.

I test JVM/Robolectric non dimostrano che una notifica sia realmente arrivata su un telefono.

Se non hai accesso a un dispositivo Android, dichiaralo apertamente e prepara una checklist pratica per l'utente, indicando esattamente come verificare una notifica immediata e una programmata.

Non dichiarare il bug definitivamente risolto senza prova sufficientemente rappresentativa.

## E. Regressioni da controllare

Dopo le modifiche dei tre interventi, verifica che funzionino ancora:

- salvataggio e modifica prenotazioni;
- selettore check-in/check-out;
- calendario;
- lista prenotazioni;
- spese;
- tassa automatica;
- IVA sulle commissioni OTA;
- pulizie e lavanderia;
- rendiconti sintetici e dettagliati;
- rettifiche;
- cancellazioni;
- bozze;
- notifiche;
- backup e ripristino.

Testare almeno una migrazione a partire da un database che rappresenti una versione precedente realmente distribuibile.

Non perdere gli snapshot storici confermati.

## F. Consegna Android obbligatoria

La base precedente era 0.4.10/code10. Se non esistono altre versioni nel frattempo, consegna una nuova 0.4.11/code11.

Verifica sempre i numeri prima di impostarli.

Il risultato non può limitarsi a commit e documentazione.

Devi:

1. Eseguire realmente compilazione, unit test e lint.
2. Generare un APK debug installabile.
3. Verificare application ID `it.elledue.control`.
4. Verificare versione e versionCode.
5. Verificare firma e compatibilità con l'APK installato precedentemente, per quanto dimostrabile.
6. Calcolare SHA-256.
7. Mettere l'APK in una posizione GitHub accessibile.
8. Verificare realmente il download dell'artefatto.
9. Fornire un collegamento diretto.
10. Spiegare come effettuare il backup e aggiornare l'app senza disinstallarla.
11. Fornire screenshot e PDF dimostrativi del risultato finale.
12. Preparare una PR di revisione senza merge.

Se l'API GitHub risulta nuovamente bloccata con `403 Forbidden`, non ripetere tentativi indefiniti e non dichiarare una PR creata. Fornisci il collegamento di confronto e le istruzioni per creare la PR manualmente.

Non richiedere privilegi illimitati per risolvere un problema di rete.

Non pubblicare keystore, credenziali, dati reali o backup dell'utente.

## G. Risposta finale

Fornisci:

- versione finale;
- branch;
- commit;
- modifiche per ciascuno dei tre interventi;
- bug risolti e bug ancora da verificare;
- risultati dei test;
- limiti delle prove;
- nuova versione dello schema Room;
- migrazioni;
- screenshot UI;
- PDF sintetico e dettagliato di esempio;
- link diretto APK verificato;
- hash SHA-256;
- compatibilità della firma;
- istruzioni di installazione;
- PR oppure link di confronto.

Non effettuare merge su `main` senza autorizzazione.

**Criterio di successo:** l'utente deve riuscire a consultare prenotazioni, inserire spese, generare rendiconti, gestire dati e ricevere notifiche con un'esperienza più chiara e meno operazioni manuali rispetto alla 0.4.10.