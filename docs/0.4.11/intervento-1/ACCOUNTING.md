# Regole economiche — intervento1/3

## OTA e IVA

Unico motore: `BookingFinance`. Tutti gli importi sono centesimi Long con moltiplicazioni decimali esatte; arrotondamento HALF_UP al centesimo separatamente su commissione base, IVA e compenso. L'importo effettivo, incluso zero, prevale sulla percentuale.

- `BASE_PLUS_VAT`: percentuale × lordo, oppure effettivo base, più IVA sulla sola commissione. Default IVA22%, configurabile.
- `INCLUDED`: importo già comprensivo di IVA; nessuna seconda maggiorazione. Inserendo per la prima volta un effettivo nel modulo viene proposto questo trattamento; l'operatore può correggerlo se l'importo è imponibile.
- `NONE`: nessuna maggiorazione economica; il costo è la percentuale o l'effettivo registrato.
- Diretta: zero e nessuna maggiorazione come default; una configurazione esplicita del portale può prevedere un costo.

Gerarchia: prenotazione corretta esplicitamente → portale immobile → immobile. La prenotazione salva percentuali, trattamento e aliquota IVA; report, PDF e analytics utilizzano i medesimi totali memorizzati e validati. La modifica di una tariffa non riscrive i report confermati.

Esempio: lordo100000; OTA19,5%=19500; IVA22%=4290; costo23790; netto76210; gestione20%=15242 (152,42€), oppure25%=19053 (190,53€).

Per prudenza gli immobili esistenti conservano `NONE` finché non viene configurata la maggiorazione. Non è corretto assumere che ogni fattura storica debba ricevere un ulteriore22%. Il cambio di configurazione adegua le prenotazioni modificabili senza OTA effettiva o trattamento manuale; gli effettivi storici non sono riclassificati arbitrariamente.

## Tassa e anticipo convenzionale

Nel flusso ordinario bastano ospiti, date ed eventuali esenti. Regole per immobile, default3€ × non esenti × min(notti,4), dai16anni compiuti. Nessuna conferma fiscale per la normale previsione.

Per gli immobili gestiti l'anticipo convenzionale è abilitato come default. La migrazione abilita la regola sull'immobile; la riconciliazione la applica soltanto ai soggiorni modificabili. Gli immobili propri non ricevono anticipi verso un proprietario esterno. L'eccezione è facoltativa nel modulo della prenotazione: disattivazione dell'automatismo o importo diverso; l'incasso reale resta una registrazione opzionale distinta.

Per ogni soggiorno:

`target = importo anticipo rettificato, se presente; altrimenti max(0, tassa prevista − riscossioni attive di proprietario/portale/altro)`

`quota automatica = max(0, target − tutte le compensazioni attive già registrate per il soggiorno)`

`anticipi riconosciuti del mese = compensazioni registrate nel mese + quote automatiche dei soggiorni con check-in nel mese`

Si considerano anche le compensazioni registrate in altri mesi per non riconoscere di nuovo la stessa somma. Esempio36€: senza movimenti quota automatica36; con compensazione12 quota automatica24 e totale36; con compensazione36 quota0. Se36 sono già riconosciuti in dicembre, novembre non li anticipa una seconda volta. I movimenti stornati non sono attivi.

Nessuna quota automatica genera COLLECTION, COMPENSATION, REMITTANCE o denaro incassato. Il riconoscimento resta verificabile dai parametri, dalle revisioni e dallo snapshot. Una registrazione manuale già riconosciuta non viene cancellata disattivando l'automatismo: per correggerla occorre stornare il movimento o aprire una rettifica, preservando lo storico.

Le prenotazioni annullate non ricevono nuovi anticipi; i movimenti preesistenti restano da riconciliare. Le rettifiche manuali dell'imposta rimangono valide. Le esenzioni storiche ignote non vengono inventate.

## Costi e saldo

Pulizie:2 ore ×8€/ora per soggiorno gestito salvo tariffe/override. Lavanderia:ceil(ospiti/2) ×5€ per ciascun soggiorno, poi somma dei soggiorni;2+1+1 ospiti distribuiti in3 soggiorni producono3set/15€, pulizie6ore/48€.

Costi proposti in bozza e approvazione atomica aggregata esistenti conservati. Approvazione contabile, consuntivo e pagamento rimangono distinti. I PDF al proprietario presentano soltanto costi anticipati/rimborsabili pertinenti; le spese non rimborsabili restano in contabilità interna. Le ore/set aggregano le stesse righe mostrate nei relativi importi.

`saldo = compenso + spese anticipate rimborsabili − anticipi riconosciuti − pagamenti del proprietario`

Esempio150+26−36=140€. Il riepilogo finale è identico nei due PDF. Un saldo negativo è presentato in valore assoluto come CREDITO DA RICONCILIARE.

## Storicità e competenza

Mese/anno del check-in per l'intero soggiorno. Data intervento distinta; vecchie spese manuali in altro mese conservano la loro attribuzione, senza doppie somme. La riconciliazione0411 è transazionale, con marcatore separato dalla0410 e ripetibile. Salta annullate, fonti allocate e tutte le prenotazioni presenti negli snapshot confermati. Ogni modifica applicata produce una revisione economica. Una modifica intenzionale dell'anticipo nella prenotazione viene anch'essa registrata senza imporre un nuovo modulo di approvazione.

Gli snapshot confermati restano byte per byte invariati; le modifiche a un periodo chiuso richiedono una nuova versione rettificativa. Il PDF ufficiale è esportato dallo snapshot confermato senza etichetta bozza; un export non confermato riporta BOZZA · NON DEFINITIVO. Hash, identificativi, versioni tecniche, nominativo del proprietario, percentuali di gestione e stati fiscali non sono stampati nei PDF.
