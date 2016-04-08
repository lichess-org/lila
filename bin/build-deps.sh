#!/bin/sh
set -e

dir=$(mktemp -d)
echo "Building in $dir"
cd "$dir"

git clone https://github.com/gilt/gfc-semver
cd gfc-semver
sbt publish-local
cd ..

git clone https://github.com/ornicar/ReactiveMongo --branch lichess-shaded
cd ReactiveMongo
sbt compile
sbt publish-local
cd ..

git clone https://github.com/ornicar/scalalib
cd scalalib
sbt publish-local
cd ..

git clone https://github.com/ornicar/scala-kit --branch lichess-fork
cd scala-kit
git checkout 75fb61c8306a7a3db4c5dae6f110972130e7c6bc
sbt -Dversion=1.2.11-THIB2 publish-local
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
