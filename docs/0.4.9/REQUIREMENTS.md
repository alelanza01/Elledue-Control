# Matrice di verifica 0.4.9

Il [brief completo](BRIEF.md) rimane il riferimento. Le prove automatiche sono eseguite su JVM/Robolectric Android API 34; non equivalgono a prove su telefono fisico.

| Ambito | Implementazione | Verifica |
|---|---|---|
| Preservazione dati | Migrazione Room 7→8 additiva; catena 1→8; niente fallback distruttivo | ConsolidatedMigrationTest e test di migrazione precedenti, con fixture degli schemi pubblicati |
| Backup | Formato portabile 1, versioni database 4–8, staging e validazione prima del ripristino | BackupServiceTest, ConsolidatedDataTest.versionSevenBackupRestoresAndPreservesHistoricalReportBytes, ProductArchiveTest |
| Nuova prenotazione | Nessun immobile preselezionato; immobili archiviati esclusi; modifica conserva immobile | ConsolidatedUiTest, StabilityDataTest, ProductArchiveTest |
| Date | Un solo DateRangePicker Material; date testuali accessibili; notti e validazione intervallo | ConsolidatedUiTest, ReservationUiTest, BookingDatesTest |
| Stato | Derivato da date e orari previsti/effettivi, cancellazione prioritaria; nessuna modifica allo stato contabile | ConsolidatedDataTest, ReservationUiTest |
| OTA | Default 19,5% Booking/Airbnb/Vrbo, diretta 0; configurazione portale prima del default; manuale prevalente | ConsolidatedDataTest, OperationsRepositoryTest, OperationsUiTest, BookingFinanceTest |
| Lavanderia | Scaglione configurabile per immobile; ceil(ospiti/scaglione); override persistente | ConsolidatedDataTest.guestChangesRecalculateLaundryUntilOverrideWithoutChangingConfirmedCosts |
| Pulizie | 2 ore proposte alla tariffa per immobile; previsione separata dal costo sostenuto | ConsolidatedDataTest.forecastsAreNotCostsAndCleaningConfirmationIsOneExpenseWithActualOverride, ConsolidatedUiTest |
| Consuntivi | Una sola spesa con ID stabile; rettifica della spesa già registrata; avvisi sui consuntivi mancanti nel report | ConsolidatedDataTest, AccountingRepositoryTest, ReportRepositoryTest |
| Immobili propri | Nessun compenso automatico né saldo con proprietario esterno; risultati privati separati | BusinessModelTest, OwnershipUiTest, ConsolidatedDataTest |
| Trasporti | Categoria distinta, importo abituale facoltativo per immobile, quantità/tariffa modificabili | ConsolidatedDataTest.ownPropertyHasTransportCategoryAndNoAutomaticManagementOrCostForecasts; ExpenseEditor riutilizzato |
| Tassa | Esenti come quantità, default nuovi immobili 3€/notte, 16 anni, max4; vecchie regole conservate | ConsolidatedDataTest.taxThirtySixIsForecastNotCollectionAndPartialCollectionKeepsCorrectResidual, AccountingRepositoryTest |
| Movimenti tassa | Riscossione manuale anche parziale; compensazioni e riversamenti separati; revisioni e storni | AccountingRepositoryTest, OperationalAccountingTest, AccountingUiTest |
| Riepilogo soggiorno | Dati registrati, previsioni/consuntivi, tassa, attività/problemi e azioni con collegamento già impostato | ConsolidatedUiTest, StabilityUiTest, ReservationUiTest |
| Problemi | Stato, priorità, filtri, incarico, date facoltative, risoluzione e riapertura; nessun costo automatico | ConsolidatedDataTest, ConsolidatedUiTest |
| Dipendenze problemi | L'immobile rimane proprietario del problema; eliminazione prenotazione scollega; spesa collegata senza duplicazione | ConsolidatedDataTest.bookingDeletionDetachesIssueInsteadOfRemovingItAndBackupRoundTripsAllNewData |
| Giornata operativa | Tap data; cronologia con arrivi/partenze/attività/interventi; senza ora in fondo; cambi ospite | ConsolidatedDataTest, ConsolidatedUiTest |
| Eventi contestuali | Soggiorno, attività, problema, conferma ore pulizia e promemoria ospite aprono il percorso pertinente | ConsolidatedUiTest, OperationalDay, OperationsUiTest |
| Calendario | Bande settimanali con nome immobile, orari, segnali testuali; colori immobile distinti dai colori semantici | ConsolidatedUiTest, ReservationUiTest; screenshot nativi sintetici |
| Copia calendario | Check-out >= data copia, date originali, ordinamento, nessun limite futuro | ConsolidatedDataTest, GuestTextTest, OperationsUiTest |
| Occupazione | Notti nel mese effettivo, check-out escluso, sovrapposizioni deduplicate; contabilità invariata | ConsolidatedDataTest, ProductArchiveTest |
| Rendiconti | Motore unico; OTA accanto al prezzo; lordo/OTA/netto/percentuali, categorie di rimborso, saldo | OperationsPdfTest, ReportPdfTest, ProductArchiveTest, ReportRepositoryTest |
| Esclusioni dal report | Opzione per singolo documento, motivazione e storico interni, fonte conservata, versione confermata immutabile | ConsolidatedDataTest, BusinessModelTest, VisibilityPdfTest |
| Notifiche programmate | Allarme→PendingIntent→Receiver→notifica, consegna differita, deduplica, riarmo, permessi | ConsolidatedReminderTest e ReminderEngineTest; limiti fisici in QA.md |
| Bug notifiche reali | La riconciliazione non cancella più una notifica appena consegnata | ConsolidatedReminderTest.scheduledPendingIntentReallyRunsReceiverAndPublishesNotificationWithoutCallingTestButton |
| Messaggi da notifica | Modello IT/EN pertinente, variabili, testo modificabile, copia/condivisione manuale, Gestito esplicito | ConsolidatedUiTest, GuestTextTest; tassa zero elimina la riga col relativo segnaposto |
| Protezione | API biometriche/credenziale dispositivo esistenti, fallback, ritorno da background e deep link; notifiche senza dettagli riservati con blocco attivo | AccessUiTest, ReminderEngineTest, ConsolidatedReminderTest; sensore fisico non disponibile |
| Analytics | Schede Operatività/Economia/Elledue privata, occupazione reale, confronti e trend | ConsolidatedUiTest, OperationsUiTest, BusinessModelTest |
| Archiviazione/eliminazione | Archiviati esclusi dalle operazioni; storico esplicito; backup preventivo verificato; fonti di rendiconti confermati non eliminabili | StabilityDataTest, ProductArchiveTest, StabilityUiTest |
| Bozze e navigazione | Salvataggio privato durevole, isolamento per record, pulizia dopo successo, ritorno all'origine | StabilityUiTest, StabilityDataTest, AppNavigationTest; correzioni 0.4.8 conservate |
