# ELLEDUE CONTROL — AGGIORNAMENTO CONSOLIDATO 0.4.9
## Stabilizzazione, gestione operativa, economia, calendario, rendiconti e registro problemi

### RUOLO

Agisci come responsabile tecnico del progetto Elledue Control, sviluppatore Android senior, progettista UX/UI e QA engineer.

Devi implementare concretamente le modifiche richieste in questo documento.

Non voglio una semplice analisi, una roadmap teorica o prototipi scollegati dall'applicazione.

Il risultato deve essere una nuova versione Android realmente installabile, verificabile e compatibile con i dati precedenti.

Repository:

`alelanza01/Elledue-Control`

Application ID:

`it.elledue.control`

Versione obiettivo: **0.4.9**, salvo che lo stato reale del repository richieda un diverso avanzamento del versionCode.

Prima di intervenire verifica il branch e l'ultimo commit effettivamente disponibili.

Crea un branch di sviluppo dedicato, senza merge automatico su main.

Non modificare il sito Elledue Management / Elledue Stays: è un progetto distinto.

---

# 1. VINCOLI FONDAMENTALI

L'app è utilizzata per la gestione reale di appartamenti, prenotazioni, operazioni, costi e rendiconti.

Sono quindi obbligatori:

- Nessuna perdita dei dati esistenti.
- Nessuna migrazione Room distruttiva.
- Preservazione dei rendiconti confermati e del relativo versionamento.
- Nessuna duplicazione di spese o movimenti economici.
- Compatibilità del backup e del ripristino.
- Conservazione delle funzionalità esistenti.
- Distinzione tra immobili gestiti per terzi e immobili propri/in comproprietà.
- Separazione tra previsioni economiche e importi effettivamente confermati.
- Utilizzo delle API Android ufficiali.
- Test automatici e QA visivo documentati.
- Nessun merge o rilascio definitivo senza autorizzazione.

Non reimplementare da zero funzionalità già correttamente sviluppate nella versione precedente: verificale e correggi ciò che non funziona.

Alcune funzioni erano state dichiarate implementate, ma durante l'utilizzo reale hanno manifestato problemi, in particolare notifiche programmate, calcoli economici e leggibilità del calendario.

L'esistenza del codice non equivale al funzionamento della funzione.

---

# 2. REGISTRO PROBLEMI IMMOBILI — NUOVA FUNZIONE APPROVATA

Implementare una nuova funzionalità chiamata:

**Problemi e interventi**

Deve consentire di registrare e seguire problemi relativi agli appartamenti.

Esempi:

- Condizionatore non funzionante.
- Serratura difettosa.
- Perdita d'acqua.
- Elettrodomestico guasto.
- Biancheria mancante.
- Problema segnalato da un ospite.
- Intervento di manutenzione da organizzare.
- Problema emerso durante una pulizia.

## 2.1 Creazione problema

Ogni segnalazione deve comprendere:

- Appartamento obbligatorio.
- Titolo breve.
- Descrizione.
- Categoria.
- Data della segnalazione.
- Priorità: bassa, normale, alta, urgente.
- Stato.
- Eventuale prenotazione collegata.
- Eventuale data prevista per la risoluzione.
- Eventuale persona o fornitore incaricato, se noto.
- Note operative.
- Eventuale costo associato, quando realmente registrato.

Non rendere obbligatori dati che normalmente non sono conosciuti al momento della segnalazione.

## 2.2 Stati

Prevedere almeno:

1. Aperto.
2. Da organizzare.
3. Intervento programmato.
4. In lavorazione.
5. Risolto.
6. Annullato / non pertinente.

La chiusura deve registrare la data effettiva della risoluzione.

Consentire di riaprire un problema precedentemente risolto.

## 2.3 Collegamento con le prenotazioni

Se un problema nasce durante un soggiorno, deve essere possibile collegarlo alla relativa prenotazione.

Tuttavia:

**Il problema appartiene all'immobile e non deve scomparire automaticamente alla conclusione del soggiorno.**

Deve rimanere aperto finché non viene risolto o annullato esplicitamente.

Se una prenotazione viene modificata, archiviata o cancellata, il registro deve mantenere un comportamento coerente senza creare riferimenti orfani.

