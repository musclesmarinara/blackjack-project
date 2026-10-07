#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
build_dir=$(mktemp -d)
trap 'rm -rf "$build_dir"' EXIT
src=app/src/main/java/com/example/blackjackoverlay
java -m jdk.compiler/com.sun.tools.javac.Main -d "$build_dir" "$src/CountEngine.java" "$src/Strategy.java" "$src/TableState.java" "$src/CardLayout.java" "$src/RankFilter.java" "$src/CornerSelector.java" "$src/ScanControl.java" "$src/HandGrouping.java" "$src/CardIdentity.java" "$src/TableComponents.java" "$src/TableTracker.java" tests/*.java
for test in CoreTest CardLayoutTest ScannerTest GroupingTest IdentityTest RoundRegressionTest TableTrackerTest TableComponentsTest CornerSelectorTest; do
 java -cp "$build_dir" "$test"
done
