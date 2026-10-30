# ELLEDUE CONTROL — MILESTONE 0.4.8
## Stabilizzazione, correzione dei problemi riscontrati sul telefono ed evoluzione operativa

Agisci come Senior Android Developer, Software Architect, Product Designer, UX/UI Designer e QA Engineer.

Ho installato e provato Elledue Control. Durante l'utilizzo reale ho individuato problemi e mancanze che ora devono essere risolti concretamente.

Non voglio un'altra proposta teorica, un semplice restyling o una nuova applicazione: voglio un aggiornamento realmente funzionante, costruito sul progetto esistente.

## 1. Repository e condizioni iniziali

Repository:

`alelanza01/Elledue-Control`

Ultima consegna nota:

- Versione: `0.4.7`
- Branch: `feature/0.4.7-operations`
- Commit: `ea7e654adc548e9d89160fb946a7e87df05fbe2d`
- Application ID: `it.elledue.control`

Prima di lavorare, verifica lo stato effettivo del repository e la presenza di eventuali commit successivi. Il commit indicato è un riferimento, non un'autorizzazione a tornare indietro.

Crea un nuovo branch dedicato alla 0.4.8 partendo dalla versione effettivamente più aggiornata e pertinente.

Non fare merge su `main`, non distribuire pubblicamente l'app e non alterare il progetto del sito web.

Mantieni la compatibilità con l'installazione precedente e con i dati esistenti.

### Priorità di lavoro

Organizza l'intervento in questo ordine:

**P0 — Integrità e affidabilità dei dati**
- Modifiche alle prenotazioni che non vengono salvate.
- Perdita dei contenuti dei moduli quando l'app viene chiusa.
- Eliminazione definitiva di prenotazioni e immobili.
- Correttezza dei calcoli contabili e della tassa di soggiorno.

**P1 — Architettura gestionale**
- Separazione immobili propri e immobili di terzi.
- Economia più intuitiva.
- Controllo delle prenotazioni incluse nei rendiconti.

**P2 — Esperienza operativa**
- Calendario più curato e realmente cromatico.
- Grafici migliori.
- Notifiche Android.
- Accesso biometrico/PIN.

Non considerare una funzionalità completata soltanto perché è presente un pulsante o una schermata.

---

# 2. BUG CRITICO — LE MODIFICHE ALLE PRENOTAZIONI NON SI SALVANO

Durante i test reali ho riscontrato che modificare una prenotazione non produce il salvataggio atteso.

Questa è una priorità assoluta.

Individua la causa, senza limitarti a inserire messaggi grafici di conferma.

Verifica:

- caricamento iniziale dei dati nel modulo;
- corretto binding tra campi e stato del form;
- validazione;
- repository e operazioni Room;
- identificativo della prenotazione modificata;
- transazione di aggiornamento;
- propagazione alle altre schermate;
- persistenza dopo chiusura, riavvio e ricreazione dell'attività Android.

Una modifica deve essere salvata realmente nel database.

Verifica tutti i campi modificabili, non soltanto il nome dell'ospite:

- ospite;
- numero ospiti;
- immobile;
- date e orari;
- portale;
- importi;
- commissioni OTA;
- parametri contabili;
- note;
- altri campi già presenti.

Quando modifico una prenotazione, i relativi dati devono aggiornarsi anche in:

- calendario;
- dettaglio;
- copia calendario;
- tassa di soggiorno prevista;
- analisi;
- contabilità;
- rendiconto ancora modificabile;
- messaggi ospiti con variabili dinamiche.

Non devono nascere prenotazioni duplicate.

Se una modifica ha effetti su movimenti economici già consolidati o rendiconti confermati, applica le regole di rettifica/versionamento esistenti e segnala esplicitamente le conseguenze. Non alterare silenziosamente documenti storici confermati.

Mostra un messaggio di conferma soltanto dopo il successo effettivo dell'operazione.

### Test di accettazione fondamentale

