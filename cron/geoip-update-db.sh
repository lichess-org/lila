#!/bin/sh -e

LILA_DIR="$(dirname -- $0)/.."
DATA_DIR="$LILA_DIR/data"
TGZ_FILE="GeoLite2-City.mmdb.tar.gz"

if [ -z "$MAXMIND_KEY" ]; then
  echo "MAXMIND_KEY is not set, exiting"
  exit 1
fi

echo "Downloading GeoLite2-City database to $DATA_DIR/$TGZ_FILE"

mkdir -p $DATA_DIR
cd $DATA_DIR
curl -L https://download.maxmind.com/app/geoip_download\?edition_id\=GeoLite2-City\&license_key\=$MAXMIND_KEY\&suffix\=tar.gz -o $TGZ_FILE
tar xvzf $TGZ_FILE
mv GeoLite2-City_*/GeoLite2-City.mmdb ./
rm -rf GeoLite2-City_*
cd -
