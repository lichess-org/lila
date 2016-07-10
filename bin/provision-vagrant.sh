#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

# We add environment variables to these files.
readonly PROFILES=("$HOME/.bash_profile" "$HOME/.bashrc")

# Directory containing the lila source code.
readonly LILA_DIR='/vagrant'

# The HTTP port to listen on.
readonly PORT=9663

# List of packages to install with apt.
readonly DEPENDENCIES=(
    git
    sbt
    npm
    zsh

    mongodb
    nginx

    gcc
    make
    closure-compiler
    openjdk-8-jdk
)

error() {
    echo >>/dev/stderr "ERROR: $*"
}

info() {
    echo "INFO: $*"
}

setup_application_config() {
    cat >"$LILA_DIR/conf/application.conf" <<'CONF'
include "base"

net {
  domain = "l.org"
  asset.domain = "en.l.org"
  extra_ports = []
}

ai {
  hash_size = 128
  threads = 1
  instances = 4
  debug = false
  play {
    movetime = 500 ms
  }
  analyse {
    movetime = 2000 ms
  }
}

geoip {
    file = "data/GeoLite2-City.mmdb"
}
CONF
}

# Set compilation options so that we don't run out of memory.
setup_environment_variables() {
    for i in "${PROFILES[@]}"; do
        echo >>"$i" "export JAVA_OPTS='-Xms64M -Xmx3072M -Xss4M -XX:ReservedCodeCacheSize=64m -XX:+CMSClassUnloadingEnabled -XX:+UseConcMarkSweepGC'"
    done

    . -- "${PROFILES[0]}"
}

# For sbt: http://www.scala-sbt.org/release/tutorial/Installing-sbt-on-Linux.html
update_apt() {
    echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
    sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 642AC823

    sudo apt-get update
}

install_dependencies() {
    sudo apt-get install -y -- "${DEPENDENCIES[@]}"

    # `npm` relies on a `node` executable, but we have `nodejs`.
    # https://github.com/joyent/node/issues/3911
    sudo ln -sf '/usr/bin/nodejs' '/usr/bin/node'

    # Installed `npm` is too old, upgrade it.
    sudo npm update -g
    sudo npm install -g npm

    sudo npm install -g gulp
}

setup_nginx() {
    local nginx_conf
    nginx_conf=$(mktemp)

    sed "s#LILA_DIR#$LILA_DIR#g" >"$nginx_conf" <<'CONF'
server {
  server_name l.org ~^\w\w\.l\.org$;
  listen 80;

  error_log /var/log/nginx/lila.error.log;
  access_log /var/log/nginx/lila.access.log;

  charset utf-8;

  location /assets {
    add_header "Access-Control-Allow-Origin" "*";
    alias   LILA_DIR/public;
  }

  location / {
    proxy_set_header Host $http_host;
    proxy_set_header X-Forwarded-For $remote_addr;
    proxy_read_timeout 90s;
    proxy_pass http://127.0.0.1:9663/;
  }

  error_page 500 501 502 503 /oops/servererror.html;
  error_page 504  /oops/timeout.html;
  error_page 429  /oops/toomanyrequests.html;
  location /oops/ {
    root  LILA_DIR/public/;
  }
  location = /robots.txt {
    root  LILA_DIR/public/;
  }

}

server {
  server_name socket.l.org;
  listen 80;
  charset utf-8;
  location / {
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_pass http://127.0.0.1:9663/;
  }
}
CONF

    sudo mv -- "$nginx_conf" '/etc/nginx/sites-available/default'
    sudo nginx -s reload
}

setup_mongodb() {
    # Default MongoDB database path. `mongod` will fail to launch if this
    # doesn't exist.
    sudo mkdir -p /data/db

    sudo service mongodb start
}

build_lila() {
    cd -- "$LILA_DIR"
    git submodule update --init --recursive

    ./ui/build
    ./bin/gen/geoip
    ./bin/build-deps.sh

    sbt compile
}

main() {
    setup_application_config
    setup_environment_variables

    update_apt
    install_dependencies
    setup_nginx
    setup_mongodb
    build_lila

    info 'Lila is all set up! Add this entry to your hosts file on your'
    info 'host machine (not the virtual machine, or else I would have done it'
    info 'for you):'
    info
    info "    192.168.34.34 l.org socket.l.org en.l.org de.l.org le.l.org fr.l.org es.l.org l1.org ru.l.org el.l.org hu.l.org"
    info
    info 'Then run "vagrant ssh" and carry out these steps inside your SSH'
    info 'connection:'
    info
    info '   1. cd /vagrant'
    info "   2. sbt run -Dhttp.port=$PORT"
    info "   3. Leave 'sbt run' running inside your virtual machine and visit"
    info "      http://en.l.org on your host machine. It won't load. This is"
    info '      expected.'
    info '   4. Wait until sbt run has finished launching. (After it has'
    info '      finished, relevant output will be printed. If the most recent'
    info '      message is'
    info
    info '          (Server started, use Ctrl+D to stop and go back to the console...)'
    info
    info '      then you have not waited long enough.)'
    info '   5. Visit http://en.l.org again. It should load now.'
}

main
