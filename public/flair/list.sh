#!/bin/bash

# Create list.txt from img/*.webp

pushd "$(dirname "$0")"
ls img/*.webp | sed 's/.webp//g' | sed 's/img\///g' >list.txt
popd

echo "Done creating flair/list.txt."
