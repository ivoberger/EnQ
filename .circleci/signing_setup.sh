#!/usr/bin/env bash
echo $SIGNING_KEYSTORE | base64 -d > keystore.jks
echo $PLAY_CREDENTIALS > app/play_credentials.json
