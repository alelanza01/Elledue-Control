# CODEX — L2 Control — Intervento 2/3
## Calendario comprensibile, lista prenotazioni e revisione UX

Continua lo sviluppo di `alelanza01/Elledue-Control` sul branch utilizzato nel precedente intervento, indicativamente `feature/0.4.11-user-feedback`.

Non ripartire da `main` o dalla vecchia 0.4.10, perché perderesti le modifiche contabili appena effettuate.

Non effettuare merge.

## Obiettivo

L2 Control deve risultare intuitiva a un operatore che gestisce più immobili e numerose prenotazioni.

Attualmente alcuni elementi hanno significato soltanto per chi ha sviluppato l'app. Voglio una navigazione comprensibile, pochi passaggi e sezioni organizzate secondo le attività reali.

## A. Ripensare il calendario

Il precedente intervento ha ridotto l'altezza del calendario, ma il risultato non è chiaro.

La griglia attuale mostra numeri, frecce ↘/↗, puntini e indicatori di colore.

**Questa soluzione non va considerata soddisfacente soltanto perché il calendario è compatto.**

Riprogetta la vista affinché un utente possa capire:

- quali giorni sono occupati;
- a quale immobile appartiene l'occupazione;
- quando iniziano i soggiorni;
- quando finiscono;
- dove sono presenti cambi ospiti.

### Soluzione richiesta

Utilizza una vista mensile leggibile, con segmenti o barre visive di soggiorno quando possibile.

Evita che frecce e punti costituiscano l'unico linguaggio grafico.

Mantieni la compattezza, ma non sacrificare la comprensione.

Se sono visibili tutti gli immobili, l'interfaccia deve mantenere una rappresentazione ordinata; se si seleziona un singolo immobile, deve evidenziarne chiaramente gli intervalli occupati.

Valuta una legenda sintetica permanente e, soprattutto, un sistema visivo che non richieda di interpretare simboli arbitrari.

Toccando il giorno, mostra un dettaglio con:

- arrivi;
- partenze;
- soggiorni attivi;
- orari;
- immobile;
- ospiti;
- pulizie e attività.

Le prenotazioni devono essere apribili direttamente.

Non rendere obbligatorio scorrere molte schermate per trovare un soggiorno.

### Prova UX

Produci screenshot delle versioni:

- mese senza prenotazioni;
- mese con un immobile;
- mese con tre immobili e più prenotazioni sovrapposte tra immobili;
- mese con cambio ospiti nello stesso giorno;
- larghezza 320 dp;
- caratteri ingranditi.

Non accettare un'interfaccia illeggibile semplicemente perché supera i test di rendering.

## B. Creare una vera sezione Prenotazioni

Oggi manca un percorso pratico e autonomo per consultare le prenotazioni.

Implementa una sezione **Prenotazioni** facilmente raggiungibile, distinta dalla vista calendario anche se condivide repository e dati.

Deve offrire:

- elenco delle prenotazioni;
- ricerca per nome/cognome dell'ospite;
- filtro immobile;
- filtro mese o intervallo;
- prossime prenotazioni;
- prenotazioni in corso;
- prenotazioni passate;
- prenotazioni annullate, con filtro dedicato;
- ordinamento per check-in;
- accesso immediato al dettaglio;
- pulsante nuova prenotazione.

Ogni riga deve mostrare con chiarezza:

- nome ospite;
- immobile;
- check-in/check-out;
- numero ospiti;
- stato del soggiorno.

La schermata non deve essere una copia poco pratica della vista Agenda filtrata al mese.

**La ricerca deve funzionare anche sulle prenotazioni storiche**, indipendentemente dal mese attualmente selezionato nel calendario.

Prevedi un accesso diretto e chiaro nella navigazione, senza aggiungere una quinta voce alla barra inferiore se questo compromette la semplicità generale. Puoi riorganizzare le sezioni o usare un accesso secondario ben visibile, ma non nascondere Prenotazioni in un menu poco evidente.

