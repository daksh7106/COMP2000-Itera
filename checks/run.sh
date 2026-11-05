#!/bin/sh
set -eu

# Run from the repository root even when invoked from another folder.
cd "$(dirname "$0")/.."
build_dir=$(mktemp -d "${TMPDIR:-/tmp}/itera-checks.XXXXXX")
trap 'rm -rf "$build_dir"' 0
trap 'exit 130' INT
trap 'exit 143' TERM

javac -d "$build_dir" -sourcepath Project Project/itera/ui/Main.java checks/*Checks.java

for source in checks/*Checks.java; do
    suite=${source##*/}
    suite=${suite%.java}
    printf '\nRunning %s\n' "$suite"
    java -Djava.awt.headless=true -cp "$build_dir" "itera.checks.$suite"
done
