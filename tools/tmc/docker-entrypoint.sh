#!/bin/sh
set -e

PROJECT_DIR=/app

if [ ! -w "$PROJECT_DIR" ]; then
  PROJECT_DIR="${TMC_WORK_DIR:-/tmp/tmc}"
  mkdir -p "$PROJECT_DIR"
  cp -R /app/. "$PROJECT_DIR/"
fi

exec /app/gradlew --project-dir "$PROJECT_DIR" "$@"
