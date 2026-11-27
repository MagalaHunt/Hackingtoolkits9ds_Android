#!/usr/bin/env sh
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
OUT="${TMPDIR:-/tmp}/packenglish-mobile-test-classes"
rm -rf "$OUT"
mkdir -p "$OUT"
javac -d "$OUT" \
  "$ROOT/app/src/main/java/id/packenglish/mobile/NcsdInspector.java" \
  "$ROOT/tests/NcsdInspectorSmokeTest.java"
java -cp "$OUT" NcsdInspectorSmokeTest
