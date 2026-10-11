#!/usr/bin/env bash
# Source this file in cloud shells; never store credentials here.
ELLEDUE_WORKSPACE="${ELLEDUE_WORKSPACE:-/workspace}"
export JAVA_HOME="$ELLEDUE_WORKSPACE/.toolchains/jdk-21.0.10+7"
export ANDROID_HOME="$ELLEDUE_WORKSPACE/.android-sdk"
export ANDROID_USER_HOME="$ELLEDUE_WORKSPACE/.android-home"
export GRADLE_USER_HOME="$ELLEDUE_WORKSPACE/.gradle-home"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/19.0/bin:$PATH"
ELLEDUE_JAVA_PROXY_FLAGS="$(python3 - <<'PYPROXY'
import os, urllib.parse
u = urllib.parse.urlsplit(os.environ.get('HTTPS_PROXY', os.environ.get('https_proxy', '')))
if u.username:
    raise SystemExit('Authenticated proxy needs supported platform routing; do not copy credentials.')
if u.hostname:
    print(f'-Dhttps.proxyHost={u.hostname} -Dhttps.proxyPort={u.port or 80} -Dhttp.proxyHost={u.hostname} -Dhttp.proxyPort={u.port or 80}')
PYPROXY
)"
export JAVA_OPTS="${JAVA_OPTS:-} $ELLEDUE_JAVA_PROXY_FLAGS -Duser.home=$ANDROID_USER_HOME"
if [ -f /etc/ssl/certs/java/cacerts ]; then
    export JAVA_OPTS="$JAVA_OPTS -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"
fi
unset ELLEDUE_JAVA_PROXY_FLAGS