## 2.4 Collegamento con Economia

Quando viene sostenuta una spesa per risolvere un problema, deve essere possibile collegare il relativo movimento economico alla segnalazione.

Esempio:

Problema: serratura difettosa.

Intervento: sostituzione cilindro.

Spesa registrata: €45.

Il costo deve essere contabilizzato una sola volta.

Non generare automaticamente una spesa soltanto perché è stato creato un problema o programmato un intervento.

## 2.5 Collegamento con calendario e Giornata operativa

Un problema con un intervento programmato deve comparire nella giornata pertinente.

Esempio:

14:30 — Intervento serratura · Enea's Home.

Toccando l'evento deve aprirsi direttamente la scheda del problema.

I problemi aperti particolarmente rilevanti devono essere riconoscibili anche dalla scheda dell'appartamento.

Non aggiungere notifiche automatiche ridondanti: utilizzare il sistema centralizzato dei promemoria, una volta riparato.

## 2.6 Interfaccia

Il registro deve essere semplice.

Prevedere filtri per:

- Appartamento.
- Stato.
- Priorità.
- Data.
- Problemi ancora aperti.

L'interfaccia deve permettere di capire rapidamente cosa manca da risolvere, senza trasformarsi in un complesso software di manutenzione.

---

# 3. CALENDARIO E NUOVA GIORNATA OPERATIVA

L'attuale calendario e soprattutto l'agenda risultano poco intuitivi e graficamente grezzi.

Voglio un redesign funzionale completo, coerente con l'identità visiva premium di Elledue.

## 3.1 Nuova interazione principale

Toccando una data nel calendario deve aprirsi:

**Giornata operativa**

Non voglio necessariamente una nuova sezione autonoma nella Home.

La Giornata operativa deve sostituire o evolvere l'attuale agenda giornaliera.

## 3.2 Contenuto della Giornata operativa

In alto mostrare:

- Data completa.
- Numero di check-in.
- Numero di check-out.
- Numero di attività.
- Eventuali cambi ospite nella stessa giornata.
- Eventuali problemi/interventi programmati.

Sotto mostrare una sequenza cronologica chiara degli eventi.

Esempio:

11:00 — Check-out · Enea's Home.

11:30 — Pulizia · Enea's Home.

14:30 — Intervento serratura · Casa Marina.

15:00 — Check-in · Enea's Home.

Questi sono soltanto esempi: nell'app non devono essere inventati orari o attività.

Se un evento non ha un orario configurato, deve essere mostrato correttamente tra le attività senza orario.

## 3.3 Azioni contestuali

Ogni evento deve permettere di accedere immediatamente all'operazione pertinente:

- Check-in → Riepilogo soggiorno.
- Check-out → Riepilogo soggiorno e messaggi pertinenti.
- Pulizia → Registrazione o conferma delle ore.
- Promemoria ospite → Messaggio da copiare.
- Attività personalizzata → Scheda attività.
- Problema immobile → Scheda problema.
- Intervento programmato → Dettaglio dell'intervento.

La Giornata operativa deve funzionare anche per le date passate e future.

Deve mostrare cosa era previsto, cosa è stato completato e cosa rimane aperto.

Non richiedere all'utente di reinserire manualmente informazioni già disponibili nelle altre sezioni.

## 3.4 Redesign del calendario mensile

Eliminare l'utilizzo di sigle incomprensibili come unica informazione visibile.

Distinguere chiaramente:

- Soggiorni.
- Check-in.
- Check-out.
- Pulizie.
- Attività.
- Interventi.

Utilizzare bande, pill o altri elementi compatti ma leggibili.

Separare il colore identificativo dell'immobile dal colore semantico dell'evento.

La legenda deve corrispondere effettivamente ai colori visualizzati.

Gestire correttamente check-out e check-in nello stesso giorno e la presenza di più immobili.

Non affidarsi esclusivamente ai colori: utilizzare anche testi o icone pertinenti.

---

# 4. INSERIMENTO PRENOTAZIONI PIÙ RAPIDO

## 4.1 Nessun appartamento preselezionato

