# CODEX — L2 CONTROL / ELLEDUE CONTROL
## Intervento completo: automazioni economiche, retroattività, UX e consegna Android

Agisci come **Lead Android Engineer, Product Engineer e responsabile QA** del progetto L2 Control / Elledue Control.

Non limitarti a proporre soluzioni teoriche: devi **analizzare il repository, implementare le modifiche, eseguire i test, correggere gli errori e consegnare una versione Android effettivamente installabile**.

## 0. Repository e situazione attuale

Repository:

https://github.com/alelanza01/Elledue-Control

Dalla precedente ricognizione risultava:

- `main`: versione 0.4.6, commit `c156458de9c7d90f3585a3f44a7ca23db540cb88`.
- `feature/0.4.9-consolidated`: sviluppo 0.4.9, commit `28f0e6c7591bad55ec5100f6458a2216f0036559`.
- `artifacts/v0.4.9-test`: APK di test della 0.4.9.

**Verifica nuovamente lo stato reale del repository prima di iniziare.** I riferimenti sopra descrivono l'ultima ricognizione, non autorizzano a presumere che GitHub sia rimasto invariato.

La base del nuovo intervento deve essere il codice più completo e aggiornato, preservando le funzionalità della 0.4.9. **Non partire automaticamente da `main` se questo comporta perdere le modifiche 0.4.7–0.4.9.**

Se opportuno, crea un nuovo branch di sviluppo, per esempio `feature/0.4.10-ux-accounting`, senza sovrascrivere né cancellare i branch precedenti.

### Vincoli inderogabili

1. Applicazione Android nativa in Kotlin, Jetpack Compose, Room/SQLite.
2. Application ID invariabile: `it.elledue.control`.
3. Android 8.0+.
4. Offline-first: nessun login, backend, cloud obbligatorio, tracking o permesso Internet introdotto.
5. Conservare le funzionalità esistenti, inclusi immobili, prenotazioni, contabilità, rendiconti, calendario, attività, interventi, notifiche, backup e impostazioni.
6. Non introdurre migrazioni distruttive.
7. Conservare rendiconti confermati, storici, rettifiche e movimenti economici.
8. Mantenere l'identità visiva premium già approvata: blu notte `#111B32`, oro `#DBBF8B`, componenti e tipografia coerenti.
9. Non creare dati dimostrativi o movimenti fittizi nel database reale.
10. Non dichiarare test fisici, compatibilità o consegne riuscite senza prove effettive.

---

# 1. Obiettivo generale

Il principale problema di L2 Control è che alcune operazioni standard richiedono ancora troppo lavoro manuale.

Voglio un'app **rapida, intuitiva, user-friendly e fortemente automatizzata**.

Quando inserisco una prenotazione, l'app conosce già:

- immobile;
- numero di ospiti;
- check-in;
- check-out;
- numero di notti;
- portale;
- importo della prenotazione;
- regole economiche dell'immobile.

Deve quindi essere capace di calcolare automaticamente tutto ciò che dipende da questi dati, senza costringermi a inserire nuovamente informazioni già disponibili.

**Principio UX: tutto ciò che è calcolabile deve essere calcolato; tutto ciò che è già noto deve essere precompilato; devo intervenire solo per correggere un'eccezione o confermare un evento realmente avvenuto.**

Non aggiungere schermate, campi o conferme se non sono strettamente necessari.

---

# 2. Lavanderia automatica

## Regola economica obbligatoria

Ogni set di lavanderia comprende **letto + asciugamani** e copre al massimo due ospiti.

Costo standard: **5,00 € per set**.

| Ospiti | Set | Lavanderia |
|---|---:|---:|
| 1 | 1 | 5 € |
| 2 | 1 | 5 € |
| 3 | 2 | 10 € |
| 4 | 2 | 10 € |
| 5 | 3 | 15 € |
| 6 | 3 | 15 € |
| 7 | 4 | 20 € |
| 8 | 4 | 20 € |
| 9 | 5 | 25 € |

Formula:

`numeroSet = ceil(numeroOspiti / 2)`

`costoLavanderia = numeroSet × 500 centesimi`