Crea una prenotazione, salvala, modifica almeno cinque campi, salva nuovamente, chiudi completamente l'app e riaprila.

Tutti i dati devono rimanere aggiornati e coerenti in ogni schermata collegata.

---

# 3. PERSISTENZA DELLE BOZZE — NON PERDERE QUELLO CHE STO COMPILANDO

Attualmente, se sto compilando qualcosa ed esco dall'applicazione, quando rientro il modulo si ripristina e perdo ciò che avevo scritto.

Questo comportamento deve essere corretto.

Voglio il salvataggio automatico locale delle bozze durante la compilazione.

La funzione deve riguardare almeno:

- nuova prenotazione;
- modifica prenotazione;
- nuovo immobile;
- modifica immobile;
- nuova attività;
- modifica attività;
- nuova spesa o movimento;
- configurazioni economiche;
- modelli dei messaggi ospiti;
- altri moduli significativi presenti.

### Comportamento richiesto

Se interrompo la compilazione, passo a un'altra app, blocco il telefono, subisco la ricreazione dell'Activity o Android termina il processo, i dati inseriti non devono sparire.

Alla riapertura devo poter recuperare la bozza.

Implementa una persistenza locale effettiva: non affidarti esclusivamente allo stato in memoria di un ViewModel.

Mostra, quando appropriato:

- Riprendi bozza.
- Scarta bozza.
- Continua a modificare.

Non creare prenotazioni, immobili o movimenti contabili definitivi mentre sto soltanto scrivendo una bozza.

Le bozze devono essere isolate per tipo di modulo e record interessato, evitando che la modifica di una prenotazione sovrascriva quella di un'altra.

Dopo il salvataggio definitivo riuscito, la relativa bozza deve essere eliminata o contrassegnata come completata.

Proteggi l'app anche da salvataggi duplicati causati da doppi tocchi.

---

# 4. ELIMINAZIONE DEFINITIVA DI IMMOBILI E PRENOTAZIONI

Attualmente non posso eliminare realmente immobili e prenotazioni: posso soltanto archiviarli o annullarli.

Voglio poter scegliere.

## Immobili

Prevedi due comandi chiaramente distinti:

1. Archivia immobile.
2. Elimina definitivamente immobile.

L'archiviazione serve a conservarne lo storico senza mantenerlo tra gli immobili operativi.

L'eliminazione definitiva deve rimuovere realmente l'immobile e i dati collegati dal database operativo, secondo una procedura coerente e controllata.

Un immobile eliminato non deve continuare a comparire in:

- Home;
- calendario;
- Economia;
- Analisi;
- filtri;
- selettori;
- messaggi;
- rendiconti correnti;
- altre sezioni dell'app.

Non devono rimanere record orfani, risultati fantasma o statistiche che continuano a includere l'immobile eliminato.

Prima dell'eliminazione mostra una conferma forte con un riepilogo delle conseguenze: numero di prenotazioni, attività, movimenti, documenti e altri dati coinvolti.

Se esistono rendiconti confermati o registrazioni storiche collegate, non eseguire cancellazioni parziali o modifiche contabili silenziose. Predisponi un flusso esplicito di eliminazione delle dipendenze interessate, con verifica e backup preventivo, distinguendo chiaramente ciò che verrà eliminato.

L'operazione deve essere atomica: o viene completata coerentemente, oppure i dati rimangono integri.

## Prenotazioni

Prevedi tre operazioni distinte:

- Annulla prenotazione.
- Elimina definitivamente prenotazione.
- Modifica prenotazione.

L'eliminazione definitiva non deve equivalere internamente a un semplice annullamento con filtro nascosto.

Una prenotazione eliminata deve scomparire da tutte le sezioni operative e dai relativi aggregati economici.

Le attività e registrazioni economiche direttamente dipendenti devono essere analizzate e trattate coerentemente.

Non eliminare, però, spese indipendenti dalla prenotazione soltanto perché appartengono allo stesso immobile.

