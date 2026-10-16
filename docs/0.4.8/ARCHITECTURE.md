# Elledue Control 0.4.8 — scelte e invarianti

Il punto di partenza è `ea7e654` (sorgenti 0.4.7). Il branch dedicato è `feature/0.4.8-stabilization`; main non viene unito o modificato.

## Salvataggi e bozze

La prenotazione conserva il medesimo UUID durante modifiche e salvataggi; Room aggiorna una sola riga. Una bozza di modifica include la revisione di partenza (`baseUpdatedAt`): se un'altra operazione ha cambiato il record, il salvataggio viene respinto esplicitamente. Le modifiche economiche richiedono un motivo con passaggio di conferma dedicato; gli errori riportano il modulo all'inizio, dove sono visibili. La conferma viene emessa dopo il completamento del repository, non al tocco del pulsante.

Le bozze sono JSON in preferenze private, con commit sincrono a ogni modifica. Chiave distinta per modulo e UUID; non costituiscono registrazioni contabili. Home e Impostazioni offrono il recupero. Un salvataggio riuscito rimuove la relativa bozza solo dopo il commit SQLite; un errore la conserva. I backup includono le bozze con checksum separato. Le preferenze di sicurezza del dispositivo non sono credenziali esportabili.

## Room 6 → 7

Solo migrazione additiva: `businessType` e `appliedBusinessType` iniziali MANAGED, conteggi fiscali preesistenti consolidati, attività GENERAL con ora, nuova tabella `report_booking_options` con chiave composta e FK. Nessun fallback distruttivo. I calcoli applicati ai vecchi soggiorni e gli snapshot confermati rimangono invariati.

## Perimetro operativo ed economia

Le query operative escludono gli immobili archiviati. Il filtro iniziale è In gestione; Propri e Tutti sono espliciti. Le viste di dettaglio e Archivio storico mantengono accesso ai dati esclusi dall'operatività. Le nuove prenotazioni di immobili propri hanno compenso di gestione zero e non generano rendiconti per proprietari esterni. Un cambio di classificazione dell'immobile non riscrive retroattivamente i parametri dei soggiorni: le classificazioni storiche discordanti vengono segnalate. Nessuna quota di comproprietà viene dedotta o inventata.

Importi in centesimi; percentuali in punti base e arrotondamento HALF_UP. Prenotazioni interamente nel mese del check-in. Incasso, compensazione e riversamento della tassa restano movimenti distinti; niente doppie sottrazioni. Una previsione fiscale automatica richiede il numero esplicito di ospiti soggetti, resta provvisoria e non registra denaro. Conteggi consolidati, movimenti o attribuzioni bloccano la ricalcolazione automatica e richiedono verifica.

## Rendiconti

Nascondere il dettaglio nominativo nel PDF lascia gli importi inclusi e aggiunge una nota aggregata. Escludere una prenotazione rimuove dal documento corrente anche spese e movimenti direttamente collegati; non elimina i fatti contabili. Le due opzioni sono indipendenti e applicabili solo a bozze, dopo anteprima dei totali. Per documenti confermati occorre una nuova versione. Le bozze reagiscono alle invalidazioni delle tabelle sorgente; nessuna osservazione circolare dei report stessi.

## Eliminazione

Anteprima delle dipendenze, documento di backup scelto tramite Android, lettura di verifica e validazione integrale prima di abilitare l'eliminazione. La conferma include il nome e l'accettazione esplicita dei report/versioni coinvolti. Prima del commit vengono ricontrollati il file di backup e la fotografia dei dati: cambiamenti richiedono una nuova anteprima. Le cancellazioni delle dipendenze avvengono in un'unica transazione, con controllo delle FK e test di rollback. I documenti dei periodi interessati vengono rimossi integralmente, liberando le attribuzioni delle altre voci conservate; spese indipendenti e proprietari condivisi restano. PDF e backup già esportati non sono cancellabili dall'app.

## Notifiche e accesso

AlarmManager con `setAndAllowWhileIdle`, PendingIntent distinti per evento, canale Android e permesso POST_NOTIFICATIONS. Nessuna dipendenza cloud e nessun allarme esatto richiesto. Riavvio, aggiornamento pacchetto e cambi ora/fuso riprogrammano gli eventi; modifiche e annullamenti cancellano quelli obsoleti. Pianificazione Europe/Rome, ora legale, fascia di silenzio, impostazioni e marcatori di consegna persistenti. Nessuna registrazione di incasso o completamento automatico. Le pulizie programmate sono attività effettive, distinte dalle spese già avvenute. Android e i produttori possono ritardare gli allarmi; dopo arresto forzato occorre riaprire l'app.

Blocco facoltativo tramite AndroidX BiometricPrompt e credenziale sicura del dispositivo, con alternativa Keyguard sulle API precedenti. Attivazione, modifica e disattivazione richiedono autenticazione. Nessun PIN applicativo o dato biometrico conservato. Avvio a freddo bloccato e timeout in background configurabile. Notifiche aprono la destinazione solo dopo lo sblocco. Con protezione attiva Android impedisce screenshot e anteprime recenti. È controllo d'accesso, non cifratura del database o dei backup. Nessuna cancellazione dei dati dopo errori di riconoscimento.
