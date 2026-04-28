#!/bin/bash
# ─────────────────────────────────────────────────────────────────
# run.sh  –  Run TC1 + TC2 in parallel on Amazon.com
# Usage:   ./run.sh
# ─────────────────────────────────────────────────────────────────

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MVN="$SCRIPT_DIR/apache-maven-3.9.6/bin/mvn"

echo ""
echo "═══════════════════════════════════════════════════"
echo "  Amazon Selenium Tests – TestMu AI Assignment"
echo "  TC1 (iPhone) + TC2 (Galaxy) running in parallel"
echo "═══════════════════════════════════════════════════"
echo ""

"$MVN" test -DsuiteFile=testng.xml "$@"
