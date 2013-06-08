[lichess.org](http://lichess.org)
=================================

It's a free online chess game focused on [realtime](http://lichess.org/games) and simplicity.

It haz a [search engine](http://lichess.org/games/search),
[computer analysis](http://lichess.org/analyse/ief49lif), 
[tournaments](http://lichess.org/tournament), 
[forums](http://lichess.org/forum), 
[teams](http://lichess.org/team),  
and a weird [monitoring console](http://lichess.org/monitor).
The UI is available in [72 languages](http://lichess.org/translation/contribute) thanks to the community.

Lichess is written in [Scala 2.10](http://www.scala-lang.org/), 
and relies on [Play 2.1](http://www.playframework.com/) for the routing, templating, and JSON.
The codebase is fully asynchronous, making heavy use of Scala Futures and [Akka 2 actors](http://akka.io).
It uses [MongoDB 2.4](http://mongodb.org) to store about 10 million games, which are indexed by [elasticsearch 0.90](http://elasticsearch.org).
HTTP requests and websocket connections are proxied by [nginx 1.4](http://nginx.org).

Installation
============

This is full-stack application, not a library, and it may not 
be straightforward to get it fully running.
I assume you run a Unix with mongodb.

> Some steps of the installation will trigger a download of the galaxy. It will take ages.

```sh
git clone git://github.com/ornicar/lila
cd lila
git submodule update --init
bin/play compile
```

Configuration
-------------

```sh
cp conf/application.conf.dist conf/application.conf
```

`application.conf` extends `base.conf` and can override any value.
Note that `application.conf` is excluded from git index.

Language subdomains
-------------------

When accessed from the root domaing (e.g. lichess.org),
the application will redirect to a language specific subdomaing (e.g. en.lichess.org).
Additionally, lichess will open websockets on the `socket.` subdomain (e.g. socket.en.lichess.org).
Here is my local nginx configuration for `l.org`, assuming lila is installed in `/home/thib/lila` and runs on port 9000.

```conf
# Websockets
server {
  listen 80;
  server_name ~^socket\.\w\w\.l\.org$;

  location / {
    include lila-proxy.conf;
    proxy_read_timeout 5s;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_pass http://127.0.0.1:9000/;
  }
}

# Application
server {
  listen 80;
  server_name l.org ~^\w\w\.l\.org$;

  location /assets {
    alias   /home/thib/lila/public;
  }

  location /serve {
    alias   /home/thib/lila/serve;
  }

  location /import {
    proxy_set_header Host $http_host;
    proxy_read_timeout 60s;
    proxy_pass http://127.0.0.1:9000/import;
  }

  location / {
    add_header "X-UA-Compatible" "IE=Edge,chrome=1"; 
    proxy_set_header Host $http_host;
    proxy_read_timeout 90s;
    proxy_pass http://127.0.0.1:9000/;
  }
}
```

And the `/etc/hosts` file looks like:

```
127.0.0.1	l.org
127.0.0.1	en.l.org
127.0.0.1	fr.l.org
127.0.0.1	socket.en.l.org
127.0.0.1	socket.fr.l.org
```

Run it
------

Launch the play console:

```sh
bin/play
```

From here you can now run the application (`run`). 
