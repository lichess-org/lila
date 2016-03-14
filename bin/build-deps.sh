#!/bin/sh
set -e

mkdir -p local
cd local
rm -rf maven
git clone https://github.com/ornicar/maven
cd ..

dir=$(mktemp -d)
echo "Building in $dir"
cd "$dir"

git clone https://github.com/msimav/pushy-scala
cd pushy-scala
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
