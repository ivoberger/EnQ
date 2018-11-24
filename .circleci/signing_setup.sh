#!/usr/bin/env bash
cat <<EOF > keystore.properties
    storePassword=${SIGNING_KEYSTORE_PW}
    keyPassword=${SIGNING_KEY_PW}
    keyAlias=enq
    storeFile=keystore.jks
EOF
echo $SIGNING_KEYSTORE | base64 -d > app/keystore.jks