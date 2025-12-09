#!/usr/bin/env -S bash -e

# Starts a dev console to compile and run lichess.

# Usage:
# ./lila.sh
# Then in the sbt console:
# run

if [[ "${BASH_SOURCE[0]}" != "$0" ]]; then
  >&2 echo "Fatal: lila.sh should be run, not sourced!"
  return 1
fi

WARN() { >&2 printf '%s\n' "[warning] $*"; }
ABORT() { >&2 printf '%s\n' "[FATAL] $*"; exit 1; }

cd "$(dirname -- "$0")"  # set cwd

cat << 'BANNER'
   |\_    _ _      _
   /o \  | (_) ___| |__   ___  ___ ___   ___  _ __ __ _
 (_. ||  | | |/ __| '_ \ / _ \/ __/ __| / _ \| '__/ _` |
   /__\  | | | (__| | | |  __/\__ \__ \| (_) | | | (_| |
  )___(  |_|_|\___|_| |_|\___||___/___(_)___/|_|  \__, |
                                                   |___/
BANNER

if [[ -n $SBT_OPTS ]]; then
  WARN "SBT_OPTS var '$SBT_OPTS' ignored; use .sbtopts file instead."
  unset SBT_OPTS
fi

# use default files if missing
[[ -e .sbtopts ]] || cp .sbtopts{.default,}
[[ -e conf/application.conf ]] || cp conf/application.conf{.default,}

java_path_bin=$(command -v java || true)

if [[ -n "$JAVA_HOME" && -x "$JAVA_HOME/bin/java" ]]; then
  java_home_bin="$JAVA_HOME/bin/java"
else
  WARN "JAVA_HOME invalid or not set."
fi

if [[ -z "$java_path_bin" && -z "$java_home_bin" ]]; then
  ABORT "java not found!"
elif [[ -n "$java_home_bin" && -n "$java_path_bin" ]] &&
     [[ "$java_home_bin" != "$java_path_bin" ]]; then
  # On Darwin, /usr/bin/java is always a shim that ignores JAVA_HOME and
  # will never match. So: warn, unless on Darwin with path java shim.
  if [[ "$OSTYPE" != darwin* || "$java_path_bin" != /usr/bin/java ]]; then
    WARN "JAVA_HOME '$java_home_bin' != PATH '$java_path_bin'"
  fi
fi

java_bin="${java_home_bin:-$java_path_bin}"

if ! "$java_bin" --list-modules 2>/dev/null | grep -Fq jdk.compiler; then
  WARN "$java_bin incomplete. Is it a JRE (and not a JDK)?"
fi

java_version=$("$java_bin" -version 2>&1 | head -n1 | cut -d'"' -f2)

java_env=(
  "-Dreactivemongo.api.bson.document.strict=false"
  # additional java options if needed.
)

# print info
printf '%s / %s\n' "java $java_version" "sbt ${java_env[*]} $*"

exec sbt "${java_env[@]}" "$@"
