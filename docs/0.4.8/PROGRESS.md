# 0.4.8 — checkpoint sorgenti

Base verificata `ea7e654`, branch `feature/0.4.8-stabilization`. Nessuna sostituzione del progetto o delle modifiche preesistenti; main `c156458` e artefatti 0.4.7 separati. Application ID invariato; versione 0.4.8 / code 8. Nessun merge o APK pubblico.

Checkpoint A/B/C implementati: bozze private persistenti isolate per record, controllo revisione al salvataggio, rettifica economica guidata ed errori visibili, eliminazione completa delle dipendenze in transazione con backup SAF riletto prima della cancellazione, tipologia immobili e riepiloghi separati, archivio escluso dalle query operative, previsione fiscale provvisoria, opzioni PDF indipendenti, calendario a barre, notifiche Android e accesso protetto.

Verificati in gruppi mirati: modifica di più di cinque campi e riapertura del database su file; recupero di bozza dopo chiusura di tutte le Activity e nuovo avvio, salvataggio e riapertura della scheda; conservazione di bozze di altri record; rollback di eliminazione, indisponibilità del backup e dipendenze; modelli economici; allarmi, permessi, notifiche e intent; credenziale Android annullata/riuscita e schermata bloccata; PDF effettivi con riga nascosta e totali conservati; regressioni delle viste native.

La diagnosi del salvataggio non afferma di avere riprodotto il telefono dell'utente: il precedente repository aggiornava già per UUID, ma motivo di rettifica, capienza e vincolo sui rendiconti potevano respingere il modulo con un errore fuori viewport. La 0.4.8 rende espliciti questi casi. Durante il recupero delle bozze è stato inoltre corretto il ritorno alla scheda aggiornata dopo il salvataggio, anche quando l'editor proveniva dall'elenco bozze.

Verifica finale dell'intera suite, lint, APK e firma in corso. Questa nota è un checkpoint, non la dichiarazione di collaudo finale su telefono fisico. Vedere ARCHITECTURE.md e, a verifica conclusa, QA.md.