Se la prenotazione compare in un rendiconto confermato, richiedi la gestione esplicita dell'incompatibilità, inclusa l'eventuale eliminazione o invalidazione controllata del documento collegato. Non lasciare PDF o totali apparentemente validi che fanno riferimento a dati eliminati.

## Sicurezza

- Conferma forte prima dell'eliminazione.
- Backup preventivo accessibile all'utente.
- Gestione dei fallimenti senza database parzialmente corrotto.
- Nessuna cancellazione involontaria di record non collegati.
- Distinzione grafica evidente fra Archivia/Annulla ed Elimina definitivamente.

Precisa nell'interfaccia che la cancellazione dal database corrente non può cancellare retroattivamente backup esterni o PDF precedentemente condivisi.

---

# 5. IMMOBILI ARCHIVIATI — CORREZIONE ECONOMIA E FILTRI

Attualmente, anche dopo aver archiviato un appartamento, questo continua a comparire nella sezione Economia.

Non lo voglio.

### Regola operativa

Gli immobili archiviati non devono apparire nelle selezioni e nei riepiloghi operativi ordinari.

Devono essere esclusi per impostazione predefinita da:

- Economia;
- Home;
- calendario operativo;
- Analisi corrente;
- menu di selezione;
- statistiche correnti;
- liste degli immobili attivi.

Lo storico di un immobile archiviato può rimanere consultabile soltanto tramite una sezione o filtro esplicito dedicato all'archivio, senza contaminare le viste normali.

Gli immobili eliminati definitivamente non devono apparire neppure tramite il filtro Archivio.

Verifica e correggi la logica a livello di query/repository, non soltanto nascondendo componenti grafici.

---

# 6. DISTINZIONE FONDAMENTALE: IMMOBILI IN GESTIONE E IMMOBILI PROPRI

Ho anche appartamenti che non gestisco per conto di proprietari esterni.

Sono immobili nostri, in comproprietà con soci.

Non voglio che vengano trattati come immobili acquisiti in gestione per conto terzi.

Attualmente questa distinzione manca o non è sufficientemente strutturata.

## Nuova classificazione

Introduci almeno due tipologie di immobile:

**A. Immobile in gestione**

Immobile appartenente a un proprietario esterno, gestito da Elledue secondo un mandato e una commissione di gestione.

**B. Immobile proprio / in comproprietà**

Immobile operato direttamente, eventualmente insieme ad altri soci, senza mandato di gestione per conto di un proprietario esterno.

La tipologia deve essere selezionabile in fase di creazione dell'immobile e chiaramente identificabile nel dettaglio.

## Logica economica

Per gli immobili in gestione continua ad applicarsi la logica della commissione Elledue prevista dal singolo contratto.

Per gli immobili propri:

- non deve essere obbligatoria alcuna percentuale di gestione Elledue;
- non deve essere generata automaticamente una commissione di property management;
- non deve essere creato un rendiconto destinato a un proprietario esterno come se l'immobile fosse gestito per conto terzi;
- devono poter essere registrati ricavi, OTA, pulizie, lavanderia, spese operative e altri movimenti pertinenti;
- deve essere possibile analizzare il risultato economico dell'immobile.

La presenza di soci non significa che la quota economica di ciascuno sia nota o uguale.

Predisponi eventualmente dati configurabili relativi alla comproprietà, ma non inventare percentuali di partecipazione, distribuzioni di utili o accordi fra soci.

Se la suddivisione economica non è configurata, mostra soltanto il risultato complessivo dell'immobile.

## Dashboard e statistiche

Voglio filtri espliciti:

- Immobili in gestione.
- Immobili propri.
- Tutti gli immobili.

La vista predefinita deve evitare di mischiare il rendimento dei due modelli di business.

Non considerare i ricavi complessivi degli immobili propri come fatturato da commissioni di gestione Elledue.

Non mischiare nei KPI:

- compensi di property management;
- ricavi delle prenotazioni;
- risultati operativi degli immobili propri;
- saldi dei proprietari esterni.