Utilizzare aritmetica intera ed evitare calcoli monetari in floating point.

## Comportamento richiesto

- Calcolo automatico alla creazione della prenotazione.
- Ricalcolo automatico quando cambia il numero degli ospiti.
- Visualizzazione immediata dell'importo nel riepilogo economico.
- Possibilità di modificare quantità, tariffa o importo in caso di eccezione.
- Conservazione degli override manuali intenzionali.
- Nessuna duplicazione della spesa.
- Configurabilità della tariffa e del numero di ospiti per set nelle impostazioni dell'immobile, mantenendo i valori standard sopra indicati.

### Retroattività obbligatoria

Applicare questa logica anche alle prenotazioni già presenti nel database, non soltanto a quelle create dopo l'aggiornamento.

Il ricalcolo deve avvenire attraverso una procedura di riconciliazione/backfill **idempotente**, eseguita automaticamente dopo l'aggiornamento ove necessario.

Non richiedere all'utente di aprire e salvare una per una tutte le vecchie prenotazioni.

Non sovrascrivere importi effettivi già registrati, rettifiche manuali o snapshot confermati.

---

# 3. Pulizie automatiche

Per gli **appartamenti in gestione**, utilizzare come impostazione standard:

- 2 ore per soggiorno;
- 8,00 € per ora;
- totale ordinario: **16,00 € per prenotazione**.

## Comportamento richiesto

- Calcolare automaticamente il costo alla creazione della prenotazione.
- Associarlo alla prenotazione corretta.
- Mostrare ore, tariffa e totale.
- Consentire la modifica rapida di ore, tariffa o importo.
- Utilizzare eventuali parametri personalizzati dell'immobile.
- Applicare i valori standard anche alle prenotazioni storiche eleggibili.
- Non generare nuove spese per prenotazioni annullate, salvo eventuali costi effettivi già sostenuti da conservare.
- Non applicare automaticamente una commissione di gestione a immobili di proprietà di Elledue.

Se esiste già una spesa di pulizia associata a un soggiorno, non crearne una seconda.

Le spese manuali e i consuntivi precedenti devono essere riconosciuti e rispettati.

---

# 4. Costi automatici realmente presenti nei rendiconti

**Questo è un problema fondamentale da risolvere.**

Nella 0.4.9 esiste già una logica di previsione dei costi (`StayCostRepository`), ma una previsione non confermata viene esclusa dalle spese effettive del rendiconto.

Il risultato pratico è che l'utente non trova automaticamente pulizie e lavanderia nel report e deve compiere ulteriori operazioni.

## Nuovo comportamento desiderato

Il rendiconto in bozza deve:

1. Recuperare tutte le prenotazioni pertinenti.
2. Calcolare automaticamente le relative pulizie e lavanderia standard.
3. Includere tali voci nel riepilogo e nel PDF della bozza, distinguendo chiaramente valori standard proposti e importi consuntivati.
4. Mostrare i costi per prenotazione e i totali distinti.
5. Consentire la modifica rapida delle eccezioni.
6. Non richiedere di registrare manualmente una spesa standard per ogni soggiorno.

### Definitività contabile

Non falsificare lo stato dei costi: un importo calcolato automaticamente non dimostra da solo che sia già stato pagato.

Progetta quindi un meccanismo semplice per **approvare in blocco i costi standard del rendiconto** quando si conferma la versione definitiva, senza dover aprire e confermare individualmente ogni prenotazione.

L'approvazione deve essere esplicita, identificabile e tracciabile. Le voci non ancora approvate devono essere etichettate correttamente nella bozza.

Alla conferma, i costi standard approvati devono entrare nei totali definitivi secondo le regole contabili dell'immobile.

Prevenire la duplicazione tra:

- previsione automatica;
- costo standard approvato;
- spesa manuale;
- eventuale rettifica successiva.

**Un soggiorno deve contribuire una sola volta al totale per ciascuna tipologia di costo.**

### Competenza tra mesi differenti

Verifica e risolvi il caso:

- Check-in: 29 novembre.
- Check-out: 3 dicembre.
- Prenotazione attribuita economicamente a novembre.
- Pulizia effettuata il 3 dicembre.