Quando creo una prenotazione dalla funzione generale, il campo appartamento deve essere inizialmente vuoto.

Mostrare:

`Seleziona appartamento`

L'utente deve scegliere esplicitamente l'immobile.

Non selezionare automaticamente il primo appartamento disponibile.

Gli immobili archiviati non devono apparire nella selezione per nuove prenotazioni.

Durante la modifica di una prenotazione esistente deve invece rimanere correttamente selezionato il suo immobile.

## 4.2 Un solo calendario per le date

Sostituire la selezione separata di check-in e check-out con un vero date-range picker.

Flusso:

1. Apro il calendario una sola volta.
2. Seleziono check-in.
3. Seleziono direttamente check-out.
4. Confermo l'intervallo.

Mostrare immediatamente il numero di notti.

Gestire soggiorni a cavallo di mesi e anni e impedire periodi non validi.

Consentire check-out e nuovo check-in nello stesso giorno.

Le date devono essere preservate nelle bozze recuperate.

## 4.3 Stato automatico prenotazione

Alla creazione e consultazione:

- Check-in futuro → In arrivo / confermata.
- Soggiorno iniziato e non terminato → In corso.
- Check-out già trascorso → Conclusa.
- Prenotazione annullata → Cancellata.

Tenere conto anche degli orari quando pertinenti.

Una prenotazione storica inserita dopo la sua data di check-out deve risultare immediatamente conclusa.

Lo stato operativo NON deve essere confuso con il completamento economico.

## 4.4 Salvataggio e bozze

Verificare e, dove necessario, riparare:

- Modifica persistente di tutti i campi della prenotazione.
- Aggiornamento delle sezioni dipendenti.
- Recupero delle bozze quando Android chiude le Activity.
- Recupero dopo riavvio dell'app.
- Separazione delle bozze fra prenotazioni differenti.
- Eliminazione della bozza dopo il salvataggio riuscito.
- Possibilità esplicita di scartarla.
- Navigazione corretta alla schermata di origine.

Non salvare automaticamente una bozza come prenotazione definitiva.

---

# 5. COMMISSIONI PORTALI

Per le nuove prenotazioni impostare questi valori iniziali:

| Portale | Commissione predefinita |
|---|---:|
| Booking.com | 19,5% |
| Airbnb | 19,5% |
| Vrbo | 19,5% |
| Prenotazione diretta | 0% |

La percentuale deve essere sempre modificabile nella singola prenotazione.

Precedenza:

1. Override manuale prenotazione.
2. Configurazione specifica del portale per l'immobile.
3. Default generale.

Il 19,5% è una nostra impostazione operativa iniziale, non una tariffa universale dichiarata dei portali.

Mostrare separatamente:

- Prezzo prenotazione.
- Commissione OTA.
- Netto dopo commissione OTA.
- Compenso di gestione Elledue, quando applicabile.

Modificando la commissione, aggiornare i calcoli pertinenti senza creare doppioni e senza alterare retroattivamente i rendiconti confermati.

---

# 6. COSTI PRECOMPILATI PER IMMOBILI IN GESTIONE

Questa sezione aggiorna la precedente impostazione relativa alle pulizie.

Per gli immobili gestiti per conto di terzi voglio valori iniziali già compilati, ma liberamente modificabili.

## 6.1 Lavanderia

Default:

**€5 ogni due ospiti, arrotondando per eccesso.**

Esempi:

- 1–2 ospiti: €5.
- 3–4 ospiti: €10.
- 5–6 ospiti: €15.
- 7–8 ospiti: €20.

Tariffa e scaglione devono essere configurabili per appartamento.

Il valore si aggiorna quando cambia il numero degli ospiti, salvo override manuale.

## 6.2 Pulizie

Precompilare automaticamente:

**2 ore per soggiorno.**

La tariffa oraria deve essere configurabile per immobile.

Quindi, se la tariffa configurata è €10/ora:

2 ore previste = €20 previsti.

L'importo deve essere modificabile nella singola prenotazione.

IMPORTANTISSIMO:

Distinguere:

- Ore previste/precompilate.
- Ore effettive confermate.
- Costo previsto.
- Costo definitivo.

