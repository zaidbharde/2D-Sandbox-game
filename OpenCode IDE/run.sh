#!/bin/bash

echo "=========================================="
echo "    OpenCode IDE"
echo "=========================================="
echo ""

cd "$(dirname "$0")"

if [ ! -d "build/classes/ide" ]; then
    echo "Project not built. Running build.sh first..."
    ./build.sh
fi

echo "Starting OpenCode IDE..."
echo ""
java -cp build/classes ide.OpenCodeIDE