Il rendiconto deve rendere chiaro il legame tra soggiorno, costo e periodo di competenza, senza far scomparire la pulizia dal riepilogo pertinente né contabilizzarla due volte.

Conserva la data effettiva dell'intervento separatamente dalla competenza contabile. Non modificare implicitamente gli storici confermati.

---

# 5. Tassa di soggiorno automatica

## Regola standard

Per gli immobili configurati con questa tariffa:

- **3,00 € per ospite non esente, per notte**;
- massimo **4 notti imponibili per ciascun ospite**;
- dalla quinta notte in poi non si paga ulteriore tassa per lo stesso ospite.

Formula:

`imposta = ospitiNonEsenti × min(nottiSoggiorno, 4) × 300 centesimi`

Esempi:

- 2 ospiti, 2 notti, nessun esente: 12 €.
- 2 ospiti, 7 notti, nessun esente: 24 €.
- 4 ospiti, 5 notti, nessun esente: 48 €.
- 4 ospiti, 7 notti, 1 esente: 36 €.
- 5 ospiti, 10 notti, 2 esenti: 36 €.

Il limite si applica individualmente a ogni ospite, non al numero complessivo di notti-ospite.

Mantieni configurabili tariffa, soglie ed esenzioni per immobile: non imporre silenziosamente questa regola a immobili che abbiano un trattamento legittimamente diverso.

## Interfaccia richiesta

Quando inserisco una prenotazione:

- Tutti gli ospiti sono considerati non esenti per il calcolo standard.
- Se sono presenti esenti, devo poter indicare semplicemente **quanti ospiti sono esenti**.
- L'app ricava automaticamente il numero soggetto a tassa.
- L'importo è mostrato subito.
- Non devo compilare una seconda schermata fiscale per ottenere il calcolo ordinario.
- Il valore si aggiorna automaticamente se modifico ospiti, esenti o date.

Non introdurre obbligatoriamente l'inserimento delle date di nascita individuali.

## Retroattività

Ricalcola la tassa prevista anche per le prenotazioni storiche non consolidate, verificando parametri dell'immobile e dati disponibili.

Per le vecchie prenotazioni prive di informazioni attendibili sulle esenzioni, non inventare dati storici: utilizza i valori già registrati quando presenti e segnala eventuali casi ambigui in modo sintetico.

Non sovrascrivere versamenti reali, movimenti già riconciliati, imposte con rettifiche manuali o rendiconti confermati.

## Tassa prevista, riscossa e anticipi

Preserva una distinzione contabile indispensabile:

1. **Tassa prevista**: importo calcolato in base a ospiti e notti.
2. **Tassa effettivamente riscossa**: somma realmente ricevuta.
3. **Compensazione/anticipo riconosciuto**: movimento rilevante per il saldo con il proprietario.
4. **Riversamento**: eventuale successivo pagamento al soggetto competente.

Non registrare automaticamente come incassata una tassa semplicemente perché la prenotazione esiste.

Però il processo attuale è troppo complicato.

Implementa un'azione rapida dalla prenotazione, come **"Conferma tassa incassata"**, che proponga l'intero importo calcolato e consenta una cifra parziale.

Quando la riscossione avviene da parte di Elledue e il rapporto economico con il proprietario prevede la relativa compensazione, il sistema deve poter creare in **un'unica operazione atomica** i movimenti coerenti di riscossione e compensazione, senza che l'utente debba compilare due moduli separati.

Non creare automaticamente compensazioni se la riscossione è effettuata dal proprietario, dal portale o da un altro soggetto.

Prevedi adeguata gestione di riscossioni parziali, storni e rettifiche, senza duplicazioni.

Nel rendiconto devono essere immediatamente visibili:

- tassa calcolata;
- tassa riscossa;
- tassa da riscuotere;
- eventuale compensazione già riconosciuta.

La tassa resta fuori dal lordo della prenotazione e dalla base di calcolo del compenso di gestione.

---

# 6. Inserimento spese estremamente semplificato

Attualmente il form delle spese espone troppi elementi tecnici contemporaneamente.

Riprogetta la UX con un percorso rapido.

