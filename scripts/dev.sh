#!/bin/sh
set -e

if ! command -v java >/dev/null 2>&1 || ! command -v mvn >/dev/null 2>&1 || { [ -z "$DISPLAY" ] && ! command -v xvfb-run >/dev/null 2>&1; }; then
  echo "Installing Java 17, Maven, and Xvfb..."
  apt-get update -qq
  DEBIAN_FRONTEND=noninteractive apt-get install -y -qq --no-install-recommends openjdk-17-jdk maven xvfb
fi

if [ ! -f target/classes/Main.class ] || [ ! -f target/classpath.txt ]; then
  echo "Compiling Stock Price Predictor (Java 17 + AWT + SQLite JDBC)..."
  mvn -B -q compile dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
fi

CP="target/classes:$(cat target/classpath.txt)"
PORT_ARG="--preview-port=${PORT:-3000}"

if [ -z "$DISPLAY" ] && command -v xvfb-run >/dev/null 2>&1; then
  exec xvfb-run -a java -cp "$CP" Main "$PORT_ARG"
else
  exec java -cp "$CP" Main "$PORT_ARG"
fi
