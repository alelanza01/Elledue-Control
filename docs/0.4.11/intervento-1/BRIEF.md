# CODEX — L2 Control — Intervento 1/3
## Revisione completa del motore economico e dei rendiconti

Agisci come responsabile dello sviluppo Android e della correttezza contabile di L2 Control.

Repository: `alelanza01/Elledue-Control`.

### Base di lavoro

L'ultima versione verificata è la 0.4.10, branch `feature/0.4.10-ux-accounting`, commit `186a80e2d49b`.

Verifica lo stato attuale del repository. Se non sono intervenute nuove versioni, crea `feature/0.4.11-user-feedback` partendo dalla 0.4.10, NON da `main`, ancora fermo alla 0.4.6.

Questo è il primo di tre interventi successivi. Mantieni il branch disponibile per i due prompt seguenti. Non effettuare merge automatici.

### Principio generale

Il rendiconto deve essere comprensibile a un proprietario e utilizzabile quotidianamente senza conoscenze contabili avanzate.

Non aggiungere campi e approvazioni inutili. Non nascondere errori contabili dietro semplificazioni grafiche.

## A. Nuova gestione delle commissioni OTA con IVA

Il proprietario riceve il lordo al netto dei costi OTA secondo le regole economiche dell'operazione. Il compenso di gestione Elledue viene calcolato sulla base convenuta con l'immobile.

Per gli immobili configurati secondo il nuovo criterio, il costo OTA da sottrarre deve comprendere anche il 22% di IVA sulla commissione imponibile.

Esempio obbligatorio:

- Lordo: €1.000,00.
- Commissione OTA base: 19,5% = €195,00.
- IVA 22% sulla commissione: €42,90.
- Costo OTA complessivo: €237,90.
- Netto rilevante per la gestione: €762,10.
- Compenso Elledue: percentuale contrattuale dell'immobile applicata a €762,10.

Il 22% NON si applica all'intero lordo.

### Requisiti di progettazione

Analizza `BookingFinance`, `BookingDraft`, entità Room, repository prenotazioni, report, PDF e analytics.

Introduci un trattamento coerente del costo OTA, distinguendo almeno:

1. Commissione base senza IVA, con maggiorazione IVA del 22%.
2. Commissione effettiva già comprensiva di IVA.
3. Commissione senza maggiorazione nel costo economico del rendiconto, quando la configurazione del caso lo richiede.
4. Prenotazione diretta, senza commissione OTA salvo eccezioni configurate.

Non applicare il 22% due volte.

Il trattamento deve essere configurabile per immobile/portale e correggibile sulla singola prenotazione, senza trasformare il normale inserimento di una prenotazione in una procedura complessa.

Preserva la distinzione tra commissione OTA percentuale e importo effettivo inserito manualmente.

Se l'importo effettivo proviene già da una fattura comprensiva d'IVA, non aggiungere nuovamente l'IVA.

Preserva i parametri economici storici negli snapshot; adegua retroattivamente le sole prenotazioni modificabili e non consolidate, con procedure idempotenti. Gli importi di prenotazioni già rendicontate non devono essere riscritti senza una versione rettificativa.

Usa centesimi interi, arrotondamento documentato e un unico motore di calcolo per tutta l'app.

## B. Tassa di soggiorno interamente automatica

Elimina l'obbligo di confermare manualmente la tassa di soggiorno per ogni prenotazione.

L'utente inserisce soltanto:

- numero degli ospiti;
- date del soggiorno;
- eventuale numero di ospiti esenti.

Il sistema deve calcolare automaticamente:

`ospiti non esenti × min(notti, 4) × €3`

Per gli immobili che utilizzano questa regola, il limite è quattro notti per ciascun ospite.

Mantieni configurazioni differenti ove presenti.

### Nuovo trattamento nel saldo proprietario

Per gli immobili in gestione, l'importo ordinario della tassa di soggiorno deve essere considerato automaticamente come **anticipo in compensazione**, secondo la regola operativa richiesta.

Non imporre ulteriori schermate di approvazione, pulsanti "Conferma riscossione", checkbox fiscali o registrazioni manuali per l'operazione standard.

Nel saldo con il proprietario l'anticipo automatico deve essere sottratto una sola volta.

La regola è un riconoscimento economico convenzionale nel rendiconto, NON una prova automatica di un incasso bancario o contante. Non creare falsi movimenti di cassa o riversamenti.

Prevedi un modo immediato per correggere le eccezioni, ad esempio tassa non incassata da Elledue, importo diverso, prenotazione esente o tassa raccolta dal portale/proprietario. Non trasformare l'eccezione in un passaggio obbligatorio.

### Compatibilità con i dati esistenti

La 0.4.10 contiene già riscossioni, compensazioni e movimenti registrabili manualmente.

Il nuovo importo automatico deve essere riconciliato con gli anticipi già registrati, evitando che la tassa venga sottratta due volte.

Casi necessari:

- tassa prevista senza movimenti: applicare l'anticipo secondo la regola configurata;
- tassa con compensazione già registrata: non duplicarla;
- tassa con compensazione parziale: gestire correttamente la parte residua;
- importo modificato manualmente: preservare la rettifica;
- prenotazione annullata: non creare nuovi anticipi automatici;
- rendiconto confermato: non modificare lo snapshot storico;
- prenotazione che attraversa due mesi: rispettare le regole di competenza.

Tieni disponibile internamente la tracciabilità contabile, ma non esporre al proprietario una lunga lista di stati fiscali.

## C. Nuovo saldo mensile

La sezione conclusiva di entrambi i rendiconti deve seguire questa struttura:

