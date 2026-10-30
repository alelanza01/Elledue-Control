# Evidenze grafiche e di verifica 0.4.8

Screenshot del codice Android eseguito con Skia nativa in Robolectric API 34, non mockup e non fotografie di un telefono fisico. I dati sono esclusivamente sintetici. Le schermate protette sono renderizzate dal test; nell'app installata FLAG_SECURE impedisce screenshot e anteprime quando il blocco è attivo.

| Area | Screenshot |
|---|---|
| Home | [Home](home.png) |
| Calendario | [Portfolio](calendar.png), [singolo immobile](calendar-single-property.png), [320dp / testo 150%](calendar-320dp-font150.png) |
| Economia | [Panoramica](economy.png), [risultato immobili propri](owned-result.png) |
| Analisi | [Compensi e confronto](analysis.png), [ripartizione spese](analysis-expenses.png) |
| Immobile proprio | [Dettaglio](owned-property.png) |
| Eliminazione | [Prima del backup](deletion-before-backup.png), [backup verificato e conferma](deletion-verified.png) |
| Prenotazione | [Scheda e importi](booking.png) |
| Rendiconto | [Bozza](report-draft.png), [dettagli e opzioni](report-details.png), [anteprima del PDF](pdf-preview.png) |
| Notifiche | [Impostazioni Android](notification-settings.png) |
| Accesso | [Schermata bloccata 320dp / testo 130%](access-320dp-font130.png) |

[PDF sintetico](Elledue-rendiconto-sintetico.pdf) prodotto dal generatore reale. Il rendiconto privato fornito dall'utente non è incluso.

Verifiche: [esiti per suite](test-results.json), [log build/test/lint](validation.log), [lint completo](lint-results.txt), [verifica firma APK](apk-signature.txt), [metadati APK](apk-badging.txt), [manifesto con checksum](manifest.json). Il manifesto distingue percorso locale, bozza GitHub e download non disponibile.
