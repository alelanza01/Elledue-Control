# Evidenze 0.4.7

136 test JUnit/Robolectric superati;0 fallimenti,0 errori,0 saltati. Esecuzione completa:

```bash
cd /workspace/Elledue-Control
source scripts/cloud-env.sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Esito effettivo BUILD SUCCESSFUL (1m3s). Lint:0 errori,6 avvisi preesistenti non soppressi:3 helper TLS EST di BouncyCastle non usati dall’app, ExifInterface piattaforma,2 consigli Bitmap KTX. Nessun permesso Internet. Nessuna minificazione/release firmata nuovamente eseguita nella0.4.7.

Le immagini sono acquisizioni della UI Android effettiva tramite View.draw/Skia in Robolectric API34, non mockup o immagini AI di interfaccia. Solo dati fittizi inseriti nei database isolati dei test. Sono stati aperti e ispezionati visivamente i PNG delle schermate nuove e il PDF renderizzato con PyMuPDF; non è collaudo manuale su un telefono. La suite include320dp/font150% e landscape891×411dp/font130%, con app scura anche quando Android è chiaro. Le copertine artistiche originali sono asset dell’interfaccia; le foto di immobili e operatori dei test non vengono introdotte nella base dati normale.

- [Galleria delle nuove schermate](galleria.png)
- [Home](screenshots/047-home.png)
- [Form attività](screenshots/047-task-editor.png)
- [Task nel calendario](screenshots/047-task-calendar.png)
- [Copia calendario](screenshots/047-copy-calendar.png)
- [Selettore dei quattro portali](screenshots/047-portal-selector.png)
- [Analisi con dati effettivi del test](screenshots/047-analysis.png)
- [Messaggio inglese risolto e copiabile](screenshots/047-message-preview.png)
- [Aspetto e tre copertine](screenshots/047-appearance.png)
- [Calendario copiabile in landscape](screenshots/047-landscape-copy.png)
- [PDF sintetico esclusivamente fittizio, saldo170€](rendiconto-sintetico-dati-fittizi.pdf)

Il PDF sintetico separa pulizie24€, lavanderia10€, altre spese5€, totale rimborsi39€ e compenso161€, meno compensazione30€: saldo170€. Il costo aggiuntivo di pulizia900€ del test è non rimborsabile ed escluso dal saldo. Il PDF è reale, una pagina; i test regressione esercitano anche il documento analitico, rettifiche storiche e stress multipagina con85 soggiorni e testi lunghi.

L’APK compilato rimane `/workspace/artifacts/Elledue-Control-0.4.7-debug.apk`,30.468.426 byte. Il manifest riporta SHA256, certificato e commit sorgente. Nessun APK0.4.7 è pubblicato su GitHub o in una release. L’APK è debug, application ID it.elledue.control, versionName0.4.7/code7, Android8+. Certificato identico alla0.4.6 consegnata; installazione e aggiornamento su telefono fisico restano da collaudare seguendo [UPDATE.md](../UPDATE.md), senza disinstallare.

I report XML/HTML completi sono generati in app/build/test-results/testDebugUnitTest e app/build/reports/tests/testDebugUnitTest; il lint in app/build/reports/lint-results-debug.html. Il manifest pubblico descrive anche la posizione locale dell’APK senza includerne il file.
