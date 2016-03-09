#!/bin/sh
set -e

dir=$(mktemp -d)
echo "Building in $dir"
cd "$dir"

git clone https://github.com/ReactiveMongo/ReactiveMongo
cd ReactiveMongo
git checkout 466f214da17ad79f2055f274ef674eeb636ffd64
sbt publish-local
cd ..

git clone https://github.com/ReactiveMongo/Play-ReactiveMongo
cd Play-ReactiveMongo
sbt publish-local
cd ..

git clone https://github.com/ReactiveMongo/Reactivemongo-Play-Json
cd Reactivemongo-Play-Json
sbt publish-local
cd ..

git clone https://github.com/ornicar/scalalib
cd scalalib
sbt publish-local
cd ..

git clone https://github.com/ornicar/scala-kit --branch lichess-fork
cd scala-kit
sbt -Dversion=1.2.11-THIB publish-local
cd ..

git clone https://github.com/ornicar/maxmind-geoip2-scala --branch customBuild
cd maxmind-geoip2-scala
sbt publish-local
cd ..

git clone https://github.com/Nycto/Hasher
cd Hasher
sbt publish-local
cd ..

rm -rf "$dir"
