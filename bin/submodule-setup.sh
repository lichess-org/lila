#!/bin/bash -ex

if ! git submodule update --init --recursive; then
  git submodule foreach --quiet --recursive \
     "git config --local remote.origin.url | grep -e ornicar -e lichess \
      && git fetch origin +refs/pull/*/head:refs/remotes/origin/pr/* || :"
  git submodule update --recursive --init
fi
