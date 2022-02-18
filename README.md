# [newchess.fun](https://newchess.fun)

[![Build server](https://github.com/lichess-org/lila/workflows/Build%20server/badge.svg)](https://github.com/lichess-org/lila/actions?query=workflow%3A%22Build+server%22)
[![Build assets](https://github.com/lichess-org/lila/workflows/Build%20assets/badge.svg)](https://github.com/lichess-org/lila/actions?query=workflow%3A%22Build+assets%22)
[![Crowdin](https://d322cqt584bo4o.cloudfront.net/lichess/localized.svg)](https://crowdin.com/project/lichess)
[![Twitter](https://img.shields.io/badge/Twitter-%40lichess-blue.svg)](https://twitter.com/lichess)

<img src="https://raw.githubusercontent.com/lichess-org/lila/master/public/images/home-bicolor.png" alt="NewChess homepage" title="NewChess comes with light and dark theme, this screenshot shows both." />

Lila (li[chess in sca]la) is a free online chess game server focused on [realtime](https://newchess.fun/games) gameplay and ease of use.

It features a [search engine](https://newchess.fun/games/search),
[computer analysis](https://newchess.fun/ief49lif) distributed with [fishnet](https://github.com/niklasf/fishnet),
[tournaments](https://newchess.fun/tournament),
[simuls](https://newchess.fun/simul),
[forums](https://newchess.fun/forum),
[teams](https://newchess.fun/team),
[tactic trainer](https://newchess.fun/training),
a [mobile app](https://newchess.fun/mobile),
and a [shared analysis board](https://newchess.fun/study).
The UI is available in more than [130 languages](https://crowdin.com/project/lichess) thanks to the community.

NewChess is written in [Scala 2.13](https://www.scala-lang.org/),
and relies on the [Play 2.8](https://www.playframework.com/) framework.
[scalatags](https://com-lihaoyi.github.io/scalatags/) is used for templating.
Pure chess logic is contained in the [scalachess](https://github.com/ornicar/scalachess) submodule.
The server is fully asynchronous, making heavy use of Scala Futures and [Akka streams](https://akka.io).
WebSocket connections are handled by a [separate server](https://github.com/lichess-org/lila-ws) that communicates using [redis](https://redis.io/).
NewChess talks to [Stockfish](https://stockfishchess.org/) deployed in an [AI cluster](https://github.com/niklasf/fishnet) of donated servers.
It uses [MongoDB](https://mongodb.org) to store more than 1.7 billion games, which are indexed by [elasticsearch](https://github.com/elastic/elasticsearch).
HTTP requests and WebSocket connections can be proxied by [nginx](https://nginx.org).
The web client is written in [TypeScript](https://www.typescriptlang.org/) and [snabbdom](https://github.com/snabbdom/snabbdom), using [Sass](https://sass-lang.com/) to generate CSS.
The [blog](https://newchess.fun/blog) uses a free open content plan from [prismic.io](https://prismic.io).
All rated games are published in a [free PGN database](https://database.newchess.fun).
Browser testing done with [Browserstack](https://www.browserstack.com).
Proxy detection done with [IP2Proxy database](https://www.ip2location.com/database/ip2proxy).
Please help us [translate NewChess with Crowdin](https://crowdin.com/project/lichess).

See [newchess.fun/source](https://newchess.fun/source) for a list of repositories.

[Join us on Discord](https://discord.gg/lichess) for more info.
Use [GitHub issues](https://github.com/lichess-org/lila/issues) for bug reports and feature requests.

## Installation

```
./lila # thin wrapper around sbt
run
```

The Wiki describes [how to setup a development environment](https://github.com/lichess-org/lila/wiki/NewChess-Development-Onboarding).

## debug

add `-J-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5006` to `.sbtopts` and call Installation commands

## prod

```
sbt universal:packageBin
cd target/universal/ && unzip -o lila-3.2.zip && cd ../..
./ui/build prod
cp -R public/ target/universal/lila-3.2/
./target/universal/lila-ws-2.1/bin/lila-ws
```

## newchess config

You can use `conf/application.ini`

```
net {
  domain = "newchess.fun"
  socket.domains = [ "ws.newchess.fun" ]
  asset.base_url = "https://"${net.asset.domain}
  base_url = "https://"${net.domain}
}
prismic.api_url = "https://newchess1.cdn.prismic.io/api"
```

and `target/universal/lila-3.2/conf/application.ini` for secrets and etc (not included in repo):
```
-Dstorm.secret="somethingElseInProd"
-Dsecurity.password_reset.secret="somethingElseInProd"
-Dsecurity.email_confirm.secret="somethingElseInProd"
-Dsecurity.email_change.secret="somethingElseInProd"
-Dsecurity.login_token.secret="somethingElseInProd"
-J-Xms256M
-J-Xmx2048M
```

The Wiki describes [how to setup a development environment](https://github.com/lichess-org/lila/wiki/Lichess-Development-Onboarding).

## HTTP API

Feel free to use the [NewChess API](https://newchess.fun/api) in your applications and websites.

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
version at your choice with an exception for Highcharts. See [copying](https://github.com/lichess-org/lila/blob/master/COPYING.md) for
details.

## Credits

See [newchess.fun/thanks](https://newchess.fun/thanks) and the contributors here:

[![GitHub contributors](https://contrib.rocks/image?repo=lichess-org/lila)](https://github.com/lichess-org/lila/graphs/contributors)

## Competence development program

NewChess would like to support its contributors in their competence development by covering costs of relevant training materials and activities. This is a small way to further empower contributors who have given their time to NewChess and to enable or improve additional contributions to NewChess in the future. For more information, including how to apply, check [Competence Development for NewChess contributors](https://newchess.fun/page/competence-development).