## Form base

I campi inizialmente visibili dovrebbero essere soltanto quelli essenziali:

- immobile;
- categoria;
- descrizione, se necessaria;
- importo;
- soggetto che ha pagato;
- data, precompilata;
- eventuale prenotazione collegata, già selezionata se si arriva da una prenotazione.

Aggiungi una sezione espandibile **"Altri dettagli"** per informazioni raramente necessarie:

- competenza;
- quantità e tariffa unitaria;
- note;
- rimborsabilità;
- rettifiche e altri attributi contabili.

Quando la categoria lo richiede, il form deve adattarsi automaticamente:

- pulizia → ore e tariffa;
- lavanderia → set e tariffa;
- altra spesa → importo semplice.

## Precompilazione intelligente

Se entro dalla scheda di una prenotazione:

- selezionare automaticamente l'immobile;
- collegare automaticamente la prenotazione;
- utilizzare la data pertinente;
- proporre correttamente anticipata/rimborsabile in base alla tipologia dell'immobile e alla configurazione esistente.

Evitare campi ridondanti o duplicati.

L'utente deve poter inserire una spesa ordinaria in pochi tocchi.

**Le impostazioni predefinite non devono impedire di registrare eccezioni.**

---

# 7. Nuova prenotazione: un solo calendario per check-in e check-out

Nell'attuale interfaccia esiste un pulsante aggiuntivo per scegliere l'intervallo di date, oltre ai campi check-in e check-out.

Lo considero ridondante.

## Comportamento richiesto

- Mostrare chiaramente il campo check-in e il campo check-out.
- Toccando il check-in, aprire direttamente il selettore dell'intero soggiorno.
- Selezionare la prima data per il check-in e la seconda per il check-out.
- Toccando il check-out, poter riaprire lo stesso selettore per modificare l'intervallo.
- **Eliminare il terzo pulsante separato** che apre il selettore delle date.
- Mostrare immediatamente il numero delle notti.
- Aggiornare automaticamente la tassa e gli altri valori dipendenti.
- Impedire intervalli non validi.
- Conservare il controllo sulle sovrapposizioni delle prenotazioni.

Non peggiorare l'accessibilità o la gestione degli errori.

Verifica inoltre che la tastiera e i selettori non coprano il pulsante di salvataggio sui telefoni piccoli.

---

# 8. Calendario mensile compatto

L'attuale calendario è troppo alto: per visualizzare tutto il mese bisogna scorrere verticalmente in modo eccessivo.

Nel codice 0.4.9, ogni settimana può accumulare molte righe di soggiorni e attività sotto le date.

## Nuova struttura

Separare due livelli:

### Vista mese

- Griglia compatta di 5 o 6 settimane.
- Date leggibili e facilmente selezionabili.
- Indicatori sintetici di arrivi, partenze, soggiorni o attività.
- Colori immobili coerenti.
- Nessuna ripetizione estesa del nome dell'immobile e dell'ospite per ogni settimana.
- Altezza complessiva ragionevole su uno smartphone.
- Evidenza per il giorno corrente e quello selezionato.

### Giornata operativa

Toccando una data:

- mostrare check-in;
- check-out;
- soggiorni presenti;
- pulizie;
- attività;
- interventi programmati.

Visualizzare gli orari quando sono realmente disponibili, senza inventarli.

Mantenere la possibilità di aprire prenotazioni, attività e interventi.

### Funzioni da conservare

Non perdere:

- filtro per immobile;
- vista Agenda;
- filtro delle prenotazioni annullate;
- copia calendario;
- navigazione tra mesi;
- ritorno a oggi;
- giornata operativa.

La nuova soluzione deve essere utilizzabile anche su schermi di larghezza 320 dp e con caratteri ingranditi.

---

# 9. Riepilogo economico immediato della prenotazione

Una volta salvata una prenotazione, voglio trovare un riepilogo leggibile che mostri:

**Soggiorno**
- numero ospiti;
- notti;
- check-in/check-out;
- portale.

**Ricavi**
- lordo;
- commissione OTA;
- netto dopo OTA;
- compenso Elledue, secondo gli accordi dell'immobile.

