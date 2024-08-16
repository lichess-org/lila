#!/bin/bash

mongoexport --db=lichess --collection=local_bots --out=../json/local.bots.json --jsonArray $@
mongoexport --db=lichess --collection=local_assets --out=../json/local.assets.json --jsonArray $@
