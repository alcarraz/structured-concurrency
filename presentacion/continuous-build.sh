#!/bin/bash

echo "first build"
make quick
echo "Watching for .tex file changes..."
echo "Press Ctrl+C to stop"

inotifywait -m -r -e modify,create,move --include '.*\.(tex|cls)$' --format '%w%f' . | while read file
do
    echo "$(date '+%Y-%m-%d %H:%M:%S'): Detected change in $file"
    make quick
    echo "$(date '+%Y-%m-%d %H:%M:%S'): presentation compiled"
    
done