Se mostri una vista complessiva, deve riportare categorie separate e chiaramente denominate.

Le impostazioni storiche dei vecchi immobili devono essere migrate senza cambiare arbitrariamente i loro calcoli. Se non esiste una precedente classificazione, assegna provvisoriamente la tipologia compatibile con il comportamento attuale, consentendo una revisione esplicita da parte mia.

Non creare automaticamente percentuali contrattuali per gli immobili propri.

---

# 7. CALENDARIO — RESTYLING MIRATO DEGLI EVENTI

L'attuale calendario non rispecchia il concept grafico approvato.

Oggi compare una sorta di stanghetta verticale con sotto una sigla come `A1` quando c'è un check-in.

Non mi piace e rende il calendario troppo tecnico e poco elegante.

Voglio una rappresentazione decisamente migliore degli eventi, attraverso barre cromatiche raffinate e coerenti con la grafica premium di Elledue Control.

## Direzione UX

Realizza elementi visivi che diano realmente colore al calendario, senza trasformarlo in un'agenda caotica.

Ad esempio:

- piccole barre orizzontali;
- pill compatte;
- bande colorate all'interno della giornata;
- intervalli di soggiorno visivamente leggibili;
- differenziazione fra check-in, check-out e attività.

I colori devono indicare informazioni utili.

Valuta una codifica coerente fra:

- prenotazione/soggiorno;
- check-in;
- check-out;
- pulizia;
- attività generica;
- evento completato o da svolgere.

Non affidarti soltanto al colore: utilizza, dove necessario, etichette o indicatori accessibili.

Le sigle identificative degli appartamenti devono rimanere riconoscibili, ma non diventare l'elemento visivo dominante.

Gestisci:

- più immobili;
- più eventi nello stesso giorno;
- check-out e check-in nella stessa giornata;
- sovrapposizioni;
- attività;
- giorni senza eventi;
- schermi stretti;
- testo ingrandito.

Mantieni uno stile compatto, elegante e intuitivo, vicino al concept precedentemente approvato.

Non sostituire questa richiesta con una banale modifica del colore della vecchia stanghetta.

---

# 8. PRENOTAZIONI NASCOSTE O ESCLUSE DAL RENDICONTO

Voglio poter decidere che una specifica prenotazione non venga mostrata nel report.

È importante distinguere due operazioni differenti.

### Opzione A — Non mostrare il dettaglio nel PDF

La prenotazione non compare come riga nominativa nel documento condiviso, ma i suoi importi continuano a essere considerati negli aggregati economici applicabili.

Il rendiconto deve indicare in modo comprensibile che esistono importi aggregati non dettagliati.

### Opzione B — Escludi dal rendiconto

La prenotazione viene esclusa dal calcolo del rendiconto selezionato.

In questo caso tutti i totali direttamente collegati alla prenotazione devono essere ricalcolati coerentemente.

Non devono rimanere commissioni, rimborsi o importi collegati conteggiati impropriamente.

La prenotazione deve comunque rimanere nel calendario e nello storico operativo.

L'esclusione dal rendiconto non equivale a cancellare la prenotazione dal database e non deve modificare la registrazione effettiva di tasse incassate o riversate.

## UX

Nel dettaglio prenotazione, oppure nella preparazione del rendiconto, prevedi un controllo chiaro.

Mostra l'effetto dell'operazione prima di confermarla.

Deve essere possibile ripristinare l'inclusione.

Se il report è già confermato, applica il flusso di rettifica/versionamento: non modificare silenziosamente il PDF storico.

Non utilizzare la stessa variabile booleana per nascondere una riga ed escludere una prenotazione dai calcoli.

---

# 9. TASSA DI SOGGIORNO — CALCOLO AUTOMATICO DA CORREGGERE

Durante l'uso ho rilevato che la tassa di soggiorno non sembra calcolarsi autonomamente come dovrebbe.

Voglio che il sistema proponga automaticamente l'importo previsto per ciascuna prenotazione.

Non devo reinserire ogni volta tutti i dati economici già disponibili nell'app.

