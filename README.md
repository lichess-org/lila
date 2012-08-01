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

> Some steps of the installation will trigger a download of the galaxy. It will take ages.

```sh
git clone git://github.com/ornicar/lila
cd lila
git submodule update --init
bin/build-play2
bin/play compile
```

Configuration
-------------

```sh
cp conf/local.conf.dist conf/local.conf
```

`conf/local.conf` extends `conf/base.conf` and can override any value.
Note that `conf/local.conf` is excluded from git index.

Run it
------

Launch the play console using your local configuration:

```sh
bin/play
```

From here you can now run the application (`run`). 
Open your web browser at `localhost:9000`.
