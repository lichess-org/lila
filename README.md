# [lichess.org](https://lichess.org)

[![Build server](https://github.com/ornicar/lila/workflows/Build%20server/badge.svg)](https://github.com/ornicar/lila/actions?query=workflow%3A%22Build+server%22)
[![Build assets](https://github.com/ornicar/lila/workflows/Build%20assets/badge.svg)](https://github.com/ornicar/lila/actions?query=workflow%3A%22Build+assets%22)
[![Crowdin](https://d322cqt584bo4o.cloudfront.net/lichess/localized.svg)](https://crowdin.com/project/lichess)
[![Twitter](https://img.shields.io/badge/Twitter-%40lichess-blue.svg)](https://twitter.com/lichess)

<img src="https://raw.githubusercontent.com/ornicar/lila/master/public/images/home-bicolor.png" alt="Lichess homepage" title="Lichess comes with light and dark theme, this screenshot shows both." />

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
The UI is available in more than [130 languages](https://crowdin.com/project/lichess) thanks to the community.

Lichess is written in [Scala 2.13](https://www.scala-lang.org/),
and relies on the [Play 2.8](https://www.playframework.com/) framework.
[scalatags](https://www.lihaoyi.com/scalatags/) is used for templating.
Pure chess logic is contained in the [scalachess](https://github.com/ornicar/scalachess) submodule.
The server is fully asynchronous, making heavy use of Scala Futures and [Akka streams](https://akka.io).
WebSocket connections are handled by a [separate server](https://github.com/ornicar/lila-ws) that communicates using [redis](https://redis.io/).
Lichess talks to [Stockfish](https://stockfishchess.org/) deployed in an [AI cluster](https://github.com/niklasf/fishnet) of donated servers.
It uses [MongoDB](https://mongodb.org) to store more than 1.7 billion games, which are indexed by [elasticsearch](https://github.com/elastic/elasticsearch).
HTTP requests and WebSocket connections can be proxied by [nginx](https://nginx.org).
The web client is written in [TypeScript](https://www.typescriptlang.org/) and [snabbdom](https://github.com/snabbdom/snabbdom), using [Sass](https://sass-lang.com/) to generate CSS.
The [blog](https://lichess.org/blog) uses a free open content plan from [prismic.io](https://prismic.io).
All rated games are published in a [free PGN database](https://database.lichess.org).
Browser testing done with [Browserstack](https://www.browserstack.com).
Proxy detection done with [IP2Proxy database](https://www.ip2location.com/database/ip2proxy).
Please help us [translate Lichess with Crowdin](https://crowdin.com/project/lichess).

See [lichess.org/source](https://lichess.org/source) for a list of repositories.

[Join us on discord](https://discord.gg/hy5jqSs) or in the `#lichess` freenode IRC channel for more info.
Use [GitHub issues](https://github.com/ornicar/lila/issues) for bug reports and feature requests.

## Installation

```
./lila # thin wrapper around sbt
run
```

The Wiki describes [how to setup a development environment](https://github.com/ornicar/lila/wiki/Lichess-Development-Onboarding).

## HTTP API

Feel free to use the [Lichess API](https://lichess.org/api) in your applications and websites.

## Supported browsers

| Name              | Version | Notes                                             |
| ----------------- | ------- | ------------------------------------------------- |
| Chromium / Chrome | last 10 | Full support                                      |
| Firefox           | 61+     | Full support (fastest local analysis since FF 79) |
| Opera             | 55+     | Reasonable support                                |
| Safari            | 11.1+   | Reasonable support                                |
| Edge              | 17+     | Reasonable support                                |

Older browsers (including any version of Internet Explorer) will not work.
For your own sake, please upgrade. Security and performance, think about it!

## License

Lila is licensed under the GNU Affero General Public License 3 or any later
version at your choice with an exception for Highcharts. See COPYING for
details.

## Credits

See the [contributors](https://github.com/ornicar/lila/graphs/contributors) here and [lichess.org/thanks](https://lichess.org/thanks).
<a href="https://github.com/ornicar/lila/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=ornicar/lila" />
</a>
