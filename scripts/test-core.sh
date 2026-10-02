#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/core-check
javac --release 8 -d build/core-check src/main/java/dev/cake/rawcost/Planner.java tests/core/PlannerChecks.java
java -cp build/core-check dev.cake.rawcost.PlannerChecks
