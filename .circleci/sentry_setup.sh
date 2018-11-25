#!/usr/bin/env bash
echo "Setting environment variables"
export SENTRY_FILE=app/src/main/resources/sentry.properties
export SENTRY_PG_FILE=sentry.properties
echo "Setting version"
export APP_VERSION=`sed -n 's/^\s*versionName\s//p' app/build.gradle | sed 's/"//g'`
echo "App version is ${APP_VERSION}"
echo "Writing ${SENTRY_FILE}"
mkdir -p app/src/main/resources
cat <<EOF > ${SENTRY_FILE}
dsn=${SENTRY_DSN}
stacktrace.app.packages=me.iberger.enq
release=${APP_VERSION}
EOF
echo "Writing ${SENTRY_PG_FILE}"
cat <<EOF > ${SENTRY_PG_FILE}
defaults.project=enq
defaults.org=personal_projects
auth.token=${SENTRY_API_TOKEN}
EOF
