#!/bin/sh -e

# Use .env file, if it exists.
if test -f .env; then
    export $(egrep -v '^#' .env | xargs)
fi

mkdir -p data
cd data
curl https://download.maxmind.com/app/geoip_download\?edition_id\=GeoLite2-City\&license_key\=$MAXMIND_KEY\&suffix\=tar.gz -o GeoLite2-City.mmdb.tar.gz
tar xvzf GeoLite2-City.mmdb.tar.gz
mv GeoLite2-City_*/GeoLite2-City.mmdb ./
rm -rf GeoLite2-City_*
cd -
