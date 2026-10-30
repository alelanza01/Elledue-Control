# CODEX — ELLEDUE CONTROL — 0.4.7
## Rifinitura operativa + nuovi asset + calendario copiabile + messaggi ospiti

Agisci come Senior Android Developer, Software Architect, Senior Product/UX/UI Designer e QA Engineer. Devi **implementare realmente** le modifiche elencate nel repository Android esistente, non produrre soltanto un documento o un mockup.

**Repository:** `alelanza01/Elledue-Control`  
**Application ID da preservare:** `it.elledue.control`  
**Riferimento storico della versione approvata graficamente:** 0.4.6, commit `c156458de9c7d90f3585a3f44a7ca23db540cb88`. Questo non autorizza rollback: verifica il HEAD effettivo e preserva ogni lavoro successivo.

L'utente allega `Elledue_Control_047_Pacchetto_Codex.zip`. Estrai il pacchetto e ispeziona i file nella struttura indicata in fondo. Se l'allegato non è disponibile nel workspace, **non inventare asset**: segnala l'impedimento e procedi con le parti che non ne dipendono. Non usare immagini d'anteprima come asset finali.

## 0. Preflight obbligatorio

1. Esamina stato del repository, branch, HEAD, modifiche non committate, Gradle, application ID, versione installabile, schema e migrazioni Room, documentazione, test e asset già presenti.
2. Compila una matrice sintetica `requisito → già presente / da implementare / rischio` e poi procedi all'implementazione, senza fermarti alla pianificazione.
3. Preserva il design della 0.4.6, giudicato sostanzialmente approvato, e tutte le funzionalità già esistenti. Non ricominciare il progetto e non cambiare globalmente navigazione, palette, tipografia o logica contabile.
4. Lavora su un branch dedicato. Non fare merge, release pubbliche o distribuzione senza autorizzazione. Non usare migrazioni distruttive, non cancellare dati reali, non alterare prenotazioni/rendiconti storici.
5. Mantieni: immobili, proprietari, prenotazioni e annullamenti, calendario, contabilità, OTA, pulizie, lavanderia, spese, anticipi, tassa di soggiorno, riscossioni, compensazioni, riversamenti, rettifiche, rendiconti PDF con bozze/versioni, backup/ripristino. Non eliminare funzioni per semplificare la UI.

## 1. Asset grafici allegati — obbligatorio

Nel file ZIP trovi:

- `01_Copertine_originali/01_terrazza_etna.png`
- `01_Copertine_originali/02_living_mediterraneo.png`
- `01_Copertine_originali/03_terrazza_blu_ora.png`
- `02_Copertine_ottimizzate/` con varianti WebP `16x9` e `banner_2p4x1` per ciascuna delle tre immagini;
- `03_Icone_portali_PNG/booking.png`
- `03_Icone_portali_PNG/airbnb.png`
- `03_Icone_portali_PNG/vrbo.png`
- `03_Icone_portali_PNG/prenotazione_diretta.png`
- `04_Riferimenti/` contiene soltanto anteprime/tavola originale: non inserirle come una singola icona UI.

### Home

Integra le **tre fotografie singole senza testo** come alternative per la copertina artistica della Home. Non incorporare titolo, saluto, KPI o logo direttamente nella fotografia: sono componenti UI sovrapposti e devono rimanere accessibili e responsivi. Integra il logo ufficiale già presente nel repo; **non usare loghi AI inventati** presenti in vecchi concept. La scelta predefinita può essere `01_terrazza_etna`, senza cancellare l'immagine preesistente dell'utente. Nelle impostazioni Aspetto inserisci selezione copertina fra tre immagini con anteprima, applica, eventuale ripristino e gestione sicura di un asset personalizzato già presente. Usa la variante adatta al layout, evita distorsione/crop che copra il soggetto o impedisca la leggibilità. Prevedi overlay adattivo di contrasto e QA su più schermi, modalità scura e chiara ove prevista. Evita pesanti PNG originali come carico runtime quando WebP ottimizzato è adeguato.

### Quattro simboli dei portali

