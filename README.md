# [lichess.org](https://lichess.org)

[![Build server](https://github.com/lichess-org/lila/actions/workflows/server.yml/badge.svg)](https://github.com/lichess-org/lila/actions/workflows/server.yml)
[![Build assets](https://github.com/lichess-org/lila/actions/workflows/assets.yml/badge.svg)](https://github.com/lichess-org/lila/actions/workflows/assets.yml)
[![Crowdin](https://d322cqt584bo4o.cloudfront.net/lichess/localized.svg)](https://crowdin.com/project/lichess)
[![Mastodon](https://img.shields.io/mastodon/follow/109298525492334687?domain=mastodon.online)](https://mastodon.online/@lichess)
[![Bluesky](https://img.shields.io/badge/Bluesky-0285FF?logo=bluesky&logoColor=fff)](https://bsky.app/profile/lichess.org)
[![Discord](https://img.shields.io/discord/280713822073913354?label=Discord&logo=discord&style=flat)](https://discord.gg/lichess)

<img src="https://raw.githubusercontent.com/lichess-org/lila/master/public/images/home-bicolor.png" alt="Lichess homepage" title="Lichess comes with light and dark theme, this screenshot shows both." />

Lila (li[chess in sca]la) is a free online chess game server focused on [realtime](https://lichess.org/games) gameplay and ease of use.

It features a [search engine](https://lichess.org/games/search),
[computer analysis](https://lichess.org/ief49lif) distributed with [fishnet](https://github.com/lichess-org/fishnet),
[tournaments](https://lichess.org/tournament),
[simuls](https://lichess.org/simul),
[forums](https://lichess.org/forum),
[teams](https://lichess.org/team),
[tactic trainer](https://lichess.org/training),
a [mobile app](https://lichess.org/mobile),
and a [shared analysis board](https://lichess.org/study).
The UI is available in more than [140 languages](https://crowdin.com/project/lichess) thanks to the community.

Lichess is written in [Scala 3](https://www.scala-lang.org/),
and relies on the [Play 2.8](https://www.playframework.com/) framework.
[scalatags](https://com-lihaoyi.github.io/scalatags/) is used for templating.
Pure chess logic is contained in the [scalachess](https://github.com/lichess-org/scalachess) submodule.
The server is fully asynchronous, making heavy use of Scala Futures and [Akka streams](https://akka.io).
WebSocket connections are handled by a [separate server](https://github.com/lichess-org/lila-ws) that communicates using [redis](https://redis.io/).
Lichess talks to [Stockfish](https://stockfishchess.org/) deployed in an [AI cluster](https://github.com/lichess-org/fishnet) of donated servers.
It uses [MongoDB](https://www.mongodb.com) to store more than 4.7 billion games, which are indexed by [elasticsearch](https://github.com/elastic/elasticsearch).
HTTP requests and WebSocket connections can be proxied by [nginx](https://nginx.org).
The web client is written in [TypeScript](https://www.typescriptlang.org/) and [snabbdom](https://github.com/snabbdom/snabbdom), using [Sass](https://sass-lang.com/) to generate CSS.
All rated games are published in a [free PGN database](https://database.lichess.org).
Browser testing done with [Browserstack](https://www.browserstack.com).
Proxy detection done with [IP2Proxy database](https://www.ip2location.com/database/ip2proxy).
Please help us [translate Lichess with Crowdin](https://crowdin.com/project/lichess).

See [lichess.org/source](https://lichess.org/source) for a list of repositories.

[Join us on Discord](https://discord.gg/lichess) for more info.
Use [GitHub issues](https://github.com/lichess-org/lila/issues) for bug reports and feature requests.

## Installation

```
./lila.sh # thin wrapper around sbt
run
```

The Wiki describes [how to setup a development environment](https://github.com/lichess-org/lila/wiki/Lichess-Development-Onboarding).

## HTTP API

Feel free to use the [Lichess API](https://lichess.org/api) in your applications and websites.

## Supported browsers

| Name              | Version | Notes                                             |
| ----------------- | ------- | ------------------------------------------------- |
| Chromium / Chrome | last 10 | Full support                                      |
| Firefox           | 75+     | Full support (fastest local analysis since FF 79) |
| Edge              | 91+     | Full support (reasonable support for 79+)         |
| Opera             | 66+     | Reasonable support                                |
| Safari            | 11.1+   | Reasonable support                                |

Older browsers (including any version of Internet Explorer) will not work.
For your own sake, please upgrade. Security and performance, think about it!

## License

Lila is licensed under the GNU Affero General Public License 3 or any later
version at your choice. See [copying](https://github.com/lichess-org/lila/blob/master/COPYING.md) for
details.

## Production architecture (as of July 2022)

![Lichess production server architecture diagram](https://raw.githubusercontent.com/lichess-org/lila/master/public/images/architecture.png)

## Credits

See [lichess.org/thanks](https://lichess.org/thanks) and the contributors here:

[![GitHub contributors](https://contrib.rocks/image?repo=lichess-org/lila)](https://github.com/lichess-org/lila/graphs/contributors)

## Competence development program

Lichess would like to support its contributors in their competence development by covering costs of relevant training materials and activities. This is a small way to further empower contributors who have given their time to Lichess and to enable or improve additional contributions to Lichess in the future. For more information, including how to apply, check [Competence Development for Lichess contributors](https://lichess.org/page/competence-development).