## Comportamento richiesto

Il calcolo deve utilizzare i parametri configurati per l'immobile e i dati effettivi della prenotazione.

Prevedi:

- tariffa configurabile per persona/notte;
- numero degli ospiti soggetti;
- eventuali fasce di esenzione configurate;
- eventuale limite massimo di notti tassabili;
- soggiorno effettivo;
- eventuali esenzioni o rettifiche esplicite.

Non inserire aliquote o esenzioni universali inventate: le regole possono variare per Comune e periodo. Mantieni parametri espliciti e modificabili secondo la configurazione applicabile.

Se i dati di età/esenzione non sono disponibili, richiedi solo le informazioni necessarie, senza inventare il numero degli ospiti soggetti.

### Esempio puramente matematico

Tariffa configurata: 3 €/persona/notte.

Ospiti soggetti: 2.

Notti tassabili: 4.

Tassa prevista: 24 €.

Se modifico durata, ospiti soggetti o impostazioni applicabili, la previsione deve aggiornarsi correttamente finché non viene consolidata.

Distingui sempre:

- tassa prevista;
- tassa effettivamente riscossa;
- residuo;
- eventuale compensazione;
- riversamento.

Il calcolo automatico non deve registrare un incasso automatico.

Non modificare silenziosamente movimenti già confermati.

Verifica anche il trattamento dei soggiorni a cavallo mese e dei dati modificati successivamente.

---

# 10. ECONOMIA — SEMPLIFICAZIONE DELL'ESPERIENZA

La sezione Economia è ancora troppo poco intuitiva.

Non voglio limitarmi a rinominare qualche pulsante: vorrei che l'intera esperienza fosse più semplice, senza impoverire le funzionalità contabili.

Riprogetta in modo mirato l'organizzazione dell'area Economia, mantenendo il design premium già approvato.

## Obiettivo

Aprendo Economia devo poter comprendere immediatamente:

- quali somme mi spettano;
- quali somme spettano ai proprietari, per gli immobili in gestione;
- quali spese ho sostenuto;
- quali importi devono essere rimborsati;
- la situazione delle tasse di soggiorno;
- quali operazioni devo ancora registrare;
- quali rendiconti sono da preparare o confermare.

Per gli immobili propri, il riepilogo deve invece seguire la relativa logica economica, senza inventare commissioni di gestione o saldi con proprietari esterni.

## Architettura suggerita

Valuta una struttura chiara, per esempio:

- Panoramica.
- Movimenti.
- Rendiconti.

All'interno, utilizza azioni rapide contestuali e componenti ordinati.

Non creare sottosezioni o pulsanti che obblighino a ricordare dove si trovano le operazioni.

### Operazioni chiaramente distinte

- Registra spesa.
- Registra pulizia.
- Registra lavanderia.
- Registra incasso.
- Registra pagamento/anticipo.
- Gestisci tassa di soggiorno.
- Registra compensazione.
- Registra riversamento.
- Genera rendiconto.

Il linguaggio deve distinguere nettamente previsioni, registrazioni effettive e saldi.

Evita doppie compilazioni: una registrazione deve alimentare automaticamente tutte le sezioni pertinenti.

Mantieni la possibilità di accedere al dettaglio completo e allo storico delle rettifiche, senza mostrare necessariamente tutta la complessità contabile nella prima schermata.

Le viste devono rispettare i filtri sugli immobili attivi, archiviati e propri.

---

# 11. RENDICONTI — TOGLIERE LA DICITURA «STIMATA» DALLA COMMISSIONE

Nel rendiconto, sulla voce relativa alla commissione di gestione, compare la parola «stimata».

Voglio rimuoverla.

Utilizza una denominazione professionale, per esempio:

**Compenso di gestione Elledue**

oppure:

**Commissione di gestione Elledue**

La dicitura deve essere uniforme nell'interfaccia e nel PDF.

Questo è un cambiamento di presentazione, non un'autorizzazione a spacciare per definitivo un importo non verificato.

