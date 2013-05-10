lichess.org
===========

Complete source code of http://lichess.org,
using Scala 2.10.1, Play 2.1, Akka 2, ReactiveMongo and Scalaz 

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

Run it
------

Launch the play console:

```sh
bin/play
```

From here you can now run the application (`run`). 
Open your web browser at `localhost:9000`.
