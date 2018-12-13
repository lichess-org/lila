@echo off

REM Starts a dev console to compile and run lichess.
REM
REM To edit this file, copy it to bin\dev.bat. It will not be tracked by Git.
REM
REM Usage:
REM bin\dev.bat [compile] [run]

REM Yes it needs tons of memory. Go for 4G if you have them.
set JAVA_OPTS=-Xms2048M -Xmx2560M -XX:ReservedCodeCacheSize=128m -XX:+CMSClassUnloadingEnabled -XX:+UseConcMarkSweepGC -XX:+ExitOnOutOfMemoryError -Dkamon.auto-start=true

REM For development without nginx.
set SERVE_ASSETS=1

sbt %*