La previsione può essere mostrata immediatamente nel riepilogo economico, ma non deve essere rappresentata come spesa definitivamente sostenuta senza conferma.

Dopo il check-out deve essere possibile confermare le due ore proposte oppure modificarle.

Aggiornare la stessa voce economica senza generarne un'altra.

Segnalare eventuali consuntivi non confermati durante la preparazione del rendiconto.

Non introdurre una tariffa oraria universale non autorizzata: utilizzare quella configurata per il singolo immobile.

---

# 7. IMMOBILI PROPRI O IN COMPROPRIETÀ

Mantenere una distinzione esplicita fra:

- Immobile gestito per conto di terzi.
- Immobile proprio/in comproprietà.

Per gli immobili propri non applicare automaticamente una commissione di gestione Elledue come se esistesse un proprietario cliente.

Distinguere correttamente ricavi dell'attività, costi e risultati privati.

## 7.1 Categoria Trasporti

Aggiungere la voce economica:

**Trasporti**

Utilizzo principale: registrare il costo del viaggio per portare o ritirare la biancheria in lavanderia.

Prevedere:

- Importo.
- Data.
- Immobile.
- Eventuale prenotazione collegata.
- Numero di viaggi, se pertinente.
- Nota.
- Possibilità di configurare un importo abituale.
- Modifica manuale.

Non inventare automaticamente costi chilometrici o importi che non sono stati configurati.

Lavanderia e trasporti devono rimanere categorie economiche distinte.

Non attribuire due volte la medesima spesa.

La gestione avanzata di un singolo trasporto condiviso fra più prenotazioni rimane nel backlog futuro; in questa versione è sufficiente garantire che possa essere registrato una sola volta senza duplicazioni.

---

# 8. IMPOSTA DI SOGGIORNO

Per gli immobili ai quali si applica la regola indicata, configurare inizialmente:

- €3 per persona/notte.
- Massimo quattro notti tassabili.
- Ospiti soggetti all'imposta dai 16 anni compiuti, inclusi.
- Gli ospiti esenti vengono inseriti come quantità.

Non richiedere obbligatoriamente la data di nascita o l'età di tutti gli ospiti.

Dati essenziali:

- Numero ospiti totale.
- Numero ospiti esenti.
- Date del soggiorno.

Formula della configurazione iniziale:

`imposta prevista = 3 × (ospiti totali - ospiti esenti) × min(notti, 4)`

Esempio:

Quattro ospiti, uno esente, cinque notti:

€36.

Tutti i parametri devono essere configurabili per immobile e conformi alle regole comunali effettivamente pertinenti.

Distinguere rigorosamente:

- Dovuto.
- Riscosso.
- Residuo da riscuotere.
- Riversato.

Un importo automaticamente calcolato non deve essere considerato automaticamente incassato.

Le modifiche alla prenotazione devono aggiornare i calcoli previsti preservando la tracciabilità dei movimenti già registrati.

---

# 9. RIEPILOGO SOGGIORNO

Implementare una schermata unica raggiungibile dalla scheda prenotazione.

Deve mostrare:

### Dati principali
- Appartamento.
- Ospite.
- Provenienza.
- Date.
- Ospiti.
- Stato operativo.

### Economia
- Importo prenotazione.
- Commissione OTA.
- Netto OTA.
- Lavanderia.
- Pulizie previste o confermate.
- Imposta di soggiorno.
- Imposta riscossa e residua.
- Altre spese.
- Compenso di gestione, quando applicabile.

### Operatività
- Check-in.
- Check-out.
- Attività.
- Messaggi ospite.
- Promemoria.
- Eventuali problemi aperti collegati al soggiorno.

### Azioni rapide
- Modifica prenotazione.
- Registra pulizia.
- Aggiungi spesa.
- Registra pagamento tassa.
- Apri messaggio ospite.
- Apri problemi collegati.

La schermata deve utilizzare dati già registrati, senza creare ulteriori campi duplicati.

---

# 10. ECONOMIA E RENDICONTI

Riorganizzare Economia per renderla più intuitiva, preservando tutte le funzionalità esistenti.

