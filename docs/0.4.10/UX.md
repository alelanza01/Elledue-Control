# Flussi e verifiche UX

Il design system esistente, i colori navy/oro, il logo ufficiale, la tipografia e la Home premium sono conservati. Nessun dato dimostrativo viene inserito nell'app normale.

| Flusso | Percorso verificato | Azioni richieste |
|---|---|---|
| Spesa ordinaria da soggiorno | Spesa → importo → Salva; immobile, soggiorno e data già impostati | 3 tocchi più digitazione quando la categoria proposta è pertinente; cambiare categoria richiede apertura e selezione |
| Date | Toccare check-in oppure check-out → scegliere prima e seconda data → confermare | Un solo selettore, nessun terzo pulsante; tipicamente 4 tocchi per un nuovo intervallo |
| Tassa intera | Conferma tassa incassata → Conferma incasso | 2 tocchi quando soggetto e riconoscimento proposti corrispondono all'accordo; importo già compilato |
| Tassa parziale | Stesso riquadro, modifica importo, conferma | Nessun secondo modulo per la compensazione; operazione atomica |
| Standard nel report | Genera bozza → controlla importi/PDF → approva il gruppo → conferma documento | Nessuna conferma individuale per ogni soggiorno; la conferma finale protegge lo snapshot immutabile |
| Eccezione costo | Modifica standard/consuntivo dal riepilogo → quantità, tariffa o importo → salva | Override intenzionale conservato; bozza locale dell'eccezione recuperabile |
| Dettagli spesa | Altri dettagli | Competenza, quantità/tariffa per spese generiche, note e rimborsabilità non affollano il form base |
| Calendario | Griglia mese → tocca data | Griglia 5/6 settimane; dettaglio completo nella Giornata operativa, soggiorni presenti inclusi |

I conteggi descrivono le azioni dei flussi verificati, non tempi misurati su un telefono. Scorrimenti e tocchi di tastiera dipendono da viewport, dimensione caratteri e dati disponibili.

Il calendario è misurato a 320 dp con font 150%: altezza della griglia inferiore a 420 dp. Filtri, Agenda, annullate, copia calendario, mesi e Oggi conservati. La griglia evita i nomi ripetuti per settimana e usa frecce, indicatori attività e colori immobili; il dettaglio non dipende solo dai colori.

Nei form il salvataggio resta nell'intestazione, la tastiera viene chiusa prima di aprire il calendario e viene applicato il padding IME. Il riquadro rapido della tassa usa lo scorrimento principale del soggiorno: non contiene finestre o aree di scorrimento annidate che possano nascondere il comando di conferma.

La Home apre direttamente prossimi soggiorni/partenze, attività incomplete, tasse residue, problemi aperti, eccezioni e rendiconti da verificare. Il `+` include la creazione di un problema/intervento. I dati vengono dalle entità persistenti, senza valori inventati.

Le prove e gli screenshot sono rendering Android nativo in Robolectric, non fotografie di un telefono reale. Gli stati vuoti, le validazioni, la correzione, le bozze e i ritorni dagli editor rimangono coperti dalle regressioni precedenti e dai nuovi test.
