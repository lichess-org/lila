Scalashogi
==========

Shogi API written in scala for [lishogi.org](https://lishogi.org)

It is entirely functional, immutable, and free of side effects.

INSTALL
-------

Clone lishogi and then set the directory to `/modules/shogi`.

    git clone git://github.com/WandererXII/lishogi
    cd lishogi/modules/shogi

Get latest sbt on http://www.scala-sbt.org/download.html

Start sbt in `/modules/shogi` directory

    sbt

In the sbt shell, to compile scalashogi, run

    compile

To run the tests (with coverage):

    clean coverage test
    coverageReport

Code formatting
---------------

This repository uses [scalafmt](https://scalameta.org/scalafmt/).

Please [install it for your code editor](https://scalameta.org/scalafmt/docs/installation.html)
if you're going to contribute to this project.

If you don't install it, please run `scalafmtAll` in the sbt console before committing.