[lichess.org](https://lichess.org)
==================================

[![Build Status](https://travis-ci.org/ornicar/lila.svg?branch=master)](https://travis-ci.org/ornicar/lila)
[![Crowdin](https://d322cqt584bo4o.cloudfront.net/lichess/localized.svg)](https://crowdin.com/project/lichess)
[![Twitter](https://img.shields.io/badge/Twitter-%40lichess-blue.svg)](https://twitter.com/lichess)

<img src="https://raw.githubusercontent.com/ornicar/lila/master/public/images/home-bicolor.png" alt="lichess.org" />

Lila (li[chess in sca]la) is a free online chess game server focused on [realtime](https://lichess.org/games) gameplay and ease of use.

It features a [search engine](https://lichess.org/games/search),
[computer analysis](https://lichess.org/ief49lif) distributed with [fishnet](https://github.com/niklasf/fishnet),
[tournaments](https://lichess.org/tournament),
[simuls](https://lichess.org/simul),
[forums](https://lichess.org/forum),
[teams](https://lichess.org/team),
[tactic trainer](https://lichess.org/training),
a [mobile app](https://lichess.org/mobile),
and a [shared analysis board](https://lichess.org/study).
The UI is available in more than [80 languages](https://crowdin.com/project/lichess) thanks to the community.

Lichess is written in [Scala 2.11](https://www.scala-lang.org/),
and relies on [Play 2.4](https://www.playframework.com/) for the routing, templating, and JSON.
Pure chess logic is contained in [scalachess](https://github.com/ornicar/scalachess) submodule.
The server is fully asynchronous, making heavy use of Scala Futures and [Akka 2 actors](http://akka.io).
Lichess talks to [Stockfish](http://stockfishchess.org/) deployed in an [AI cluster](https://github.com/niklasf/fishnet) of donated servers.
It uses [MongoDB](https://mongodb.org) to store more than 800 million games, which are indexed by [elasticsearch](http://elasticsearch.org).
HTTP requests and websocket connections are proxied by [nginx](http://nginx.org).
The web client is written in [TypeScript](https://typescriptlang.org) and [snabbdom](https://github.com/snabbdom/snabbdom).
The [blog](https://lichess.org/blog) uses a free open content plan from [prismic.io](https://prismic.io).
All rated games are published in a [free PGN database](https://database.lichess.org).
Browser testing done with [![Browserstack](https://raw.githubusercontent.com/ornicar/lila/master/public/images/browserstack.png)](https://www.browserstack.com).
Please help us [translate lichess with Crowdin](https://crowdin.com/project/lichess).

[Join us on discord](https://discord.gg/hy5jqSs) or in the #lichess freenode IRC channel for more info.
Use [GitHub issues](https://github.com/ornicar/lila/issues) for bug reports and feature requests.

Installation
------------

The Wiki describes [how to setup a development environment](https://github.com/ornicar/lila/wiki/Lichess-Development-Onboarding).

The source code is available for learning and contribution, but please don't just setup a public Lichess clone. Don't expect developers to help you run your own instance. Questions about the installation and runtime issues will probably be ignored.

HTTP API
--------

Feel free to use [lichess API](https://lichess.org/api) in your applications and websites.

Credits
-------

See the [contributors](https://github.com/ornicar/lila/graphs/contributors) on this repository and [lichess.org/thanks](https://lichess.org/thanks).

Supported browsers
------------------

- [Chrome](https://www.google.com/chrome) or [Chromium](https://www.chromium.org/getting-involved/download-chromium), 1 year old or newer (fastest local analysis!)
- [Firefox](https://www.mozilla.org/firefox), 1 year old or newer (second fastest local analysis!)
- Opera 55 and newer (meh)
- Safari 10.1 and newer (boo)
- Microsoft Edge 17 and newer (eww)

Older browsers (including any version of Internet Explorer) will not work.
For your own sake, please upgrade. Security and performance, think about
it!

License
-------

Lila is licensed under the GNU Affero General Public License 3 or any later
version at your choice with an exception for Highcharts. See COPYING for
details.
