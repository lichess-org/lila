lichess.org
===========

Backend of http://lichess.org

- Scala 2.9 with Play2, Akka 2, Scalaz and Salat
- [Scalachess](https://github.com/ornicar/scalachess)
- MongoDB 2.2

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

To run the test, use `test`.

Production
==========

Register as daemon on archlinux
--------------------------------

```sh
cd /etc/rc.d
sudo ln -s /path/to/lila/bin/prod/archlinux/rc.d/lila ./
cd /etc/conf.d
sudo ln -s /path/to/lila/bin/prod/archlinux/conf.d/lila ./
```

- Configure the daemon in /etc/conf.d/lila
- Add lila to DAEMONS in /etc/rc.conf

Optional
ulimit -n 99999

Restart on timeout
------------------

```sh
bin/prod/timeout-restarter
```

Notes to self
=============

Elasticsearch mongodb river
---------------------------

cd $ES_HOME;
bin/plugin -url https://github.com/downloads/richardwilly98/elasticsearch-river-mongodb/elasticsearch-river-mongodb-1.6.1.zip -install river-mongodb
service elasticsearch restart

curl -XPUT 'http://localhost:9200/_river/team/_meta' -d '{ 
    "type": "mongodb", 
    "mongodb": { 
        "db": "lichess", 
        "collection": "team"
    }, 
    "index": {
        "name": "lila", 
        "type": "team" 
    }
}'

/etc/mongodb.conf
replSet=rs0
oplogSize=100
