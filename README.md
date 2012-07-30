lichess.org
===========

Backend of http://lichess.org

- Scala 2.9 with Play2, Akka 2, Scalaz and Salat
- [Scalachess](https://github.com/ornicar/scalachess)
- MongoDB 2
- ArchLinux

Installation
============

This is full-stack application, not a library, and it may not 
be straightforward to get it fully running.
I assume you run a Unix with mongodb.

> Some steps of the installation will trigger a download of the galaxy.
> This will take, naturally, ages.

Play 2.1
--------

Interestingly enough, Play 2.1 lives in a repository named Play20.

```sh
git clone git://github.com/playframework/Play20.git Play21
cd Play21/framework
./build build-repository
./build publish-local
```

Lila
----

The database layer, website, cron tasks, websockets and whatnot.

```sh
git clone git://github.com/ornicar/lila
cd lila
git submodule update --init
play compile
```

Config is in `conf/application.conf`.

If all the above worked, which would be quite surprising, you can now setup a webserver and run the application.

Nginx
-----

This is just my local nginx configuration, as an example.

```
server {
  listen 80;
  server_name l.org ~^\w\w\.l\.org$; 

  location /assets {
    alias   /home/thib/lila/public;
  }

  location /serve {
    alias   /home/thib/lila/serve;
  }

  location / {
    proxy_set_header Host $http_host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_pass http://127.0.0.1:9000/;
  }

  error_page 500 501 502 503 504  /error.html;
  location = /error.html {
    root  /home/thib/lila/public/;
  }
}
```

Run it
------

Open lila play console and give it a ride.

>To open a play console using development configuration, use:
>
>    bin/dev
>
>This will use the `conf/application_dev.conf` configuration file.

```
bin/dev
[lila] $ run
```
