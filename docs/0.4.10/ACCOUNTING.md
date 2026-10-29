# Regole economiche e riconciliazione 0.4.10

## Importi e natura delle registrazioni

I calcoli usano centesimi Long, punti base Int e BigDecimal HALF_UP. Nessun importo contabile usa Float/Double. La tassa è sempre esterna al lordo e alla base del compenso. I portali pagano normalmente il proprietario.

- **Previsione standard:** quantità, tariffa e totale proposti; nessuna spesa o riscossione viene registrata automaticamente dal backfill.
- **Bozza di rendiconto:** include i consuntivi e gli standard proposti, etichettati. I totali sono già quelli da verificare; il PDF della bozza mostra la natura provvisoria delle voci.
- **Standard approvato:** l'operatore approva esplicitamente in blocco i costi da riconoscere nel saldo quando conferma il report. L'approvazione non attesta un pagamento né l'avvenuta esecuzione dell'intervento. La data rimane indicata come prevista.
- **Consuntivo:** registrazione esplicita dell'operatore. Una rettifica di uno standard approvato non lo trasforma automaticamente in consuntivo: occorre selezionare «Confermo il consuntivo dell’intervento». Il motivo della rettifica e lo storico restano conservati.

Le condizioni economiche già concordate consentono i costi standard per gli immobili in gestione. L'operatore deve verificare l'accordo prima dell'approvazione. Pulizie iniziali: 2 ore × tariffa immobile (default 8 €/h = 16 €). Lavanderia: ceil(ospiti / ospiti per set) × tariffa (default 2 ospiti, 5 €, letto + asciugamani), configurabile. Anche gli immobili propri possono avere una previsione privata di lavanderia; non ricevono automaticamente pulizie da immobile gestito, compenso di gestione o rendiconto per proprietario esterno.

## Competenza e date

Il soggiorno mantiene la competenza integrale del mese del check-in. Per i **nuovi standard approvati in blocco** la competenza segue il soggiorno e la data prevista dell'intervento è il check-out. Sono due dati distinti.

Esempio: 29 novembre–3 dicembre, pulizia standard proposta nel rendiconto di novembre con intervento previsto il 3 dicembre. Dopo l'approvazione la stessa voce è riconosciuta a novembre e non viene generata nuovamente a dicembre.

Una spesa manuale già registrata a dicembre conserva quella competenza. Il rendiconto di novembre mostra il collegamento informativo alla spesa, con data, periodo e importo **escluso dai suoi totali**. Quello di dicembre la contabilizza. Nessun trasferimento silenzioso tra mesi e nessun costo duplicato.

## Retroattività e ambiguità

La riconciliazione `EconomicReconciliation` viene eseguita automaticamente all'avvio dopo l'aggiornamento e dopo il ripristino di un vecchio backup. Il marcatore `maintenance_state/economics-0410` registra il completamento. La procedura è transazionale e idempotente; i test ripetono anche una riconciliazione forzata.

1. Prenotazione annullata: nessun nuovo standard o incasso; costi realmente registrati conservati.
2. Soggiorno in uno snapshot confermato o già attribuito: niente backfill sulle sue fonti; il documento resta intatto. Per cambiare un costo rendicontato si apre una nuova versione rettificativa.
3. Standard mancante: si crea soltanto la previsione, usando le regole dell'immobile.
4. Previsione automatica: può aggiornarsi se cambiano ospiti o parametri; override manuali preservati.
5. Spesa collegata, consuntivo o rettifica esistente: conservati. Nessuna seconda previsione approvabile per lo stesso tipo di servizio.
6. Spese dello stesso tipo senza prenotazione nel periodo: non si indovina l'associazione da importo o descrizione. Il report avvisa e non propone una seconda voce per i soggiorni potenzialmente coperti. L'operatore può collegare la spesa corretta.
7. Tassa non consolidata, senza movimenti o rettifiche manuali e con conteggi attendibili: ricalcolo della previsione. Esenzioni storiche sconosciute rimangono sconosciute e vengono segnalate, senza attribuire falsamente «zero esenti».
8. Imposta consolidata, rettificata, già riscossa/riconciliata o rendicontata: preservata. Cambiare le impostazioni dell'immobile non la ricalcola silenziosamente. Modificare il soggiorno può richiedere una verifica esplicita dei conteggi.

## Approvazione atomica e unicità

Gli standard usano l'origine stabile `stay-cost:<prenotazione>:<tipo>`, non il confronto tra descrizioni o importi. Un indice univoco protegge l'origine. La conferma controlla nuovamente l'hash dell'anteprima e le fonti, crea in transazione le spese approvate, le revisioni, il batch d'approvazione e lo snapshot definitivo con le attribuzioni. Un errore a metà provoca il rollback di tutto: nessun costo o audit parziale.

`standard_approvals` conserva documento, data, identità dell'operatore locale configurato, origini approvate e hash dell'anteprima. Le revisioni delle spese conservano quantità, tariffe e importi di ogni versione. Non è un'identità remota verificata: l'app resta locale e senza account obbligatorio.

Un nuovo intervento manuale dello stesso tipo e soggiorno viene rifiutato se esiste già una voce attiva: si modifica quella esistente. Eventuali duplicati storici non sono cancellati automaticamente; il report li segnala e richiede la correzione prima di confermare un totale duplicato.

## Tassa, riscossione e anticipo

La formula standard è `(ospiti - esenti) × min(notti, limite immobile) × tariffa`. Nuove prenotazioni: esenti inizialmente zero, quantità modificabile, nessuna data di nascita obbligatoria. I parametri del comune rimangono configurabili.

«Conferma tassa incassata» propone il residuo e permette una cifra parziale. La scelta del soggetto che incassa è esplicita. Se incassa Elledue, l'operatore può riconoscere contemporaneamente la somma come anticipo nel saldo del proprietario: riscossione e compensazione sono create **nella stessa transazione**, con un identificativo di richiesta durevole e idempotente. Proprietario, portale e altri soggetti non generano compensazioni Elledue. Immobili propri: nessun anticipo con proprietario esterno.

Il riversamento resta separato; non è una seconda sottrazione al saldo. Storni e rettifiche conservano gli originali. Il report espone previsto, riscosso, residuo e compensato. Un saldo negativo rimane un credito da riconciliare, senza creare bonifici negativi automatici.