**RIEPILOGO FINALE**

- Compenso di gestione Elledue
- Spese anticipate da Elledue
- Già saldato / anticipi in compensazione
- **TOTALE ANCORA DOVUTO A ELLEDUE**

Formula:

`Saldo = compenso di gestione + spese anticipate rimborsabili − anticipi riconosciuti − altri pagamenti già registrati`

Gli anticipi in compensazione sono normalmente costituiti dalla tassa di soggiorno.

Se esistono altri pagamenti già registrati, includili nel totale "Già saldato" senza perdere la tracciabilità dei singoli movimenti.

Se il risultato è negativo, mostra chiaramente un credito, senza rendere ambiguo il segno.

Esempio di quadratura:

- Compenso Elledue: €150.
- Spese anticipate: €26.
- Tassa riconosciuta in compensazione: €36.
- Totale dovuto: **€140**.

Verifica che nessuna voce venga sommata o sottratta due volte.

## D. Pulizie e lavanderia

La 0.4.10 calcola già valori standard. Mantieni tali automatismi.

Lavanderia per singolo soggiorno:

`set = ceil(ospiti/2)`

`costo = set × €5`

**Non calcolare i set sul totale degli ospiti di tutte le prenotazioni del mese.**

Caso obbligatorio:

- soggiorno A: 2 ospiti → 1 set;
- soggiorno B: 1 ospite → 1 set;
- soggiorno C: 1 ospite → 1 set.

Risultato: **3 set, €15**, non 2 set/€10.

Pulizia ordinaria per immobili in gestione:

- 2 ore per soggiorno;
- €8/ora;
- €16 per soggiorno, salvo override.

Nel rendiconto devono comparire:

- totale ore di pulizia;
- totale costo pulizie;
- totale set lavanderia;
- totale costo lavanderia.

Nel PDF dettagliato puoi conservare righe per soggiorno, con ore/set, tariffa e costo.

Nel PDF sintetico bastano i totali aggregati, senza elenchi di singole pulizie e singoli set.

Gli importi devono essere coerenti tra i due layout.

I valori automatici devono applicarsi anche alle prenotazioni pregresse eleggibili, preservando override e spese consuntivate.

## E. Semplificazione grafica di entrambi i PDF

Modifica `ReportPdf.kt`, `ReportSnapshot.kt`, `ReportScreens.kt` e i modelli necessari.

### Eliminare dai PDF destinati ai proprietari

- Il nome del proprietario nell'intestazione.
- Le percentuali di gestione applicate.
- La colonna "Gestione" con la percentuale.
- La parola "Stimata" sotto la commissione OTA.
- Etichette tecniche come "Costi a carico Elledue / standard".
- Etichette come "Costi da riconoscere nel saldo".
- Suddivisioni superflue della tassa tra prevista, riscossa, residua e compensata.
- Spiegazioni contabili prolisse.
- Identificativi tecnici, hash e versioni non utili al destinatario.
- La dicitura "Bozza" nei documenti ufficiali confermati ed esportati.

Conservare il valore monetario del compenso Elledue e il costo OTA complessivo, IVA compresa quando prevista.

### PDF sintetico

Struttura suggerita:

1. Intestazione Elledue, immobile e mese.
2. Prenotazioni e ricavi.
3. Lordo, commissioni OTA complessive con IVA e netto.
4. Compenso Elledue.
5. Pulizie: numero totale ore e importo.
6. Lavanderia: numero totale set e importo.
7. Altre spese anticipate.
8. Anticipi in compensazione / già saldato.
9. Totale da corrispondere.

### PDF dettagliato

Stessa logica contabile e stesso riepilogo finale, con dettaglio per prenotazione e per spesa.

Il sintetico non deve omettere lavanderia o pulizie soltanto perché sono disponibili nel dettagliato.

### Bozze ed esportazione

Mantieni internamente il ciclo di vita bozza/confermato e l'immutabilità degli snapshot.

L'utente può visualizzare in app un'anteprima chiaramente indicata come bozza.

Il **PDF ufficiale senza dicitura di bozza** deve essere esportato dopo la conferma del rendiconto. Se permetti l'esportazione di un PDF non ancora confermato, deve essere chiaramente differenziato e non presentato come documento definitivo.

Non cancellare le protezioni contabili per eliminare una scritta.

## F. Test obbligatori

Aggiungi test per:

- commissione OTA 19,5% + IVA 22%;
- importo OTA già comprensivo di IVA;
- commissione OTA effettiva;
- portale senza maggiorazione;
- OTA diretta allo 0%;
- percentuali di gestione diverse per immobile;
- tassa automatica con 0, 1 o più esenti;
- compensazione automatica senza doppio conteggio;
- movimenti di compensazione preesistenti;
- prenotazioni annullate;
- importi manuali e storici confermati;
- lavanderia aggregata da tre soggiorni 2+1+1;
- pulizie 2 ore per soggiorno;
- uguaglianza dei totali tra PDF sintetico e dettagliato;
- esportazione corretta e nessuna dicitura tecnica indesiderata;
- migrazioni, backup, restore e backfill idempotente.

Genera PDF sintetici di prova, leggi il loro contenuto effettivo e verifica le somme.

## Consegna del primo intervento

Implementa davvero il codice e crea commit sul nuovo branch.

Restituisci:

- branch e commit;
- file modificati;
- regole economiche definitive;
- esempi numerici;
- migrazioni;
- test realmente eseguiti;
- PDF di test;
- problemi ancora aperti.

Non effettuare merge su `main`.

Il lavoro proseguirà nello stesso branch con il prompt 2/3.