**Costi**
- pulizie standard;
- lavanderia standard;
- altre spese collegate.

**Tassa**
- prevista;
- riscossa;
- ancora da riscuotere;
- eventuale compensazione.

Distingui importi automatici, consuntivi e movimenti realmente registrati attraverso etichette comprensibili.

Non creare un'altra schermata lunga e complessa: usa sezioni compatte con dettagli espandibili.

Da questo riepilogo devono essere disponibili azioni rapide per:

- registrare una spesa;
- modificare pulizie/lavanderia;
- confermare tassa incassata;
- aprire il rendiconto pertinente, quando disponibile.

---

# 10. Rendiconti sempre coerenti e aggiornati

Rivedi il processo di generazione e aggiornamento delle bozze.

Attualmente esiste un comando per aggiornare la bozza dai dati sorgente. Verifica se questo può causare la visualizzazione di importi obsoleti quando l'utente modifica prenotazioni o spese.

Comportamento desiderato:

- Una bozza deve mostrare automaticamente i dati pertinenti più recenti.
- Non deve essere necessario ricordarsi di premere un comando di ricalcolo.
- Devono essere visibili pulizie e lavanderia in sezioni distinte.
- La tassa di soggiorno deve comparire con il suo stato corretto.
- Le spese devono contribuire ai totali una sola volta.
- Il PDF riepilogativo deve mostrare importi chiari, riconciliabili con quelli dell'app.
- Il PDF dettagliato deve conservare il dettaglio delle voci.
- Nessun importo deve scomparire silenziosamente a causa del mese di competenza.
- Eventuali informazioni mancanti devono produrre avvisi precisi e comprensibili.

Le versioni già confermate rimangono **immutabili**.

Se una modifica retroattiva interessa un rendiconto confermato, consentire una versione rettificativa, senza modificare il documento storico.

---

# 11. Retroattività su tutto il database

Questa richiesta è prioritaria.

Il nuovo comportamento deve funzionare:

- per prenotazioni create dopo l'aggiornamento;
- per prenotazioni esistenti della 0.4.9;
- per prenotazioni provenienti dalla 0.4.6 e dalle versioni intermedie;
- per vecchie prenotazioni senza costi standard inseriti;
- per prenotazioni con alcune spese manuali già presenti.

## Requisiti tecnici

Realizzare una riconciliazione una tantum, sicura, transazionale quando necessario e idempotente.

Classificare correttamente:

1. Voci mancanti → generare la corrispondente previsione standard.
2. Previsioni automatiche esistenti → ricalcolare se necessario.
3. Override manuali → preservare.
4. Spese consuntivate → preservare.
5. Movimenti fiscali già registrati → preservare.
6. Prenotazioni annullate → trattare senza creare falsi costi o incassi.
7. Rendiconti confermati → non alterare.
8. Bozze → aggiornare attraverso le regole correnti.

Non basarsi soltanto su importi o descrizioni per identificare i duplicati: usare collegamenti stabili, categorie, prenotazioni e identificatori di origine.

Non eseguire il backfill in modo ripetuto creando nuove voci a ogni avvio.

Se una situazione storica è ambigua, conservarla e segnalarla; non sovrascrivere in modo distruttivo.

---

# 12. Home e navigazione più operative

Senza stravolgere la grafica approvata, valuta una rifinitura della Home per ridurre il lavoro quotidiano.

Dare priorità a:

- prossimi check-in;
- prossimi check-out;
- attività da completare;
- pulizie e lavanderia con eccezioni da verificare;
- tasse ancora da riscuotere;
- problemi aperti;
- rendiconti in sospeso.

I dati devono essere reali, non mock.

Ogni elemento deve essere cliccabile e aprire direttamente il contenuto pertinente.

Il pulsante `+` deve consentire di creare rapidamente una prenotazione, una spesa, un'attività o un intervento, senza passaggi inutili.

Non sostituire la Home premium esistente con un'interfaccia generica. Preservare la coerenza visiva dell'app.

---

# 13. Verifiche UX obbligatorie

Per ogni flusso principale devi verificare:

