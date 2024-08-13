#!/bin/bash

mongoimport --db=lichess --collection=local_bots --file=../json/local.bots.json --jsonArray $@