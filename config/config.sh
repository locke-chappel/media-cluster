#!/bin/sh

JAR_DATA="JAR_BASE64_DATA"
TEMP_FILE=$(mktemp /tmp/config.XXXXXXXXXX.jar)

# Make sure we always clean up the temp file
trap "rm -f $TEMP_FILE" 0 2 3 15

echo "$JAR_DATA" | base64 -d > "$TEMP_FILE"

java -jar "$TEMP_FILE"