Semplificare l'inserimento di una spesa dalla prenotazione: immobile e soggiorno devono risultare già associati.

Le spese non collegate a una prenotazione devono poter essere inserite direttamente da Economia.

## 10.1 Fedeltà al rendiconto originale

Utilizzare il precedente PDF del rendiconto Enea's Home come riferimento per la struttura informativa, se disponibile nel contesto o allegato al progetto.

Non è necessario replicarne l'impaginazione grafica.

Il report deve distinguere:

### Riepilogo prenotazioni
- Ospite.
- Date.
- Importo prenotazione.
- Commissioni OTA.

Accanto al prezzo della prenotazione deve comparire la commissione del portale, NON il compenso di gestione Elledue.

### Resoconto compenso gestione
- Fatturato.
- Commissioni OTA.
- Netto OTA.
- Percentuale gestione applicata.
- Compenso Elledue.

Utilizzare la dicitura:

`Compenso di gestione Elledue`

Non utilizzare `stimata` come parola fissa dell'etichetta.

### Resoconto spese
- Pulizie.
- Lavanderia.
- Detersivi.
- Spese varie.
- Altre categorie applicabili.
- Totale.

### Resoconto finale
- Compensi.
- Spese.
- Anticipi.
- Movimenti relativi alla tassa di soggiorno effettivamente riscossa, se pertinenti.
- Saldo finale.

Le somme devono riconciliarsi con i movimenti reali.

Non replicare eventuali discrepanze aritmetiche contenute nei vecchi documenti.

## 10.2 Report sintetico e dettagliato

Utilizzare un unico motore contabile.

Il report dettagliato deve mostrare anche le singole operazioni.

Quello sintetico deve aggregare i valori, mantenendo tutte le categorie economiche rilevanti.

I totali devono coincidere a parità di immobile, periodo e perimetro.

## 10.3 Esclusione di una prenotazione dal rendiconto

Aggiungere una spunta:

`Includi nel rendiconto proprietario`

Attiva per impostazione predefinita.

Quando legittimamente disattivata, la prenotazione:

- Rimane nel database.
- Rimane nel calendario.
- Contribuisce all'occupazione reale.
- Mantiene attività e adempimenti.
- Rimane nelle aree economiche interne pertinenti.
- Non entra nel dettaglio e nei totali dello specifico rendiconto proprietario.

Richiedere una motivazione interna dell'esclusione e conservarne lo storico.

L'opzione non deve consentire di occultare somme contrattualmente dovute al proprietario. Il perimetro del rendiconto deve rimanere corretto e trasparente.

I guadagni di competenza Elledue devono risultare nella sezione privata, senza essere automaticamente esposti nei PDF proprietario.

Non modificare silenziosamente rendiconti già confermati.

---

# 11. CALCOLO CORRETTO DELL'OCCUPAZIONE

Separare competenza contabile e occupazione effettiva.

La convenzione contabile esistente rimane:

**L'intero soggiorno è attribuito al mese del check-in.**

Le spese operative mantengono la propria competenza.

Le analisi dell'occupazione devono invece distribuire le notti nei mesi in cui sono effettivamente trascorse.

Esempio:

Check-in: 30 settembre.

Check-out: 7 ottobre.

Risultato:

- Una notte a settembre.
- Sei notti a ottobre.

Il giorno del check-out è escluso dal conteggio delle notti.

Gestire correttamente:

- Soggiorni a cavallo di mesi.
- Soggiorni a cavallo di anni.
- Anni bisestili.
- Check-out e nuovo check-in nello stesso giorno.
- Prenotazioni cancellate.
- Prenotazioni escluse dal rendiconto proprietario ma realmente occupanti l'immobile.
- Eventuali sovrapposizioni erronee, senza gonfiare il tasso di occupazione.

Non modificare per questo motivo la competenza contabile dei rendiconti storici.

---

# 12. COPIA MESSAGGIO CALENDARIO — ULTIMA MODIFICA APPROVATA

Conservare la funzionalità che permette di copiare il calendario delle prenotazioni come testo pronto per WhatsApp, separatamente per ogni appartamento.

Modificare però il filtro temporale.

Quando copio il messaggio, devono essere incluse:

