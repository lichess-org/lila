#!/bin/bash

mongoimport --db=lichess --collection=local_bots --file=../json/local.bots.json --jsonArray $@
mongoimport --db=lichess --collection=local_assets --file=../json/local.assets.json --jsonArray $@