Usa quattro **PNG separati** nell'ordine logico Booking.com, Airbnb, Vrbo, Prenotazione diretta. Ognuno rappresenta un'opzione distinta nel selettore della nuova prenotazione e, ove utile, nei dettagli/calendario. Dimensioni coerenti, icona non deformata né tagliata, label testuale sempre presente per accessibilità. Sono **badge grafici ispirati al concept, non attestazioni di loghi ufficiali/licenze dei relativi marchi**: non spacciarli per materiale ufficiale né modificarne forma/colore senza motivo. Non usare la tavola con le quattro icone unite come asset di interfaccia. Per prenotazioni storiche con portali diversi, mantieni la label corretta e un fallback grafico neutro.

## 2. Attività — modulo completo

Alla creazione: **titolo obbligatorio**; descrizione/note, data, ora, immobile, prenotazione facoltativi. Stato iniziale Da fare. Puoi salvare un'attività senza data; se l'orario necessita di data, UI esplicita senza rendere obbligatoria la programmazione di tutte le task. Se selezioni una prenotazione, verifica coerenza con immobile associato. Permetti creazione, dettaglio, modifica, completamento, eventuale riapertura, eliminazione con conferma. Le attività datate compaiono nel calendario; quelle senza data nella lista delle attività non programmate. Tutte le viste si aggiornano immediatamente. **Eliminare una task generica non cancella pulizie/spese/movimenti già contabilizzati**: per essi valgono i meccanismi di rettifica appropriati.

Correggi la scorciatoia del pulsante centrale `+`: `Nuova attività` deve **aprire direttamente il form di creazione**, senza passare dall'elenco con un secondo `+`. Assicurati che Nuova prenotazione e Nuova spesa restino dirette. Non creare duplicati a seguito di doppio tocco su Salva.

## 3. Calendario e prenotazioni annullate

Conserva il sistema cromatico attuale (vista generale distinta per immobile; nella vista dedicata check-in, check-out e soggiorni con gerarchia chiara), i filtri esistenti e la gestione del cambio ospiti nello stesso giorno. Mostra normalmente le prenotazioni attive. Mantieni le annullate nello storico e in un filtro secondario `Mostra annullate`, visivamente distinguibili, senza occupare date come se fossero soggiorni attivi. Attività con data visibili in calendario; nessuna task senza data deve comparire in un giorno inventato.

## 4. NUOVA FUNZIONE: Copia calendario per WhatsApp, per appartamento

Aggiungi nel calendario il comando `Copia calendario` accessibile chiaramente, con selezione periodo `Settimana`, `Mese`, `Personalizzato`. Preseleziona il periodo coerente con la vista corrente. L'utente sceglie un appartamento oppure, in vista generale, vede un blocco **indipendente per ciascun immobile** con un proprio bottone `Copia messaggio`. **Non mescolare più appartamenti in un testo unico**.

Includi prenotazioni **attive che si sovrappongono al periodo selezionato** (non soltanto quelle il cui check-in cade nel periodo); riporta sempre le date complete e reali. Escludi annullate. Ordina per check-in crescente, poi criterio stabile in caso di pari data/ora.

Formato obbligatorio testuale (dati qui sotto SOLO DI ESEMPIO):

```text
CASA MARE

Mario Rossi (4 ospiti)
Check-in 15/10/2026 ore 15.00
Check-out 20/10/2026 ore 11.00

Giuseppe Bianchi (2 ospiti)
Check-in 22/10/2026 ore 16.00
Check-out 26/10/2026 ore 10.00
```

Regole: intestazione NOME APPARTAMENTO; riga vuota; `NOME OSPITE (N ospiti)` oppure `(1 ospite)`; due righe check-in/check-out in formato `DD/MM/YYYY ore HH.mm`; riga vuota fra prenotazioni. Usa il nome ospite principale e numero ospiti effettivo del database. Se l'orario non è registrato, scrivi `orario non indicato`, non inventare un orario. Gestisci accenti/apostrofi e dati mancanti senza crash. **Non includere** prezzi, commissioni, recapiti, documenti ospiti o informazioni contabili. Per periodo vuoto mostra stato vuoto leggibile, senza una falsa prenotazione. Mostra un'anteprima e copia negli appunti Android la stringa effettiva, con conferma solo se la copia riesce. L'utente la incolla manualmente in WhatsApp: nessun invio automatico e nessuna WhatsApp Business API. Riusa l'attuale fonte dati del calendario, non creare un secondo archivio.

Testa uno/più soggiorni, più immobili, nessuna prenotazione, annullata, cavallo mese/anno, cambio ospiti nello stesso giorno, singolo ospite, orario mancante, caratteri speciali, periodo personalizzato e contenuto degli appunti.

