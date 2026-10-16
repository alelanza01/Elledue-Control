# 0.4.8 — stato in lavorazione

Base verificata ea7e654, branch feature/0.4.8-stabilization. Nessun lavoro locale preesistente da sovrascrivere; main c156458 e branch artefatti0.4.7 distinti. Application ID preservato; versione0.4.8/code8. Non distribuire pubblicamente, non fare merge.

CheckpointA in corso: bozze persistenti private per modulo/record, errori di salvataggio visibili, rettifica economica esplicita, eliminazione dipendenze completa e atomica con backup SAF verificato e controllo dati cambiati.

Evidenza iniziale sul salvataggio: il repository già aggiorna per ID con Room Upsert in transazione. Le rettifiche economiche senza motivo e le capienze insufficienti sono rifiutate; l’errore veniva mostrato solo in cima a un modulo scorrevole, spesso fuori viewport. I record rendicontati richiedono una rettifica. La0.4.8 non aggira questi controlli: presenta motivo/conseguenze e rende visibili gli errori. Il caso completo deve ancora essere validato con test a cinque campi e riapertura reale del database.

Modello7 additivo predisposto per tipologia immobili/prenotazioni, previsione fiscale provvisoria e opzioni rendiconti. Classificazione precedenteMANAGED, nessun ricalcolo storico automatico; immobili propri nuovi senza fee inventate. CheckpointB/C e QA non ancora completati: questo documento non costituisce una consegna.