**Tutte le prenotazioni non cancellate il cui check-out è uguale o successivo alla data della copia.**

Esempio: oggi è il 10 ottobre.

| Prenotazione | Includere? |
|---|---|
| 2–8 ottobre | No |
| 7–9 ottobre | No |
| 7–10 ottobre | Sì |
| 7–12 ottobre | Sì |
| 10–15 ottobre | Sì |
| 20–25 ottobre | Sì |

Il confronto è sulla data, indipendentemente dall'orario del check-out.

Una prenotazione terminata oggi deve comparire anche se il messaggio viene copiato nel pomeriggio.

Mantenere sempre le date originali del soggiorno.

Non sostituire il check-in con la data corrente.

Non imporre un limite futuro arbitrario.

Ordinare cronologicamente le prenotazioni.

Formato indicativo:

NOME APPARTAMENTO

NOME OSPITE (4 ospiti)

Check-in 07/10/2026 ore 15:00

Check-out 10/10/2026 ore 11:00

Ogni immobile mantiene il proprio pulsante indipendente di copia.

Il calendario completo dell'app deve invece continuare a mostrare anche tutte le prenotazioni passate.

La copia non deve inviare automaticamente alcun messaggio.

---

# 13. NOTIFICHE AUTOMATICHE: RIPARAZIONE OBBLIGATORIA

Problema osservato:

La notifica di prova arriva, ma quelle programmate non vengono ricevute regolarmente.

Verificare e correggere l'intera catena:

- Creazione e salvataggio del promemoria.
- Programmazione Android.
- Notification Channels.
- Permessi.
- Modifiche o cancellazioni della prenotazione.
- Riavvio del telefono.
- App non aperta.
- Fuso orario e ora legale.
- Restrizioni Android sugli allarmi.
- Prevenzione delle notifiche duplicate.

Non dichiarare risolto il problema perché funziona il pulsante `Notifica di prova`.

Implementare test che esercitino realmente il percorso delle notifiche programmate, documentando separatamente ciò che richiede verifica su dispositivo fisico.

## 13.1 Notifiche collegate ai messaggi ospite

Quando tocco una notifica relativa a un messaggio da inviare, Control deve aprire direttamente il testo pertinente.

Esempio:

Il giorno prima del check-out arriva una notifica che invita a preparare il messaggio per l'ospite.

Toccandola si apre il template contestualizzato con:

- Data del check-out.
- Orario.
- Istruzioni.
- Eventuale importo della tassa di soggiorno ancora da riscuotere.

Se la tassa è già stata riscossa integralmente, non deve comparire un sollecito.

Prevedere:

- Copia messaggio.
- Modifica testo.
- Eventuale condivisione WhatsApp.
- Stato manuale `Gestito`.

Non inviare automaticamente comunicazioni agli ospiti.

La copia del testo non equivale alla conferma dell'effettivo invio.

Utilizzare i template italiano/inglese già presenti senza duplicarne inutilmente l'architettura.

---

# 14. PROTEZIONE ACCESSO

Prevedere una protezione facoltativa tramite:

- Biometria supportata dal dispositivo.
- Credenziali sicure del dispositivo.
- Eventuale PIN applicativo implementato correttamente.

Utilizzare BiometricPrompt e le API ufficiali Android.

Non salvare PIN in chiaro.

Non sviluppare un sistema biometrico proprietario.

Gestire fallback, errori, recupero e ritorno dal background.

Nessuna cancellazione dei dati in seguito a tentativi di autenticazione falliti.

Se tocco una notifica con l'accesso protetto attivo, dopo l'autenticazione devo arrivare alla schermata corretta, non genericamente alla Home.

Proteggere anche le informazioni riservate mostrate nelle notifiche e nelle anteprime del sistema operativo.

---

# 15. ANALYTICS E CONFRONTI — REDESIGN

Migliorare sensibilmente la presentazione grafica di Analytics e Confronti.

Non limitarsi a cambiare il colore dei grafici.

Voglio un risultato più moderno, curato e intuitivo.

Mantenere l'identità visiva Elledue:

- Navy `#111b32`.
- Oro/beige `#dbbf8b`.
- Colori aggiuntivi coerenti quando servono a distinguere dati ed eventi.

