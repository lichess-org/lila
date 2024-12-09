#!/bin/sh -e

# Starts a dev console to compile and run lichess.

# Usage:
# ./lila
# Then in the sbt console:
# run

# We use .sbtopts instead
export SBT_OPTS=""

if [ ! -f ".sbtopts" ]; then
  cp .sbtopts.default .sbtopts
fi

if [ ! -f "conf/application.conf" ]; then
  cp conf/application.conf.default conf/application.conf
fi

java_env="-Dreactivemongo.api.bson.document.strict=false"

cat << "BANNER"
   |\_    _ _      _
   /o \  | (_) ___| |__   ___  ___ ___   ___  _ __ __ _
 (_. ||  | | |/ __| '_ \ / _ \/ __/ __| / _ \| '__/ _` |
   /__\  | | | (__| | | |  __/\__ \__ \| (_) | | | (_| |
  )___(  |_|_|\___|_| |_|\___||___/___(_)___/|_|  \__, |
                                                   |___/
BANNER

if ! command -v java; then
    echo "Cannot find Java, is it installed?"
    exit 1
fi
version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo Java "$version"

javabin=$(which java)

if [ -z "$JAVA_HOME" ]; then
    echo "Warning, JAVA_HOME is unset"
elif [ "$JAVA_HOME/bin/java" != "$javabin" ]; then
    echo "Warning, JAVA_HOME [$JAVA_HOME] is not the same as java on PATH [$javabin]"
fi

if ! $(java --list-modules | grep -q jdk.compiler -); then
    echo Warning, $javabin is missing module jdk.compiler - this can happen if you mistakenly use a JRE instead of a JDK.
fi


command="sbt $java_env $@"
echo $command
$command