## 5. Portale prenotazione e commissioni OTA

Nel form `Nuova prenotazione`, selettore con i quattro badge/label:

1. Booking.com — **preselezionato** inizialmente;
2. Airbnb;
3. Vrbo (questa grafia);
4. Prenotazione diretta.

Per ogni immobile, in impostazioni intuitive, configura percentuali OTA predefinite persistenti e modificabili per Booking.com/Airbnb/Vrbo; prenotazione diretta con commissione OTA iniziale 0%. **Non inventare percentuali universali**: se assenti, richiedi configurazione appropriata. Nella nuova prenotazione carica la percentuale del portale per l'immobile scelto, consenti override per la singola prenotazione e inserimento dell'importo OTA effettivo in euro. Se presente, **l'importo OTA effettivo prevale** sulla stima percentuale secondo la logica economica già approvata. Congela/salva i parametri effettivamente applicati sulla singola prenotazione: cambiare i default non ricalcola storici/rendiconti passati. Prenotazione diretta con OTA 0% non significa assenza assoluta di altre commissioni/costi di pagamento; gestiscili separatamente se già previsti, senza doppie registrazioni. Gestisci portali legacy senza perdere dati.

## 6. Tassa di soggiorno: interfaccia esplicita, stessa correttezza contabile

Rendi chiaro il percorso `Economia → + → tassa e anticipi` (eventuale etichetta riformulata). Separa nettamente:

- `Tassa prevista` — stima/calcolo assistito basato sui parametri configurati e sugli ospiti soggetti, con verifica;
- `Registra incasso` — importo **effettivamente** ricevuto, manuale, anche parziale;
- `Compensa con proprietario` — riconoscimento manuale nel saldo;
- `Registra riversamento` — versamento effettivo all'ente, manuale;
- `Altri pagamenti/anticipi del proprietario` — ove già previsti, distinti.

La creazione della prenotazione non prova un incasso. La registrazione di una previsione non genera automaticamente compensazione o riversamento. Un riversamento non sottrae nuovamente somme già compensate al saldo del proprietario. Conserva integri audit, storico, importi parziali e controlli sui doppi conteggi. Non cambiare indiscriminatamente aliquote o regole vigenti: utilizza la configurazione esistente e documenta eventuali parametri da aggiornare separatamente.

## 7. Rendiconto sintetico e PDF

Nel sintetico mostra **Pulizie** e **Lavanderia** distintamente, non soltanto nascoste dentro `Spese rimborsabili`. Presenta chiaramente: compenso gestione Elledue; pulizie; lavanderia; altre spese; totale dei soli rimborsi effettivamente dovuti; eventuale tassa riscossa separata secondo corretto trattamento; anticipi/compensazioni/pagamenti; saldo finale. Una voce di pulizia non diventa automaticamente rimborsabile: preserva distinzioni fra costo operativo, anticipo Elledue, rimborsabile e già riconosciuto. Nessun doppio conteggio. Riduci l'eccesso di dettaglio fiscale nel PDF sintetico destinato al proprietario, conservando dettaglio completo nelle viste interne o nel rendiconto analitico. PDF multipagina premium, leggibile e coerente con i totali.

**Regola contabile immutabile:** l'intera prenotazione, anche a cavallo di mese/anno, appartiene al rendiconto del mese/anno del CHECK-IN. Pulizie, lavanderia e altre spese seguono il mese di competenza assegnato alla singola spesa. Non modificare rendiconti già confermati: usa flussi di rettifica/versionamento già previsti.

## 8. Analisi e confronti

La sezione risultava troppo spoglia. Migliorala in modo mirato e premium/futuristico, usando grafici e indicatori **soltanto da dati reali**, colori coerenti con calendario/immobili, filtri immobile/periodo e confronti sensati. Valuta e implementa per quanto i dati presenti supportano: andamento economico per immobile e mese, commissioni Elledue maturate, confronto fra immobili, spese per categoria, andamento prenotazioni, scostamenti fra periodi. Non confondere lordo, netto dopo OTA, compenso Elledue e importi proprietario. Non confondere periodo di soggiorno e competenza contabile. Percentuali e confronti devono gestire correttamente periodi vuoti/basi zero; non inventare statistiche o dati demo spacciandoli per reali. Stati vuoti anch'essi curati.

## 9. Libreria messaggi ospiti IT/EN

