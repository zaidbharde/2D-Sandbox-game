#!/bin/bash

echo "=========================================="
echo "    OpenCode IDE - Build Script"
echo "=========================================="
echo ""

cd "$(dirname "$0")"

echo "[1/2] Creating directories..."
mkdir -p build/classes
mkdir -p dist

echo "[2/2] Compiling Java files..."
javac -d build/classes -sourcepath src -encoding UTF-8 src/ide/*.java

if [ $? -ne 0 ]; then
    echo ""
    echo "Build failed!"
    exit 1
fi

echo ""
echo "=========================================="
echo "    Build successful!"
echo "=========================================="
echo ""
echo "To run the IDE, use: ./run.sh"
echo ""
