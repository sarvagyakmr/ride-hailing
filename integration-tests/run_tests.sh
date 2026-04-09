#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────
# Run integration tests for the ride-match module.
#
# Prerequisites:
#   1. ride-match service is running on localhost:8080
#   2. Python 3.8+ is installed
#
# Usage:
#   ./run_tests.sh              # run all tests
#   ./run_tests.sh -k "health"  # run only health tests
#   ./run_tests.sh --html=report.html  # generate HTML report
# ──────────────────────────────────────────────────────────────

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "📦 Installing dependencies..."
pip install -q -r "$SCRIPT_DIR/requirements.txt"

echo ""
echo "🚀 Running integration tests..."
echo "   Target: http://localhost:8080"
echo ""

python -m pytest "$SCRIPT_DIR" "$@"
