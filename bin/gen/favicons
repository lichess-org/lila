#!/bin/sh

file=public/logo/lishogi-favicon-1024.png
thumb_file=$2

for px in 512 256 192 128 64; do
  convert $file -scale ${px}x${px} public/logo/lishogi-favicon-${px}.png
done
