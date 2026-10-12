# Milestone 4.5 — mappa prima dell'implementazione

Da sei destinazioni equivalenti a quattro: Home, Immobili, Calendario, Economia.
Il + centrale apre massimo tre azioni pertinenti, senza contabilità duplicata.

| Funzione | Collocazione nuova |
|---|---|
| Immobili, proprietari, foto, archivio | Immobili → scheda → menu azioni |
| Prenotazioni, calendario | Calendario → soggiorno |
| Attività | Home → Operatività; accesso dalla prenotazione (stato esplicito, nessuno scheduler simulato) |
| Spese, pulizie, lavanderia, tassa, anticipi, categorie | Economia → Contabilità; tassa/spesa anche dalla prenotazione |
| Analisi, confronti, occupazione calendario | Economia → Analisi |
| Rendiconti proprietari | Economia → Rendiconti → documento → anteprima PDF |
| Backup, organizzazione | Intestazione → Impostazioni |
| Profilo operatore locale | Avatar nell'intestazione |
| Centro notifiche futuro | Campanella → stato vuoto esplicito |

Home: agenda del giorno, attenzione contabile, bozze da verificare, prossimi soggiorni,
portfolio limitato. Nessuna fotografia, nome o statistica inventata in produzione.
Economia interna separata dai documenti destinati ai proprietari. Occupazione = notti
occupate nel periodo / giorni del calendario, non disponibilità commerciale.

Staff, manutenzioni, archivio e partner restano evoluzioni documentate: futuro accesso
staff limitato agli incarichi, sync autenticata e ruoli; intervento con id stabile e
una sola spesa collegata dopo conferma; documenti privati locali/cifrati con controllo
accessi e scadenze; catalogo partner sotto Operatività. Nessuna falsa sincronizzazione.
