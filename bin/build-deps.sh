#!/bin/sh

dir=`mktemp -d`
echo "Building in $dir"
cd $dir

rm -rf scalalib
git clone https://github.com/ornicar/scalalib
cd scalalib
sbt publish-local
cd ..

rm -rf maxmind-geoip2-scala
git clone https://github.com/ornicar/maxmind-geoip2-scala --branch customBuild
cd maxmind-geoip2-scala
sbt publish-local
cd ..

rm -rf $dir