- numero di tocchi necessari;
- duplicazione dei campi;
- selezioni che possono essere precompilate;
- stati vuoti;
- errori di validazione;
- facilità di correzione;
- scroll eccessivo;
- comportamento della tastiera;
- funzionamento su smartphone piccoli;
- feedback visibile dopo un salvataggio;
- preservazione delle bozze quando si esce da una schermata.

Evita dialoghi di conferma inutili.

Conserva le conferme necessarie a proteggere dati, movimenti economici e azioni irreversibili.

---

# 14. Test obbligatori

Non considerare completato il lavoro soltanto perché il progetto compila.

Aggiungi test nuovi senza eliminare o disabilitare quelli precedenti.

## Calcoli lavanderia

Verificare almeno tutti i casi da 1 a 10 ospiti.

Verificare anche:

- modifica del numero di ospiti;
- tariffa personalizzata;
- override manuale;
- prenotazione cancellata;
- prenotazione storica;
- assenza di duplicati dopo doppia riconciliazione.

## Tassa di soggiorno

Verificare:

- 1, 2, 3, 4, 5, 7 e 10 notti;
- 1, 2, 3, 4 e 5 ospiti;
- esenzioni parziali;
- tutti gli ospiti esenti;
- modifica delle date;
- modifica degli ospiti;
- limite di 4 notti applicato a ogni ospite;
- diverse tariffe per immobile;
- riscossione parziale;
- compensazione;
- storni;
- assenza di incassi automatici fittizi.

## Rendiconti

Verificare:

- prenotazione con pulizie e lavanderia automatiche;
- più prenotazioni nello stesso mese;
- soggiorno che attraversa due mesi;
- spesa manuale già esistente;
- rettifica di un costo;
- nessuna duplicazione;
- correttezza del PDF riepilogativo;
- correttezza del PDF dettagliato;
- immutabilità dei rendiconti confermati;
- creazione di versioni rettificative.

## Retroattività e database

Verificare migrazioni da tutte le versioni supportate, in particolare dalle versioni installabili precedenti alla 0.4.9.

Testare esplicitamente:

- database con prenotazioni storiche;
- database con importi personalizzati;
- database con rendiconti confermati;
- backup/restore;
- ripetizione della procedura di backfill;
- riapertura dell'app dopo aggiornamento;
- conservazione dei dati dopo modifica delle impostazioni.

## Interfaccia

Verificare:

- selettore unico delle date;
- assenza del terzo pulsante;
- calendario compatto;
- form rapido delle spese;
- accessibilità a 320 dp;
- caratteri ingranditi;
- navigazione e salvataggi;
- assenza di regressioni nelle schermate esistenti.

Fornire screenshot reali del rendering ottenuto nei test, con dati esclusivamente sintetici.

Distingui espressamente test JVM/Robolectric da prove su telefono fisico.

---

# 15. Aggiornamento e consegna APK — OBBLIGATORIO

In precedenza alcune modifiche erano presenti nel codice, ma l'utente continuava a non riceverle nell'app installata.

Questo problema non deve ripetersi.

## Requisiti di consegna

1. Incrementare `versionName` e `versionCode` rispetto alla versione precedente, senza riutilizzare un codice versione esistente.
2. Generare realmente un APK Android installabile.
3. Verificare application ID, versione, dimensioni e SHA-256 dell'APK.
4. Verificare la firma e la compatibilità con gli APK di test precedenti.
5. Pubblicare l'APK in una posizione GitHub effettivamente accessibile all'utente, preferibilmente un artifact o una release di test verificata.
6. Non considerare sufficiente la sola presenza del codice su un branch.
7. Fornire un **link diretto realmente verificato** per scaricare l'APK.
8. Fornire istruzioni per installare l'aggiornamento **sopra l'app esistente, senza disinstallarla**.
9. Prescrivere l'esportazione preventiva di un backup.
10. Se la firma impedisce l'aggiornamento sul posto, segnalarlo chiaramente: non consigliare disinstallazioni distruttive.
11. Verificare che nel repository e nell'APK non siano inclusi keystore privati, credenziali, backup reali o dati personali.

