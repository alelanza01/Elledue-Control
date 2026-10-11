# Verifica del rendiconto di riferimento

Il PDF fornito è stato esaminato come esempio operativo, non importato nel database o nel repository. Qui sono riportati soltanto aggregati e ambiguità contabili, senza nominativi o dati personali.

- Lordo delle righe: **1.784,35 €**, coerente con il totale dichiarato.
- Somma OTA delle righe: **346,99 €**; riepilogo OTA: **423,33 €**. Differenza **76,34 €**, da riconciliare prima dell’automazione dei rendiconti.
- Una riga da **254,88 €** indica OTA effettiva **48,74 €**; la stima al 19,5% sarebbe **49,70 €**. Può essere un costo effettivo diverso, non necessariamente un errore: nell’app l’importo effettivo prevale sulla stima.
- Il documento mensile include un soggiorno con partenza il primo giorno del mese successivo. La decisione definitiva della Milestone 3 assegna l’intera prenotazione al mese e anno del check-in, anche a cavallo di mesi o anni. Non si divide l’importo.
- Da chiarire quali componenti siano comprese nel lordo (ad esempio pulizie, tassa o altri extra) e nella base commissionabile. Non dedurre automaticamente nuove esclusioni dal PDF.
- Con OTA delle righe, netto **1.437,36 €**: il 20% sul totale dà **287,47 €**, mentre sommare le commissioni arrotondate sulle singole prenotazioni dà **287,48 €**. La Milestone 2 usa HALF_UP per singola prenotazione e somma gli snapshot. Un’eventuale diversa politica mensile va concordata esplicitamente.
- La formula del riepilogo dichiarato è aritmeticamente corretta: **272,20 + 194,00 − 141,00 = 325,20 €**. Non risolve però la differenza tra OTA dettagliate e riepilogo.

La competenza è risolta dall’utente: mese del check-in. Prima della Milestone 4 occorre ancora chiarire composizione del lordo e riconciliazione delle OTA. Non sono state corrette automaticamente le cifre, creati rendiconti o implementate compensazioni fiscali. Gli incassi dei portali spettano al proprietario; Elledue matura il proprio compenso.