Prevedere una chiara separazione fra:

### Analisi operative
- Occupazione.
- Notti vendute.
- Notti disponibili.
- Check-in.
- Check-out.
- Durata soggiorni.
- Confronti fra immobili.

### Analisi economiche
- Fatturato.
- Commissioni OTA.
- Netto OTA.
- Pulizie.
- Lavanderia.
- Altre spese.
- Compensi Elledue.
- Risultati economici pertinenti.

### Guadagni privati
Tenere separati i risultati riservati Elledue dai rendiconti e dalle esportazioni destinate ai proprietari.

Richiedo grafici leggibili su smartphone, filtri chiari, stati vuoti curati, unità di misura e calcoli coerenti.

Non utilizzare dati fittizi nelle schermate operative dell'app.

---

# 16. ARCHIVIAZIONE, ELIMINAZIONE E INTEGRITÀ

Verificare il funzionamento delle operazioni già richieste nelle versioni precedenti.

Distinguere:

- Archiviazione.
- Annullamento di una prenotazione.
- Eliminazione definitiva.

Un appartamento archiviato non deve comparire automaticamente nei filtri operativi attivi, nelle statistiche correnti e nelle nuove prenotazioni.

Lo storico deve rimanere accessibile mediante un percorso esplicito.

L'eliminazione definitiva deve essere protetta da conferme adeguate e gestire correttamente tutti i collegamenti dipendenti.

La presenza del nuovo Registro problemi non deve creare riferimenti orfani o provocare eliminazioni a cascata non intenzionali.

Preservare sempre i documenti contabili confermati secondo l'architettura di versionamento esistente.

---

# 17. TEST DI ACCETTAZIONE OBBLIGATORI

Oltre alle regressioni della suite precedente, verificare almeno questi scenari:

| Scenario | Risultato richiesto |
|---|---|
| Nuova prenotazione | Nessun appartamento preselezionato |
| Selezione soggiorno | Check-in e check-out nello stesso calendario |
| Prenotazione già terminata | Stato conclusa |
| Booking da €500 | Commissione iniziale €97,50 |
| Prenotazione diretta | Commissione OTA 0% |
| Lavanderia 4 ospiti | €10 con default configurato |
| Pulizie immobile gestito | 2 ore iniziali previste e modificabili |
| Conferma ore effettive | Aggiornamento della stessa voce, nessun doppione |
| Tassa: 4 ospiti, 1 esente, 5 notti | €36 con la regola configurata |
| Tassa non riscossa | Residuo corretto |
| Immobile proprio | Categoria Trasporti disponibile |
| Prenotazione esclusa dal report | Rimane nelle aree interne pertinenti |
| Report dettagliato/sintetico | Stessi totali contabili |
| 30 settembre – 7 ottobre | 1 notte settembre, 6 ottobre |
| Copia calendario il 10 ottobre | Include soggiorno 7–10 ottobre |
| Copia calendario l'11 ottobre | Esclude soggiorno 7–10 ottobre |
| Problema collegato a soggiorno concluso | Rimane aperto |
| Problema con intervento programmato | Compare nella Giornata operativa |
| Problema risolto | Mantiene storico e data risoluzione |
| Spesa collegata a problema | Registrata una sola volta |
| Tap data nel calendario | Apre la Giornata operativa corretta |
| Check-out e check-in nello stesso giorno | Entrambi visibili |
| Notifica programmata | Funzionamento verificato oltre il pulsante Test |
| Tap notifica ospite | Apre il messaggio pertinente |
| Recupero bozza | Campi preservati dopo riavvio app |
| Immobile archiviato | Escluso dalle viste operative attive |
| Migrazione database | Dati preesistenti preservati |

I test devono coprire anche casi limite, record incompleti, modifiche successive e compatibilità con backup precedenti.

Non eliminare test esistenti per ottenere artificialmente una suite PASS.

---

# 18. QA VISIVO

Produrre screenshot di verifica delle schermate principali, in particolare:

