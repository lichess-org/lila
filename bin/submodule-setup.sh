#!/bin/bash -ex

git submodule init
git submodule foreach --quiet --recursive \
  "git config --local remote.origin.url | grep -e ornicar -e lichess \
   && git fetch origin +refs/pull/*/head:refs/remotes/origin/pr/* || :"
git submodule update --recursive --init
