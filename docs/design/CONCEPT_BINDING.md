# Redesign vincolato al concept

Base 48a0ef6, archivio Room 5 e contabilità invariati. Il riferimento è il concept
cinematografico navy/oro con cinque schermate fornito nella conversazione.

Corrispondenze richieste: header sottile; hero fotografico con saluto sovrapposto;
KPI 2×2; azioni a icone; navigazione a quattro destinazioni e + dorato circolare;
portfolio fotografico con gradienti, stato e indicatori sovrapposti; calendario
compatto con pill per colore immobile/tipo evento; timeline operativa; rendiconti
con selettore a segmenti, preview carta reale e saldo/azioni raggruppati.

Font incorporati DM Serif Display e Inter con licenze OFL. Dark principale anche
quando Android usa il tema chiaro. Il marchio originale sostituisce esclusivamente
il simbolo inventato del concept. Nessun meteo, avatar personale, numero economico,
manutenzione o disponibilità commerciale vengono inventati.

Foto di test solo nei test/screenshot; nessuna immagine dimostrativa precaricata.
In assenza di copertina: placeholder scenografico di marca, non dati fittizi.

## Riprodotto e adattato

| Concept | Implementazione reale |
|---|---|
| Hero fotografico e saluto | Copertina conservata nell'archivio, gradiente per leggibilità, nome operatore locale |
| KPI 2×2 | Arrivi/partenze, ospiti, occupazione calendario, lordo ai proprietari con compensi distinti |
| Azioni a icone | Prenotazione, check-in, pulizia e rendiconto; solo funzioni esistenti |
| Portfolio fotografico | Copertina a piena card, gradienti, stato attivo/archiviato, posti/letti e proprietario |
| Calendario con pill | UUID immobile → palette stabile; vista singola con check-in teal, check-out coral, soggiorno blu |
| Timeline attività | Arrivi/partenze con orari previsti; interventi registrati e task persistenti; orario sconosciuto mostrato come — |
| Rendiconti | Selettore sintetico/dettagliato, stato/periodo/versione, pagina PDF vera, export e controlli sorgenti |
| Barra inferiore | Quattro destinazioni, + dorato centrale; Attività e Rendiconti nell'area Economia e accessibili dalla Home |
| Dialoghi | Contenitore navy con bordo oro sobrio, titolo serif, stesse conferme/validazioni esistenti |

Adattamenti espliciti: il simbolo ad arco inventato nella reference è sostituito dal
marchio ufficiale; avatar e meteo non vengono copiati. Senza fotografia personale
compare un avatar anonimo. Nessuna metrica commerciale, camera, manutenzione o
comunicazione automatica viene inventata. La disponibilità commerciale non è
conosciuta: il KPI è etichettato occupazione calendario. I messaggi ospiti completi
non sono ancora operativi, quindi l'azione Home usa i rendiconti realmente disponibili.
Le fotografie negli screenshot sono fixture di scenario con attribuzione, non case
reali né immagini copiate dal concept; nel prodotto vengono usate le copertine dell'utente.

Non viene riprodotto il bordo hardware iPhone: il prodotto è Android nativo, con
inset e controlli accessibili. Su 320dp/font150% la barra conserva le descrizioni
accessibili a icone e il contenuto scorre senza ridurre artificialmente il carattere.
Il tema dark è ora principale anche con Android in modalità chiara, come richiesto.

## Conservazione e verifiche

Application ID `it.elledue.control` invariato; versione 0.4.6/code6 per il nuovo APK.
Room 5, data/domain, migrazioni e backup invariati byte per byte rispetto a 48a0ef6.
Nessuna release key generata, nessuna modifica alla firma esterna configurata.
Test di regressione conservati, con attese esplicite per dati/controlli pronti e
scorrimento verso voci non ancora composte nelle liste. Nessuna asserzione contabile
rimossa. Controllo pixel del testo warm-white nella timeline, due viste del calendario,
ricreazione Activity e render PDF reale su bitmap Android.

Acquisizioni native via Android View.draw/Skia in Robolectric: per la galleria vengono
ricreate le Activity ripristinando la navigazione, così si invalidano le cache grafiche
host prima della cattura. Il visualizzatore PDF resta aperto e viene acquisito dopo
il rendering effettivo delle pagine. Non sono schermate HTML o immagini del concept.
Installazione, tastiera e launcher sul telefono fisico restano da collaudare.

Consegna: `deliverables/redesign-0.4.6`, con APK, galleria delle schermate effettive e
manifest SHA-256. Il collaudo non prosegue verso altre milestone o notifiche.

Esito finale: assembleDebug e lintDebug riusciti; 113 test passati, zero falliti,
errori o saltati. Lint zero errori e sei avvisi non soppressi (tre helper TLS
BouncyCastle non usati, ExifInterface piattaforma e due suggerimenti Bitmap KTX).
La preview mantiene l'altezza durante la rigenerazione per evitare spostamenti
delle azioni. La viewport di navigazione è delimitata e il chrome ha priorità di
disegno, evitando sovrapposizioni delle schermate all'intestazione.
Firma debug verificata e identica alla 0.4.5. Foto demo escluse dall'APK, licenze dei
font incluse nell'APK oltre che nel repository. Collaudo fisico non eseguito.