Non inventare un link, non dichiarare una build riuscita senza il relativo artefatto, non affermare di avere eseguito test su un dispositivo non disponibile.

Predisponi una PR chiara e verificabile per la revisione.

**Non effettuare automaticamente il merge su `main` senza autorizzazione dell'utente.**

---

# 16. Organizzazione del lavoro

L'intervento è articolato in quattro blocchi funzionali:

**Blocco A — Stabilità e consegna**
- Base di sviluppo corretta.
- Compatibilità delle migrazioni.
- Processo di build e consegna dell'APK.

**Blocco B — Motore economico e retroattività**
- Tassa di soggiorno.
- Pulizie.
- Lavanderia.
- Backfill storico.
- Contabilità, spese e rendiconti.

**Blocco C — UX operativa**
- Inserimento rapido spese.
- Nuovo selettore check-in/check-out.
- Calendario compatto.
- Riepilogo prenotazione.
- Azioni rapide e Home.

**Blocco D — QA e consegna**
- Test automatici.
- Migrazioni.
- Prove visive.
- PDF dimostrativi.
- Build APK.
- Istruzioni di aggiornamento.

Procedi autonomamente all'implementazione, senza fermarti dopo la sola analisi.

Se un dettaglio non è definito, scegli la soluzione più coerente con il comportamento esistente e con i principi sopra indicati, documentandola.

Non modificare silenziosamente regole contabili sensibili o dati storici consolidati.

---

# 17. Criteri di accettazione finali

Considererò il lavoro completato soltanto se:

- [ ] La lavanderia viene calcolata automaticamente in base al numero di ospiti.
- [ ] Le pulizie degli immobili gestiti utilizzano il valore standard di 16 €.
- [ ] La tassa di soggiorno viene calcolata automaticamente in base a ospiti, esenti e massimo quattro notti imponibili per persona.
- [ ] I valori automatici sono visibili nelle prenotazioni.
- [ ] I costi standard compaiono correttamente nelle bozze di rendiconto e nei PDF.
- [ ] È possibile approvare i costi standard senza conferme individuali per ogni soggiorno.
- [ ] Le riscossioni reali della tassa e le compensazioni sono registrabili con un'operazione semplice.
- [ ] Le modifiche si applicano anche alle prenotazioni pregresse eleggibili.
- [ ] Non vengono duplicati costi o movimenti.
- [ ] Gli importi manuali e i rendiconti confermati vengono preservati.
- [ ] Il form delle spese è sensibilmente più semplice.
- [ ] Il calendario permette di vedere il mese in una forma realmente compatta.
- [ ] Check-in e check-out si selezionano da un unico calendario senza terzo pulsante.
- [ ] La grafica premium esistente viene mantenuta.
- [ ] Il database e i backup restano compatibili.
- [ ] Tutti i test eseguiti sono documentati correttamente.
- [ ] Viene consegnato un APK aggiornato con link diretto verificato.

---

# 18. Risposta finale richiesta a Codex

Al termine, restituisci un resoconto strutturato con:

1. **Versione realizzata**, branch e commit.
2. **Funzionalità implementate**, distinguendo nuove logiche, correzioni e modifiche UX.
3. **Retroattività**, spiegando esattamente come vengono trattate le prenotazioni esistenti.
4. **Regole contabili**, indicando cosa viene calcolato, cosa viene proposto e cosa richiede una conferma effettiva.
5. **Database**, con schema e migrazioni.
6. **Test**, riportando comandi realmente eseguiti, PASS/FAIL, problemi residui e limiti delle verifiche.
7. **Screenshot**, per calendario, nuova prenotazione, spese e rendiconti.
8. **APK**, con link diretto, SHA-256, versione, firma e istruzioni d'installazione.
9. **Problemi rimasti**, senza dichiarare completo ciò che non lo è.
10. **PR**, pronta per revisione, senza merge automatico su `main`.

L'obiettivo non è una versione con più pulsanti o più configurazioni: è una versione che, nella gestione quotidiana degli affitti brevi, richieda **meno tempo, meno inserimenti manuali e meno possibilità di errore**, mantenendo affidabilità contabile e integrità dei dati.

Implementa, verifica e consegna.