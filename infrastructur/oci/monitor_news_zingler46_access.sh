#!/bin/bash

# Pfad zur Logdatei (relativ zum Skript)
LOG_FILE="./logs/news_zingler46_access.log"

# Prüfen, ob jq installiert ist
if ! command -v jq &> /dev/null; then
    echo "Fehler: 'jq' ist nicht installiert."
    echo "Bitte installiere es zuerst:"
    echo "  Ubuntu/Debian: sudo apt-get install jq"
    echo "  Oracle Linux:  sudo dnf install jq"
    exit 1
fi

# Prüfen, ob die Logdatei existiert
if [ ! -f "$LOG_FILE" ]; then
    echo "Warnung: $LOG_FILE existiert noch nicht."
    echo "Warte darauf, dass Caddy startet und Logs schreibt..."
fi

echo "Starte Live-Monitor (Beenden mit Ctrl+C)..."
echo "---------------------------------------------------"

# Der Befehl
sudo tail -f "$LOG_FILE" | jq -r --unbuffered '
  "[" + (.ts | todate) + "] " +
  "User: " + (.user_id // "anon") + " | " +
  (.request.method) + " " + (.request.uri) + " -> " + (.status | tostring) + " | " +
  "IP: " + .request.remote_ip + " | " +
  "Agent: " + (.request.headers["User-Agent"][0] // "-")
'