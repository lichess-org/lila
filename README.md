lichess.org in scala
====================

Complete rewrite of [lichess.org](lichess.org) using scala and play2-mini.

Prerequisites
-------------

- Any Java runtime >= 1.6
- sbt (get it with your package manager, or see [the manual installation guide](https://github.com/harrah/xsbt/wiki/Getting-Started-Setup))

Installation
------------

    git clone git://github.com/ornicar/lila && cd lila

Run tests
---------

    sbt test

Start http server on port 9000
------------------------------

    sbt run

Credits 
-------

Chess logic based on [Synesso scala chess](https://github.com/Synesso/scala-chess)
