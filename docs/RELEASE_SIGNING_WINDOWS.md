# Firma release sul PC Windows

La chiave definitiva deve essere creata e custodita dall’utente. Il progetto non genera chiavi release nel cloud. L’application ID resta `it.elledue.control`.

1. Sul PC, aprire il progetto in Android Studio; verificare JDK e SDK indicati nel README.
2. Menu **Build → Generate Signed App Bundle or APK → APK → Next → Create new**.
3. Scegliere una cartella privata esterna al repository, ad esempio `C:\Users\TUO_UTENTE\Documents\ElleduePrivate\elledue-release.jks`. Non usare cartelle del checkout o consegne ZIP dei sorgenti.
4. Creare alias `elledue-release`, password robuste, validità almeno 25 anni e dati del titolare. Conservare password e alias in un password manager separato dal file.
5. Completare la procedura usando build variant `release`; verificare il certificato dell’APK prima di distribuirlo.
6. Conservare una seconda copia privata del keystore in un archivio o volume cifrato, su un supporto distinto. Conservare la password della copia separatamente. Provare il recupero della copia e verificare che restituisca lo stesso certificato. Nessuna copia su GitHub.
7. Utilizzare **sempre lo stesso keystore e alias** per gli aggiornamenti. Non creare una chiave nuova a ogni versione.

## Gradle con keystore esterno

La configurazione legge queste variabili dell’ambiente del processo, senza scrivere segreti nel repository:

- `ELLEDUE_RELEASE_STORE`: percorso assoluto del keystore privato.
- `ELLEDUE_RELEASE_ALIAS`: alias.
- `ELLEDUE_RELEASE_STORE_PASSWORD`: password dello store.
- `ELLEDUE_RELEASE_KEY_PASSWORD`: password della chiave.

In PowerShell si possono impostare store e alias e acquisire le password interattivamente, evitando valori nel file o cronologia:

```powershell
$env:ELLEDUE_RELEASE_STORE = 'C:\Users\TUO_UTENTE\Documents\ElleduePrivate\elledue-release.jks'
$env:ELLEDUE_RELEASE_ALIAS = 'elledue-release'
$env:ELLEDUE_RELEASE_STORE_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Password keystore' -AsSecureString)).Password
$env:ELLEDUE_RELEASE_KEY_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Password chiave' -AsSecureString)).Password
.\gradlew.bat :app:assembleRelease
Remove-Item Env:ELLEDUE_RELEASE_STORE_PASSWORD
Remove-Item Env:ELLEDUE_RELEASE_KEY_PASSWORD
```

Le password sono necessarie nel processo Gradle per firmare, ma non vengono stampate dalle istruzioni. Chiudere la sessione dopo la compilazione. Il task `verifyReleaseSigning` blocca il pacchetto release senza configurazione completa; rifiutati keystore dentro il repository. `assembleDebug` resta disponibile senza chiave release.

## Verificare i certificati

Dal terminale Android Studio / PowerShell, usando il percorso effettivo del proprio SDK:

```powershell
& "$env:ANDROID_HOME\build-tools\35.0.0\apksigner.bat" verify --verbose --print-certs 'C:\Percorso\versione-precedente.apk'
& "$env:ANDROID_HOME\build-tools\35.0.0\apksigner.bat" verify --verbose --print-certs 'C:\Percorso\versione-nuova.apk'
```

Confrontare **Signer certificate SHA-256 digest**: deve coincidere. Il checksum SHA-256 dell’intero APK cambia normalmente tra versioni e non è il certificato. Annotare il certificato in un registro privato di distribuzione. Il certificato è pubblico, ma il keystore e le password sono privati.

## Aggiornamenti e passaggio da debug a release

Prima di aggiornare: esportare il backup dall’app e conservarlo privatamente. Verificare application ID, versionCode maggiore e certificato identico; installare l’APK nuovo sopra l’installazione esistente, senza disinstallare. Le migrazioni Room gestiscono i dati.

Una release firmata con la chiave definitiva **non può aggiornare direttamente** la precedente app con firma debug. Per il primo passaggio: esportare e verificare il backup, provarne il ripristino su un dispositivo di prova, poi sostituire l’installazione debug e importare il backup nella release. Non disinstallare l’unica copia dei dati prima della prova di recupero.

La build cloud consegna solo APK debug. Il progetto non è dichiarato pronto per distribuzione aziendale stabile finché la chiave definitiva e la copia recuperabile non sono sotto il controllo dell’utente e un aggiornamento reale su dispositivo non è stato verificato. I test Room/backup non equivalgono a quella prova Android.
