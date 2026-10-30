# Tracciabilità requisiti

Riferimento completo: [BRIEF.md](BRIEF.md). Le regole e le scelte sui dati storici sono spiegate in [ACCOUNTING.md](ACCOUNTING.md); migrazione e installazione in [MIGRATION.md](MIGRATION.md).

| Richiesta | Realizzazione | Prove |
|---|---|---|
| Base aggiornata | Branch dedicato dalla 0.4.9, confronto remoto, conservazione main e artefatti precedenti | Log Git e commit della consegna |
| Lavanderia 1–10 ospiti | Formula intera, scaglione/tariffa per immobile, override intenzionali | Automation0410Test.laundryOneThroughTenGuestsAndCustomGroupingUseExactIntegerCents |
| Pulizia ordinaria 16 € | 2 ore × tariffa immobile, standard iniziale 8 €/h, solo immobili in gestione | Automation0410Test e ConsolidatedDataTest |
| Retroattività | Passaggio transazionale all'avvio e dopo vecchio restore, marcatore, idempotenza | retroactiveBackfillIsIdempotentTransactionalAndCreatesNoExpensesOrCash, legacyEightBackupBackfillsMissingRowsAndPreservesManualCostOnRestore |
| Override e costi reali | Nessun ricalcolo di consuntivi/override; aggiornamento delle sole previsioni automatiche | cancelledOwnedManualOverridesAndActualCostsSurviveRepeatedReconciliation, configurationChangesUpdateAutomaticEstimatesButNeverConsolidatedTaxOrManualOverrides |
| Standard nei rendiconti | Totali e PDF delle bozze includono voci proposte, approvazione aggregata tracciata | proposalsAreInBothDraftPdfsButNotPostedUntilExplicitBulkApproval, Automation0410UiTest |
| Nessuna falsa attestazione | Origine e stato espliciti, data prevista distinta; consuntivo confermato separatamente | changingAnApprovedStandardDoesNotAttestActualServiceWithoutExplicitConfirmation |
| Nessun duplicato | ID/origine per prenotazione e tipo, guardie sulle spese manuali e su conferma | aManualServiceCannotBeAddedAgainAfterStandardApproval, failedBulkApprovalRollsBackEveryCostAndItsAuditThenCanBeRetried |
| Mesi differenti | Standard seguono soggiorno; vecchia spesa conserva il periodo ed è collegata informativamente | actualDecemberCostAppearsAsLinkedInformationInNovemberWithoutDoubleCounting, approvalUsesBookingCompetenceAndKeepsExpectedInterventionDateThenAllowsRevision |
| Tassa ordinaria | Esenti come quantità, regole per immobile, massimo notti applicato a ciascun ospite | taxMatrixCapsNightsPerGuestAndPreservesAllExemptionsAndCustomRates |
| Tassa storica | Dati attendibili solamente; nessuna esenzione inventata o riscrittura di imposte consolidate | unknownHistoricalExemptionsRemainUnknownAndConsolidatedTaxIsUntouched |
| Incasso semplice | Riquadro nella prenotazione, residuo precompilato, parziale, soggetto, compensazione facoltativa | quickCollectionAndCompensationAreAtomicIdempotentAndReversible, ownerOrPortalCollectionCannotCreateElledueAdvance, Automation0410UiTest |
| Form spese | Base breve + Altri dettagli, quantità/tariffa solo quando pertinenti, contesto soggiorno | ordinaryExpenseUsesShortFormPrefilledStayAndExpandableDetails e regressioni AccountingUiTest |
| Un calendario date | Due campi azionabili, stesso picker e nessun terzo pulsante | dateFieldsOpenTheSameRangePickerAndThereIsNoThirdButton, OperationsUiTest |
| Mese compatto | Griglia 5/6 settimane con indicatori, dettaglio nella giornata | compactMonthIsBoundedWithLargeTextAndDayOpens, limite misurato <420 dp a 320 dp/font150% |
| Riepilogo soggiorno | Economia, standard, tassa, dettagli espandibili e azioni contestuali | Automation0410UiTest, ReservationUiTest, StabilityUiTest |
| Bozze aggiornate | Osservazione delle fonti, refresh automatico all'ingresso/ripresa, controllo hash alla conferma | ReportRepositoryTest, ReportUiTest, Automation0410Test |
| Home e `+` | Priorità reali cliccabili e creazioni principali da ogni contesto con immobili attivi | centralPlusKeepsAllCoreActionsAvailableFromEconomy e regressioni di navigazione |
| Backup e migrazioni | Catena degli schemi pubblicati, schema8 reale, restore4–9 e audit | AutomationMigrationTest, test Migration precedenti, approvedBatchAndProposalsSurviveVerifiedBackupRestore |
| Regole storiche | Snapshot confermati e versioni precedenti non riscritti | oldConfirmedSourcesAreNotBackfilledOrRewritten, ReportRepositoryTest |
| Consegna reale | APK, metadati, firma precedente, SHA e download effettivo | QA.md e file di verifica in qa/ |
| PR | Descrizione pronta; creazione API tentata e blocco del tunnel riportato | PR.md; non viene dichiarata una PR inesistente |