- Nuova prenotazione.
- Date-range picker.
- Riepilogo soggiorno.
- Giornata operativa.
- Calendario mensile.
- Registro problemi.
- Scheda dettaglio problema.
- Registrazione pulizie.
- Registrazione spese.
- Report sintetico.
- Report dettagliato.
- Analytics.
- Confronti.
- Protezione accesso.
- Messaggi ospiti aperti dalle notifiche.

Controllare anche:

- Dimensioni delle aree touch.
- Testi tagliati.
- Contrasto.
- Scroll.
- Navigazione.
- Stati vuoti.
- Consistenza dei colori.
- Schermi di dimensioni differenti.

Distinguere esplicitamente screenshot sintetici o Robolectric da prove realmente effettuate su dispositivo Android.

---

# 19. ORDINE DI IMPLEMENTAZIONE

Organizza il lavoro in commit verificabili secondo le dipendenze effettive.

Priorità consigliate:

### P0 — Integrità dei dati e contabilità

- Audit della versione esistente.
- Persistenza.
- Nuovo inserimento prenotazioni.
- Commissioni OTA.
- Lavanderia.
- Pulizie precompilate e consuntivo.
- Tassa soggiorno.
- Trasporti.
- Collegamento spese.
- Report.
- Visibilità nel rendiconto.
- Occupazione per notte effettiva.
- Copia calendario corretta.

### P1 — Operatività

- Giornata operativa.
- Redesign calendario.
- Registro problemi.
- Riepilogo soggiorno.
- Riparazione notifiche.
- Collegamento notifiche/messaggi.

### P2 — Qualità e protezione

- Accesso biometrico/PIN.
- Analytics e Confronti.
- Rifiniture UX.
- QA complessivo.

Non dichiarare completato un blocco se sono stati realizzati soltanto i layout.

---

# 20. CONSEGNA FINALE

Richiedo:

- Nuova build Android.
- APK realmente accessibile e scaricabile.
- Versione e versionCode corretti.
- Application ID invariato.
- Firma coerente con la precedente build, per consentire l'aggiornamento in place quando possibile.
- SHA-256 calcolato sull'APK effettivo.
- Test unitari e di integrazione.
- Esito lint.
- Report QA.
- Screenshot.
- Changelog.
- Documentazione migrazione.
- Limitazioni residue dichiarate esplicitamente.

Un semplice percorso interno `/workspace/artifacts/...` non costituisce, da solo, una consegna scaricabile.

Non pubblicare release definitive e non effettuare merge senza autorizzazione.

---

# 21. BACKLOG FUTURO — DA CONSERVARE, NON IMPLEMENTARE ORA

Durante il confronto progettuale sono emerse altre idee interessanti che NON voglio perdere.

Non sono però automaticamente incluse nell'attuale sviluppo.

Mantieni una sezione dedicata nel backlog progettuale con:

- Control Pulse: eventuale evoluzione della Home intelligente, da rivalutare dopo la Giornata operativa.
- Control Check: rilevazione avanzata delle anomalie.
- Chiusura guidata del soggiorno.
- Preparazione avanzata dell'appartamento tra check-out e nuovo check-in.
- Ripartizione dei trasporti condivisi fra più soggiorni.
- Riconciliazione avanzata degli incassi OTA.
- Allegati e fotografie alle spese.
- Proiezioni economiche.
- Scorte minime degli appartamenti.
- Registro permanente delle dotazioni.
- Annotazioni rapide durante la giornata.

Queste idee non devono distrarre dallo sviluppo attualmente approvato.

La Giornata operativa e il Registro problemi, invece, SONO approvati per questa versione.

---

# OBIETTIVO DEFINITIVO

Voglio che Elledue Control diventi uno strumento operativo affidabile e intuitivo.

Devo poter inserire una prenotazione, trovare automaticamente i valori abituali, modificarli quando necessario, gestire attività e problemi, monitorare correttamente spese e occupazione, ricevere notifiche realmente funzionanti e generare rendiconti fedeli.

L'app deve ridurre il lavoro manuale, non moltiplicarlo.

Non sacrificare la stabilità per completare frettolosamente un numero elevato di schermate.

**Implementa tutte le modifiche approvate, verifica le regressioni e consegna un APK effettivamente utilizzabile.**