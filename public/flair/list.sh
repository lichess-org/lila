#!/bin/sh

# Create list.txt from img/*.webp

pushd "$(dirname "$0")"
ls img/*.webp |
  grep -v 'symbols.cancel' |
  grep -v 'objects.prayer-beads' |
  sed 's/.webp//g' |
  sed 's/img\///g' >list.txt
popd

echo "Done creating flair/list.txt."
