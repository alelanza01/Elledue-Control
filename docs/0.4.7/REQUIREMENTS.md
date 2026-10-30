# 0.4.7 — preflight e matrice

Base verificata: main c156458, nessuna modifica locale; branch feature/0.4.7-operations. Application ID it.elledue.control, versione 0.4.6/code6, Room5. Ambiente Android/JDK/Gradle esistente riutilizzato.

| Requisito | Stato iniziale | Lavoro / rischio |
|---|---|---|
| Design 0.4.6 e logo ufficiale | Presente | Conservare tipografia, navigazione, safe area |
| Copertine / badge allegati | Disponibili nello ZIP | Integrare WebP e PNG singoli; fotografie artistiche, non immobili reali |
| Attività | Solo titolo/completamento | CRUD, note/data, calendario e scorciatoia diretta; migrazione additiva |
| Calendario e annullate | Presente | Aggiungere task e copia per immobile, non cambiare occupazione |
| OTA storiche/effettive | Presente | Selettore portali, configurazione esplicita; preservare legacy |
| Tassa / ledger | Presente | Chiarire etichette; non cambiare logica finanziaria |
| PDF sintetico | Presente | Separare rimborsi pulizie/lavanderia/altre senza duplicazione |
| Analisi | Parziale | Filtri, netti/compensi, trend e categorie da dati reali |
| Messaggi | Solo testi immobile IT/EN | Libreria CRUD/categorie/variabili/copia; conservare testi legacy |
| Backup | Versioni4/5 | Versione6, compatibilità import4/5, validazione nuove tabelle |
| Aggiornamento | Firma debug nota | Stesso ID/certificato, code7; telefono fisico non disponibile |

Nessuna migrazione distruttiva, nessun merge o pubblicazione APK. La build resterà locale per la consegna. Nessun documento privato entra nel repository.

## Esito finale

| Requisito | Esito | Evidenza |
|---|---|---|
| Design / asset | Implementato | PNG/WebP originali dello ZIP, Theme/brand preservati; screenshot Home/Aspetto |
| Attività complete | Implementato | OperationsUiTest: + diretto, solo titolo senza immobili attivi, data/ora, ricreazione, modifica/completa/riapri/elimina; OperationsRepositoryTest: idempotenza, FK e contabilità conservata |
| Calendario attive/annullate/task | Già presente verificato + esteso | ReservationUiTest e OperationsUiTest; task senza data assenti dalla griglia |
| Copia calendario | Implementato | GuestTextTest (overlap, anno/mese, ordine, cambio ospite, singolo/plurale, orari mancanti, vuoto, accenti, personalizzato); OperationsUiTest verifica clipboard Android, isolamento immobili, persistenza e landscape |
| Portali / OTA | Implementato | Quattro PNG separati; UI testa Booking/Airbnb/Vrbo/diretta e OTA effettiva prioritaria; repository verifica assenza default, parametri storici e tariffe configurate |
| Tassa / anticipi | Già presente verificato + UX esplicita | AccountingUiTest/AccountingRepositoryTest/ReportRepositoryTest: parziali, compensazioni univoche, riversamenti separati, storni e snapshot |
| PDF sintetico | Implementato | OperationsPdfTest: pulizie24, lavanderia10, altre5, rimborsi39, saldo170; costo non rimborsabile900 escluso; una pagina. Analitico/stress/versioni verificati dai test esistenti |
| Analisi | Implementato | Filtro immobile/mese, lordo/netto/compensi, variazioni e trend, categorie. PortfolioAnalytics testato; UI con dati reali e periodo vuoto |
| Messaggi ospiti | Implementato | CRUD, globale/immobile, categorie, archivio, legacy IT/EN; test repository/backup e UI anteprima, variabili mancanti, copia inglese effettiva e ricreazione |
| Persistenza / migrazioni | Implementato e verificato | Catene1→6,2→6,3→6,4→6 e5→6 su SQLite/schema pubblicato con dati; snapshot confermato/allocazioni/movimenti preservati |
| Backup | Implementato e verificato | Export6, restore verificato4/5/6, nuove tabelle/copertina/note/relazioni incluse; checksum e staging atomico; suite regressione esistente |
| Build e lint | Verificato | assembleDebug + testDebugUnitTest + lintDebug: BUILD SUCCESSFUL,136 test,0 fallimenti/errori/saltati; lint0 errori/6 avvisi preesistenti |
| Telefono fisico / aggiornamento installato | Non eseguito | Ambiente senza telefono/emulatore. Certificati APK0.4.6 e0.4.7 confrontati e identici; procedura in UPDATE.md |
| APK pubblico / merge | Non eseguito, come richiesto | APK locale, sorgenti su branch dedicato, main invariato |

Nessuna funzione finanziaria simulata e nessun dato dimostrativo nella modalità normale. La libreria non invia messaggi; copia solo dopo azione manuale. I valori dinamici mancanti impediscono la copia del modello finché non risolti. I testi storici degli immobili restano disponibili; alla prima modifica in libreria viene salvato un modello della libreria, che può evolvere indipendentemente dal vecchio campo istruzioni accesso nella scheda immobile.

Convenzione task senza ora: una data senza orario è registrata all’inizio della giornata Europe/Rome e trattata come attività della giornata; non crea una notifica. Gli orari inesistenti al cambio ora legale sono rifiutati. Ricorrenze e allarmi Android rimangono alla milestone successiva.
