# 0.4.10 — Automazioni economiche e UX

- Riconciliazione automatica e idempotente delle prenotazioni pregresse eleggibili. Nessuna nuova riscossione o spesa pagata prodotta dall'aggiornamento.
- Pulizie standard degli immobili in gestione: 2 ore alla tariffa dell'immobile, inizialmente 8 €/h. Lavanderia iniziale 5 € ogni 2 ospiti, letto + asciugamani, arrotondando i set per eccesso. Tariffe, scaglioni e override personalizzati conservati.
- Previsione privata di lavanderia anche per gli immobili propri, senza pulizie automatiche da gestione, compenso di gestione o rendiconto per un proprietario esterno.
- Standard già visibili e inclusi nei totali delle bozze e nei PDF; approvazione esplicita in blocco durante la conferma del rendiconto.
- Audit del gruppo approvato, origini univoche e revisioni. Una rettifica di prezzo di uno standard approvato conserva il suo stato finché l'operatore non conferma espressamente il consuntivo.
- Collegamento dei costi del soggiorno tra mesi: nuove voci standard nella competenza del check-in con data prevista separata; spese manuali già attribuite ad altro periodo conservate e mostrate come collegamenti informativi, senza doppia somma.
- Tassa ordinaria già proposta dal form prenotazione; retroattività solo sui conteggi attendibili non consolidati e senza movimenti. Nessuna esenzione storica inventata.
- Riscossione rapida, anche parziale, dalla prenotazione. Riscossione Elledue e anticipo riconosciuto possono essere registrati atomicamente, con richiesta idempotente. Riscossioni di proprietario/portale/altri non generano compensazioni Elledue.
- Spese con form base breve, date e collegamenti precompilati, soggetto pagante e «Altri dettagli». Ore/tariffe e set/tariffe compaiono quando pertinenti.
- Check-in/check-out aprono lo stesso DateRangePicker; rimosso il terzo pulsante.
- Mese compatto di 5/6 settimane, indicatori sintetici e dettaglio nella Giornata operativa, ora comprensiva dei soggiorni presenti.
- Riepilogo soggiorno con orari, operatività ed eccezioni espandibili; azione al rendiconto pertinente e conferma rapida tassa. Home più operativa e quattro creazioni principali sempre disponibili nel `+` quando esistono immobili attivi.
- Migrazione Room 8→9 additiva, importazione backup 4–9 e conservazione degli snapshot confermati.