Se il valore dipende da dati ancora provvisori, gestisci la situazione nella bozza o con un avviso appropriato prima della conferma, invece di inserire automaticamente «stimata» nel nome della voce.

Non modificare le formule economiche corrette soltanto per cambiare l'etichetta.

---

# 12. ANALISI E GRAFICI — MIGLIORAMENTO ARTISTICO

I grafici attuali possono essere molto migliori.

Vorrei una sezione più curata, contemporanea, interessante e leggermente più colorata, sempre coerente con l'identità di Elledue Control.

Non voglio colori casuali o un'interfaccia da videogioco.

Utilizza i colori per migliorare:

- interpretazione;
- distinzione delle categorie;
- confronto temporale;
- differenza fra ricavi, costi e compensi;
- lettura dei risultati.

Valuta:

- grafici più raffinati;
- indicatori visuali;
- confronti mensili;
- barre e linee con trattamento grafico premium;
- distribuzione delle spese;
- evoluzione delle performance.

Mantieni etichette, unità e legende leggibili.

Distingui i risultati degli immobili propri da quelli in gestione. Non sommare metriche economicamente non confrontabili sotto un'etichetta generica.

Non inventare statistiche per riempire lo schermo.

Migliora anche gli stati vuoti.

---

# 13. NOTIFICHE ANDROID — IMPLEMENTAZIONE REALE

Le notifiche non sono ancora implementate come funzionalità operativa completa.

Ora voglio introdurle realmente.

Non è sufficiente inserire un'icona campanella o una schermata con impostazioni non funzionanti.

## Notifiche previste

Prevedi inizialmente:

- check-in imminenti;
- check-out imminenti;
- attività programmate;
- eventuali pulizie programmate già presenti;
- promemoria operativi pertinenti alle prenotazioni.

## Configurazione

Devo poter scegliere:

- quali categorie attivare;
- anticipo del promemoria, per esempio il giorno prima o alcune ore prima, ove appropriato;
- eventuale orario personalizzato;
- disattivazione di un promemoria;
- eventuale fascia di silenzio.

## Requisiti tecnici

Utilizza il sistema di notifiche Android reale.

Gestisci correttamente:

- permessi richiesti dalle versioni Android pertinenti;
- Notification Channels;
- modifiche a prenotazioni e attività;
- cancellazione o annullamento di eventi;
- mancata duplicazione degli avvisi;
- riavvio del dispositivo;
- cambiamento di data/ora;
- fuso orario e ora legale;
- comportamento quando il dispositivo limita le attività in background;
- compatibilità con i vincoli Android sulle notifiche programmate e sugli allarmi esatti.

Non usare servizi cloud o abbonamenti esterni se non sono necessari.

Una notifica relativa a una prenotazione modificata deve essere aggiornata, non lasciare in circolazione il vecchio promemoria.

Le notifiche devono aprire la schermata pertinente.

Non generare notifiche inutili per attività prive di una programmazione adeguata.

Documenta chiaramente eventuali restrizioni dipendenti dal sistema operativo o dai permessi concessi dall'utente.

---

# 14. ACCESSO CON IMPRONTA DIGITALE O PIN

Vorrei proteggere l'accesso a Elledue Control, considerato che contiene dati economici e informazioni personali degli ospiti.

Implementa una funzione di blocco dell'app facoltativa e configurabile.

## Modalità

Se il dispositivo supporta l'autenticazione biometrica:

- utilizza le API ufficiali Android, come BiometricPrompt;
- consenti l'accesso tramite impronta digitale o altro metodo biometrico supportato.

Prevedi un'alternativa tramite credenziale del dispositivo e/o PIN applicativo, secondo una soluzione progettata in modo sicuro.

Non memorizzare impronte digitali né creare sistemi biometrici proprietari.

Non conservare password o PIN in chiaro.

Utilizza meccanismi di protezione e conservazione delle credenziali appropriati alla piattaforma.

## Esperienza utente

