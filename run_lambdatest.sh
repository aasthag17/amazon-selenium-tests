#!/bin/bash
# ─────────────────────────────────────────────────────────────────
# run_lambdatest.sh – Run tests sequentially on LambdaTest Cloud
# ─────────────────────────────────────────────────────────────────

export LT_USERNAME="aasthagupta0604"
export LT_ACCESS_KEY="LT_W9YoxlMlPXm3xWLeLGzvKtPrExrPFdSftgXG2Dd2yGyMAv3"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MVN="$SCRIPT_DIR/apache-maven-3.9.6/bin/mvn"

echo ""
echo "═══════════════════════════════════════════════════"
echo "  Amazon Selenium Tests – LambdaTest Cloud"
echo "  (Running sequentially to respect free tier limits)"
echo "═══════════════════════════════════════════════════"
echo ""

"$MVN" clean test -P lambdatest "$@"
