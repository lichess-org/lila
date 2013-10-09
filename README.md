[lichess.org](http://lichess.org)
---------------------------------

It's a free online chess game focused on [realtime](http://lichess.org/games) and simplicity.

It haz a [search engine](http://lichess.org/games/search),
[computer analysis](http://lichess.org/analyse/ief49lif), 
[tournaments](http://lichess.org/tournament), 
[forums](http://lichess.org/forum), 
[teams](http://lichess.org/team),  
and a weird [monitoring console](http://lichess.org/monitor).
The UI is available in [72 languages](http://lichess.org/translation/contribute) thanks to the community.

Lichess is written in [Scala 2.10](http://www.scala-lang.org/), 
and relies on [Play 2.2](http://www.playframework.com/) for the routing, templating, and JSON.
Pure chess logic is contained in [scalachess](http://github.com/ornicar/scalachess) submodule.
The codebase is fully asynchronous, making heavy use of Scala Futures and [Akka 2 actors](http://akka.io).
Lichess talks to [Stockfish 4](http://stockfishchess.org/) using a [FSM Actor](https://github.com/ornicar/lila/blob/master/modules/ai/src/main/stockfish/ActorFSM.scala) to handle AI moves and analysis.
It uses [MongoDB 2.4](http://mongodb.org) to store about 15 million games, which are indexed by [elasticsearch 0.90](http://elasticsearch.org).
HTTP requests and websocket connections are proxied by [nginx 1.4](http://nginx.org).

Join us on [#lichess IRC channel](http://lichess.org/irc) for more info.

Installation
------------

> I am **not** happy to see lichess clones spreading on the Internet. This project source code is open for other developers to have an example of non-trivial scala/play2/mongodb application. You're welcome to reuse as much code as you want for your projects, and to get inspired by the solutions I propose to many common web development problems. But please don't just create a public lichess clone. Also, if you are building a website based on lichess, please mention it in the footer with `Based on <a href="http://lichess.org">lichess</a>`. Thank you!

> Also note that if I provide the source code, I do **not** offer free support for your lichess instance. I will probably ignore any question about lichess installation and runtime issues.

This is full-stack application, not a library, and it may not 
be straightforward to get it fully running.
I assume you run a Unix with nginx, mongodb, elasticsearch and stockfish installed.

```sh
git clone git://github.com/ornicar/lila
cd lila
git submodule update --init
bin/play compile
```

### Configuration

```sh
cp conf/application.conf.dist conf/application.conf
```

`application.conf` extends `base.conf` and can override any value.
Note that `application.conf` is excluded from git index.

### Websocket proxying and language subdomains

When accessed from the root domaing (e.g. lichess.org),
the application will redirect to a language specific subdomaing (e.g. en.lichess.org).
Additionally, lichess will open websockets on the `socket.` subdomain (e.g. socket.en.lichess.org).

Here is my local nginx configuration for `l.org`, assuming lila is installed in `/home/thib/lila` and runs on 127.0.0.1:9000
[/etc/nginx/l.org.conf](https://github.com/ornicar/lila/blob/master/doc/nginx/l.org.conf)

And here is my local [/etc/hosts file](https://github.com/ornicar/lila/blob/master/doc/hosts)

### Run it

Launch the play console:

```sh
bin/play
```

From here you can now run the application (`run`). 

Credits
-------

Big thanks go to lichess community for the support, inspiration, bug reports, and [amazing translation efforts](http://lichess.org/translation/contribute).

Special thanks go to:

- [Mephostophilis](http://lichess.org/@/Mephostophilis) for writing [Lichess Wiki](http://lichess.org/wiki), leading the cheater hunt, moderating the site, reporting countless bugs, and contributing to the codebase
- [Smiling Bishop](http://lichess.org/@/smiling_bishop), [legend](http://lichess.org/@/legend), [mb](http://lichess.org/@/mb) and all the moderators who spent time keeping the site enjoyable
- [Evropi](https://github.com/evropi) for contributing to the wiki and animating #lichess IRC channel
- [Steibock](https://github.com/Steibock) for board theming
- [Yusuke Kamiyamane](http://p.yusukekamiyamane.com/) for the fugue icons
- [pgn4web](http://pgn4web.casaschi.net/home.html) for the analysis board
- [chessboardjs](https://github.com/oakmac/chessboardjs/) for the board editor

Thanks to all players for feeding the database.