L'opzione deve essere attivabile dalle impostazioni.

Prevedi:

- attiva/disattiva protezione;
- metodo preferito;
- autenticazione all'apertura;
- eventuale blocco dopo un periodo configurabile di inattività;
- gestione del fallimento biometrico;
- percorso di recupero sicuro;
- comportamento corretto quando la biometria non è disponibile.

Non introdurre un meccanismo per cui un semplice errore o il mancato riconoscimento dell'impronta provochi la perdita dei dati.

La schermata di blocco deve essere coerente con il design Elledue.

Distingui la protezione dell'accesso dall'eventuale cifratura dei backup: non dichiarare cifrati file che non lo sono realmente.

---

# 15. REQUISITI DI QUALITÀ TRASVERSALI

Durante questa milestone applica alcuni principi generali:

1. Un dato deve avere una sorgente affidabile e unica.
2. Le modifiche devono aggiornare tutte le viste collegate.
3. I movimenti economici devono rimanere coerenti.
4. Nessuna operazione irreversibile deve avvenire senza conferma.
5. Nessuna schermata deve mostrare informazioni eliminate o impropriamente archiviate.
6. La compilazione dei moduli non deve andare perduta a causa di un'interruzione.
7. Le funzioni nuove non devono compromettere quelle già esistenti.
8. Non introdurre regressioni nel design 0.4.6/0.4.7.
9. Non effettuare migrazioni Room distruttive.
10. Nessuna funzione deve essere dichiarata completata soltanto perché esiste nella UI.

Evita nuovi sistemi paralleli se è possibile estendere correttamente quelli già presenti.

---

# 16. COLLAUDI OBBLIGATORI

Voglio test specifici, non soltanto una generica dichiarazione di build riuscita.

### Persistenza prenotazioni

- Creazione.
- Modifica ripetuta.
- Riavvio dell'app.
- Verifica dei nuovi dati nel database.
- Verifica di calendario, economia e rendiconto.
- Nessuna duplicazione.

### Bozze

- Interruzione durante compilazione.
- Passaggio in background.
- Ricreazione Activity.
- Terminazione del processo e successiva riapertura.
- Ripristino corretto.
- Eliminazione corretta della bozza dopo salvataggio.
- Nessuna contaminazione tra moduli.

### Eliminazioni

- Prenotazione semplice.
- Prenotazione con movimenti collegati.
- Immobile senza prenotazioni.
- Immobile con storico.
- Immobile archiviato.
- Immobile eliminato.
- Aggiornamento di tutti i filtri e aggregati.
- Nessun record orfano.
- Fallimento simulato durante l'operazione: rollback corretto.

### Immobili propri

- Gestito e proprio con prenotazioni.
- Compenso Elledue solo dove applicabile.
- Nessuna percentuale inventata per la comproprietà.
- Statistiche separate.
- Rendiconti coerenti.
- Migrazione corretta di dati esistenti.

### Tassa di soggiorno

- Ospiti soggetti ed esenti.
- Soggiorno modificato.
- Limite notti configurato.
- Incasso parziale.
- Nessun incasso registrato automaticamente.
- Nessuna doppia sottrazione.
- Modifica di parametri successiva a movimenti confermati.

### Rendiconti

- Prenotazione nascosta dal dettaglio.
- Prenotazione esclusa dal calcolo.
- Ripristino inclusione.
- Totali coerenti.
- PDF aggiornato.
- Report già confermato protetto da alterazioni silenziose.
- Dicitura commissione corretta.

### Notifiche

- Permesso concesso e negato.
- Programmazione.
- Modifica evento.
- Annullamento evento.
- Nessun duplicato.
- Apertura della schermata corretta.
- Comportamento dopo riavvio, nei limiti dell'ambiente disponibile.

### Sicurezza

- Biometria disponibile.
- Biometria non disponibile.
- Fallimento autenticazione.
- Fallback.
- PIN o credenziale.
- Disattivazione tramite autenticazione.
- Nessun dato perso.

### Grafica