Implementa se manca, o completa se parziale, la sezione **modelli di messaggi preimpostati personalizzabili**. Categorie iniziali: pre-arrivo, check-in, durante il soggiorno, check-out, post-soggiorno, richieste particolari, altre comunicazioni. Categorie espandibili/modificabili. Modelli globali o per singolo immobile. Per ciascun modello: titolo, categoria, testo italiano, testo inglese, crea/modifica/duplica/archivia/elimina con conferma, anteprima e copia. Un immobile può avere istruzioni specifiche differenti. **Non inventare** codici accesso, indirizzi, telefoni o procedure. Variabili dinamiche, per esempio nome ospite, immobile, data/ora check-in, check-out, tassa residua, risolte usando la prenotazione scelta; segnala chiaramente i valori mancanti senza inventarli. Percorso rapido dalla prenotazione: scegli modello → lingua → anteprima → copia negli appunti. Nessun invio automatico WhatsApp/email. Questa libreria è distinta dalla funzione di esportazione testuale del calendario.

## 10. Revisione UX e microinterazioni

Verifica che le operazioni frequenti richiedano passaggi essenziali: nuova prenotazione, spesa, attività, modifica, calendario filtrato per immobile, copia calendario, portale/OTA, tasse, bozza rendiconto, anteprima PDF e messaggi ospiti. Correggi pulsanti nascosti, etichette ambigue, duplicazioni e difficoltà concrete **senza rifare il design 0.4.6**. Controlla ancora icona app entro safe area Android (nessun logo tagliato), gestione/archiviazione sicura immobili con storico, cover e media, corretto layout su più dimensioni, dark/light ove previsto, accessibilità, back navigation e schermate vuote. Non eliminare mai dati contabili a seguito dell'eliminazione di una task o dell'archiviazione immobile.

## 11. Test di accettazione obbligatori

Test automatizzati quando possibile e QA manuale documentata. In particolare:

- task solo titolo, con data/ora, con immobili/prenotazione, edit/delete/complete, calendario, + diretto e nessuna cancellazione contabile;
- calendario ordinario/annullate; permanenza nello storico;
- quattro portali e quattro PNG individuali; Booking default; persistenza OTA per immobile; override, OTA effettiva prevalente; snapshot storico;
- Copia calendario: tutti i casi della sezione 4, inclusi overlap periodo, testi e clipboard;
- tasse previste/incassate/parziali/compensate/riversate e nessuna doppia sottrazione;
- pulizie e lavanderia separate nel sintetico, PDF coerente, permanenza regola mese check-in;
- messaggi IT/EN, template specifici immobile, variabili mancanti, anteprima/copia, zero invio automatico;
- grafici con pochi dati, zero, più immobili e filtri;
- cover selezionabili/leggibilità dopo riavvio e orientamenti/dimensioni pertinenti;
- build, test unitari/strumentali disponibili, lint, migrazioni Room, backup/export/import, persistenza e regressione della 0.4.6.

Non dichiarare test, screenshot su dispositivo fisico, APK, backup o verifiche realmente non eseguiti. Distingui emulatori, build e telefono fisico.

## 12. Vincoli e consegna

Non collegare PMS/OTA esterni, WhatsApp Business API, servizi cloud a pagamento, portale staff o funzioni dell'altra app personale Progress. Non alterare il sito pubblico `elleduemanagement.com`.

Consegna:

1. branch + commit identificabile, senza merge;
2. changelog 0.4.7 e matrice dei requisiti (`implementato / già presente verificato / parziale / non implementato e perché`);
3. sorgenti e asset singoli integrati correttamente;
4. APK installabile **solo se build realmente riuscita**, indicando path effettivo, firma/application ID/versionCode/versionName;
5. screenshot reali delle schermate toccate, se tecnicamente producibili;
6. test eseguiti con risultati, limiti e bug noti;
7. guida backup e aggiornamento **sopra** la versione precedente senza disinstallare, con verifica firma, migrazione e conservazione dati;
8. indicazione chiara delle eventuali attività che richiedono conferma o collaudo su telefono.

**Risultato atteso:** un'Elledue Control coerente con la bella 0.4.6, più veloce e completa: task operativi veri, calendari per immobile copiabili su WhatsApp, portali con i quattro simboli forniti, OTA configurabili, tasse leggibili, rendiconti professionali, analisi arricchite e messaggi ospiti IT/EN. Procedi con implementazione e QA reali, non fermarti a una proposta astratta.
