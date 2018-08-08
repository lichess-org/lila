@echo off

REM Starts a dev console to compile and run lidraughts.
REM Yes it needs tons of memory. Go for 2048M if you have them.

set JAVA_OPTS=-Xms2048M -Xmx3072M -XX:MaxJavaStackTraceDepth=100000 -XX:ReservedCodeCacheSize=64m -XX:+CMSClassUnloadingEnabled -XX:+UseConcMarkSweepGC -XX:+ExitOnOutOfMemoryError -Dkamon.auto-start=true -Dfile.encoding=UTF-8

sbt %*