Acquisisci screenshot reali delle schermate modificate, includendo almeno:

- calendario;
- Economia;
- Analisi;
- gestione immobile proprio;
- cancellazione;
- prenotazione e rendiconto;
- impostazioni notifiche;
- schermata di accesso protetto.

Esegui test su più dimensioni dello schermo e con testo ingrandito.

---

# 17. ORGANIZZAZIONE DELLO SVILUPPO

Questa milestone è ampia e comprende interventi sensibili al database.

Puoi organizzarla in checkpoint tecnici interni, purché facciano tutti parte della consegna richiesta:

**Checkpoint A — Integrità**
Salvataggio prenotazioni, bozze, cancellazioni e migrazioni.

**Checkpoint B — Modello gestionale**
Immobili propri, contabilità, tassa e rendiconti.

**Checkpoint C — Esperienza operativa**
Calendario, Economia, Analisi, notifiche e autenticazione.

**Checkpoint D — QA e consegna**
Test completi, screenshot, APK e istruzioni.

Non sacrificare l'integrità contabile per completare prima una funzionalità estetica.

Se un problema non può essere risolto correttamente, documentalo senza nasconderlo e senza presentare la milestone come interamente completata.

---

# 18. CONSEGNA E APK SCARICABILE — IMPORTANTE

Voglio evitare il problema riscontrato con la 0.4.7, quando il file APK risultava compilato ma non facilmente scaricabile.

Al termine devi consegnare:

1. Branch dedicato senza merge.
2. Commit identificabile.
3. Matrice dei requisiti.
4. Documentazione dei bug risolti e della loro causa.
5. Test eseguiti, con risultati effettivi.
6. Screenshot reali.
7. APK compilato, installabile e verificato.
8. VersionName e versionCode aggiornati coerentemente.
9. Application ID preservato.
10. Verifica della firma rispetto alla versione precedente.
11. Checksum SHA-256.
12. Procedura di aggiornamento senza disinstallazione.
13. Procedura di backup prima dell'aggiornamento.
14. Limiti e criticità eventualmente ancora presenti.

**Per la distribuzione dell'APK:**

Non presentare un semplice percorso `/workspace/artifacts/...` come se fosse un link GitHub pubblico o scaricabile da qualsiasi ambiente.

Se l'ambiente supporta allegati scaricabili, consegna il file reale.

Se è disponibile un caricamento autorizzato su GitHub, prepara una Release DRAFT con l'APK, senza pubblicarla e senza effettuare merge.

Se il caricamento GitHub è bloccato da un errore di rete o permessi, non dichiarare che sia riuscito. Fornisci il file tramite il meccanismo effettivamente supportato e segnala precisamente il limite.

Non creare link fittizi.

Se la firma del nuovo APK non permette l'aggiornamento dell'installazione precedente, non proporre la disinstallazione come soluzione automatica: analizza il problema e preserva i dati.

---

# OBIETTIVO FINALE

La versione 0.4.8 deve essere un passo concreto verso un prodotto che posso utilizzare quotidianamente.

Voglio:

- modificare una prenotazione ed essere certo che si salvi;
- non perdere dati quando interrompo una compilazione;
- scegliere liberamente fra archiviare, annullare ed eliminare davvero;
- non vedere immobili archiviati o eliminati nelle viste operative;
- separare economicamente immobili gestiti e immobili propri;
- avere un calendario elegante, leggibile e colorato;
- controllare quali prenotazioni compaiono nei rendiconti;
- ottenere calcoli automatici affidabili della tassa di soggiorno;
- avere una sezione Economia realmente intuitiva;
- visualizzare analisi migliori;
- ricevere notifiche Android funzionanti;
- proteggere l'accesso tramite biometria o credenziale alternativa.

Mantieni l'identità premium di Elledue Control e rendi l'app più solida, semplice e affidabile.

**Procedi concretamente con l'implementazione. Non fermarti alla sola analisi. Non effettuare merge o pubblicazioni senza autorizzazione.**