## C. Semplificare Economia

La sezione attuale presenta concetti come "Movimenti" e varie operazioni contabili che non risultano immediatamente comprensibili.

Non cancellare la contabilità sottostante.

Riorganizza l'interfaccia con priorità:

1. Riepilogo economico.
2. Spese.
3. Rendiconti.
4. Anticipi e pagamenti.
5. Eventuali dettagli contabili avanzati, accessibili quando necessari.

La terminologia deve essere orientata alle operazioni reali.

Evita di presentare "Movimenti" come una macrofunzione generica senza spiegazione.

La registrazione delle spese deve rimanere semplice:

- descrizione;
- importo;
- immobile;
- eventuale prenotazione;
- salvataggio.

Mostra solo gli ulteriori campi necessari al caso concreto.

## D. Rendere comprensibili "Problemi e interventi"

La 0.4.9 ha introdotto un registro dei problemi e degli interventi.

È una funzione potenzialmente utile per guasti e manutenzioni, ma attualmente il suo significato non è chiaro.

Valuta una denominazione orientata all'utente, come **Manutenzioni**, **Segnalazioni** oppure **Problemi dell'immobile**.

Non usare termini generici senza contesto.

Una segnalazione dovrebbe poter essere:

- associata all'immobile;
- eventualmente collegata a una prenotazione;
- descritta in modo semplice;
- contrassegnata come aperta, in lavorazione o risolta;
- collegata a una spesa già registrata, quando pertinente.

Non generare automaticamente una spesa all'apertura di una segnalazione.

Se la funzione non è rilevante nella schermata Rendiconti, rimuovi da lì i collegamenti superflui e posizionala in un'area operativa più appropriata.

## E. Riorganizzare la Home

Sopra la sezione "Il tuo portafoglio" compaiono avvisi come "Rendiconto da verificare" o "Tassa da riscuotere" che risultano collocati in modo casuale.

Riprogetta la gerarchia visiva.

Mantieni l'identità grafica premium esistente.

Suggerimento:

1. Intestazione e panoramica.
2. Il tuo portafoglio.
3. Azioni rapide.
4. Sezione riconoscibile "Da fare" / "Attività in evidenza".
5. Prossimi check-in, check-out e altre operazioni.

Gli avvisi devono essere pertinenti, derivati da dati reali e cliccabili.

Non mostrare indiscriminatamente "Tassa da riscuotere" se il nuovo modello economico applica automaticamente l'anticipo e non richiede più una conferma ordinaria.

Mostra solo anomalie o eccezioni effettive.

## F. Ridurre attriti e passaggi superflui

Rivedi:

- numero di pulsanti per operazione;
- conferme inutili;
- campi precompilabili;
- accessibilità ai comandi principali;
- nomi poco comprensibili;
- schermate troppo lunghe;
- ritorno alla schermata precedente;
- feedback dopo i salvataggi;
- ricerca e filtri.

Conserva il selettore unico check-in/check-out già realizzato nella 0.4.10.

Non reintrodurre pulsanti duplicati per le date.

## G. Test e consegna

Aggiungi test Compose e navigazione per:

- nuova vista mensile;
- apertura del dettaglio da una data;
- ricerca prenotazioni;
- filtri e stati;
- prenotazioni annullate;
- accessibilità da Home;
- creazione e apertura prenotazione;
- revisione Economia;
- registro manutenzioni;
- avvisi Home;
- schermi piccoli e caratteri ingranditi.

Genera screenshot effettivi per la verifica.

Non modificare i calcoli contabili introdotti nel prompt 1 se non per correggere regressioni dimostrate.

Esegui i test, crea commit e restituisci branch, hash, screenshot, risultati e problemi residui.

Non effettuare merge su `main`.

Il progetto proseguirà sullo stesso branch con il prompt 3/3.