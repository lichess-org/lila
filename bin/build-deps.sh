#!/bin/sh
set -e

dir=$(mktemp -d)
echo "Building in $dir"
cd "$dir"

git clone --depth 1 https://github.com/ReactiveMongo/ReactiveMongo --branch 0.12.1
cd ReactiveMongo
export ITERATEES_VERSION=2.4.6
sbt publish-local
cd ..

git clone --depth 1 https://github.com/ornicar/Kamon --branch lila
cd Kamon
sbt publish-local
cd ..

git clone --depth 1 https://github.com/gilt/gfc-semver
cd gfc-semver
sbt publish-local
cd ..

git clone --depth 1 https://github.com/ornicar/scalalib
cd scalalib
sbt publish-local
cd ..

git clone https://github.com/ornicar/scala-kit --branch lichess-fork
cd scala-kit
git checkout b019b3a2522d3f1697c39ec0c79e88c18ea49a91
sbt -Dversion=1.2.11-THIB publish-local
cd ..

git clone --depth 1 https://github.com/ornicar/maxmind-geoip2-scala --branch customBuild
cd maxmind-geoip2-scala
sbt publish-local
cd ..

git clone --depth 1 https://github.com/Nycto/Hasher
cd Hasher
sbt '+ publish-local'
cd ..

rm -rf "$dir"
