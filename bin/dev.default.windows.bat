# Starts a dev console to compile and run lichess.
# To edit this file, copy it to bin\dev.bat: it's not indexed by Git.

@echo off

# Yes it needs tons of memory. Go for 2048M if you have them.
set JAVA_OPTS=-Xms1536M -Xmx1536M -XX:ReservedCodeCacheSize=64m -XX:+CMSClassUnloadingEnabled -XX:+UseConcMarkSweepGC -XX:+ExitOnOutOfMemoryError -Dkamon.auto-start=true

sbt %*
