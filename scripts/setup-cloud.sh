#!/usr/bin/env bash
set -euo pipefail
ELLEDUE_REPO="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
ELLEDUE_WORKSPACE="${ELLEDUE_WORKSPACE:-/workspace}"
export ELLEDUE_WORKSPACE
mkdir -p "$ELLEDUE_WORKSPACE/.toolchains" "$ELLEDUE_WORKSPACE/.android-home/cache" "$ELLEDUE_WORKSPACE/.gradle-home"
ELLEDUE_DOWNLOADS="$(mktemp -d /tmp/elledue-setup.XXXXXX)"
trap 'rm -rf "$ELLEDUE_DOWNLOADS"' EXIT
if [ ! -x "$ELLEDUE_WORKSPACE/.toolchains/jdk-21.0.10+7/bin/javac" ]; then
    curl --fail --location --retry 2 https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.10%2B7/OpenJDK21U-jdk_x64_linux_hotspot_21.0.10_7.tar.gz -o "$ELLEDUE_DOWNLOADS/jdk.tar.gz"
    printf '%s  %s\n' ea3b9bd464d6dd253e9a7accf59f7ccd2a36e4aa69640b7251e3370caef896a4 "$ELLEDUE_DOWNLOADS/jdk.tar.gz" | sha256sum -c -
    tar -xzf "$ELLEDUE_DOWNLOADS/jdk.tar.gz" -C "$ELLEDUE_WORKSPACE/.toolchains"
fi
if [ ! -x "$ELLEDUE_WORKSPACE/.android-sdk/cmdline-tools/19.0/bin/sdkmanager" ]; then
    curl --fail --location --retry 2 https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip -o "$ELLEDUE_DOWNLOADS/android-tools.zip"
    # Published checksum from Google's signed-through-HTTPS SDK repository metadata.
    printf '%s  %s\n' 5fdcc763663eefb86a5b8879697aa6088b041e70 "$ELLEDUE_DOWNLOADS/android-tools.zip" | sha1sum -c -
    mkdir -p "$ELLEDUE_WORKSPACE/.android-sdk/cmdline-tools"
    unzip -q "$ELLEDUE_DOWNLOADS/android-tools.zip" -d "$ELLEDUE_DOWNLOADS/android"
    mv "$ELLEDUE_DOWNLOADS/android/cmdline-tools" "$ELLEDUE_WORKSPACE/.android-sdk/cmdline-tools/19.0"
fi
source "$ELLEDUE_REPO/scripts/cloud-env.sh"
python3 - <<'PYCONFIG'
import os, urllib.parse
from pathlib import Path
root = Path(os.environ['ELLEDUE_WORKSPACE'])
p = root / '.gradle-home/gradle.properties'
keys = ['systemProp.https.proxyHost', 'systemProp.https.proxyPort', 'systemProp.http.proxyHost', 'systemProp.http.proxyPort', 'systemProp.javax.net.ssl.trustStore', 'org.gradle.jvmargs']
lines = p.read_text().splitlines() if p.exists() else []
lines = [line for line in lines if line.split('=', 1)[0] not in keys]
u = urllib.parse.urlsplit(os.environ.get('HTTPS_PROXY', os.environ.get('https_proxy', '')))
if u.username: raise SystemExit('Do not persist proxy credentials.')
if u.hostname:
    for protocol in ('http', 'https'):
        lines.extend([f'systemProp.{protocol}.proxyHost={u.hostname}', f'systemProp.{protocol}.proxyPort={u.port or 80}'])
if Path('/etc/ssl/certs/java/cacerts').is_file():
    lines.append('systemProp.javax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts')
lines.append(f'org.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8 -Duser.home={root}/.android-home')
p.write_text('\n'.join(lines) + '\n')
PYCONFIG
python3 - <<'PYLICENSE'
import os, subprocess
sdk = os.environ['ANDROID_HOME']
p = subprocess.run([sdk + '/cmdline-tools/19.0/bin/sdkmanager', '--sdk_root=' + sdk, '--licenses'], input='y\n'*100, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
if p.returncode: print(p.stdout); raise SystemExit(p.returncode)
print('Android SDK licenses accepted for development.')
PYLICENSE
sdkmanager --sdk_root="$ANDROID_HOME" 'platforms;android-35' 'build-tools;35.0.0' 'platform-tools'
# Respect an existing local SDK path; do not replace user configuration.
if [ ! -e "$ELLEDUE_REPO/local.properties" ]; then
    printf 'sdk.dir=%s\n' "$ANDROID_HOME" > "$ELLEDUE_REPO/local.properties"
fi
cd "$ELLEDUE_REPO"
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
