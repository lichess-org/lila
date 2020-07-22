package views.html.lobby

import play.api.libs.json.Json

import lila.api.Context
import lila.app.mashup.Preload.Homepage
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.game.Pov

import controllers.routes

object home {

  def apply(homepage: Homepage)(implicit ctx: Context) = {
    import homepage._
    views.html.base.layout(
      title = "",
      fullTitle = Some {
        s"lichess.${if (isProd && !isStage) "org" else "dev"} • ${trans.freeOnlineChess.txt()}"
      },
      moreJs = frag(
        jsAt(s"compiled/lichess.lobby${isProd ?? ".min"}.js", defer = true),
        embedJsUnsafe(
          s"""lichess=window.lichess||{};customWS=true;lichess_lobby=${safeJsonValue(
            Json.obj(
              "data" -> data,
              "playban" -> playban.map { pb =>
                Json.obj(
                  "minutes"          -> pb.mins,
                  "remainingSeconds" -> (pb.remainingSeconds + 3)
                )
              },
              "i18n" -> i18nJsObject(i18nKeys)
            )
          )}"""
        )
      ),
      moreCss = cssTag("lobby"),
      chessground = false,
      openGraph = lila.app.ui
        .OpenGraph(
          image = staticUrl("logo/lichess-tile-wide.png").some,
          twitterImage = staticUrl("logo/lichess-tile.png").some,
          title = "The best free, adless Chess server",
          url = netBaseUrl,
          description = trans.siteDescription.txt()
        )
        .some,
      deferJs = true
    ) {
      main(
        cls := List(
          "lobby"      -> true,
          "lobby-nope" -> (playban.isDefined || currentGame.isDefined)
        )
      )(
        div(cls := "lobby__table")(
          div(cls := "lobby__start")(
            ctx.blind option h2("Play"),
            a(
              href := routes.Setup.hookForm(),
              cls := List(
                "button button-metal config_hook" -> true,
                "disabled"                        -> (playban.isDefined || currentGame.isDefined || ctx.isBot)
              ),
              trans.createAGame()
            ),
            a(
              href := routes.Setup.friendForm(none),
              cls := List(
                "button button-metal config_friend" -> true,
                "disabled"                          -> currentGame.isDefined
              ),
              trans.playWithAFriend()
            ),
            a(
              href := routes.Setup.aiForm(),
              cls := List(
                "button button-metal config_ai" -> true,
                "disabled"                      -> currentGame.isDefined
              ),
              trans.playWithTheMachine()
            )
          ),
          div(cls := "lobby__counters")(
            ctx.blind option h2("Counters"),
            a(id := "nb_connected_players", href := ctx.noBlind.option(routes.User.list().toString))(
              trans.nbPlayers(nbPlaceholder)
            ),
            a(id := "nb_games_in_play", href := ctx.noBlind.option(routes.Tv.games().toString))(
              trans.nbGamesInPlay(nbPlaceholder)
            )
          )
        ),
        currentGame.map(bits.currentGameInfo) orElse
          playban.map(bits.playbanInfo) getOrElse {
          if (ctx.blind) blindLobby(blindGames)
          else bits.lobbyApp
        },
        div(cls := "lobby__side")(
          ctx.blind option h2("Highlights"),
          ctx.noKid option st.section(cls := "lobby__streams")(
            views.html.streamer.bits liveStreams streams,
            streams.live.streams.nonEmpty option a(href := routes.Streamer.index(), cls := "more")(
              trans.streamersMenu(),
              " »"
            )
          ),
          div(cls := "lobby__spotlights")(
            events.map(bits.spotlight),
            !ctx.isBot option frag(
              lila.tournament.Spotlight.select(tours, ctx.me, 3 - events.size) map {
                views.html.tournament.homepageSpotlight(_)
              },
              simuls.filter(isFeaturable) map views.html.simul.bits.homepageSpotlight
            )
          ),
          if (ctx.isAuth)
            div(cls := "timeline")(
              ctx.blind option h2("Timeline"),
              views.html.timeline entries userTimeline,
              userTimeline.nonEmpty option a(cls := "more", href := routes.Timeline.home())(
                trans.more(),
                " »"
              )
            )
          else
            div(cls := "about-side")(
              ctx.blind option h2("About"),
              trans.xIsAFreeYLibreOpenSourceChessServer(
                "Lichess",
                a(cls := "blue", href := routes.Plan.features())(trans.really.txt())
              ),
              " ",
              a(href := "/about")(trans.aboutX("Lichess"), "...")
            )
        ),
        featured map { g =>
          div(cls := "lobby__tv")(
            gameFen(Pov first g, tv = true),
            views.html.game.bits.vstext(Pov first g)(ctx.some)
          )
        },
        puzzle map { p =>
          div(cls := "lobby__puzzle", title := trans.clickToSolve.txt())(
            raw(p.html),
            div(cls := "vstext")(
              trans.puzzleOfTheDay(),
              br,
              p.color.fold(trans.whitePlays, trans.blackPlays)()
            )
          )
        },
        ctx.noBot option bits.underboards(tours, simuls, leaderboard, tournamentWinners),
        ctx.noKid option div(cls := "lobby__forum lobby__box")(
          a(cls := "lobby__box__top", href := routes.ForumCateg.index())(
            h2(cls := "title text", dataIcon := "d")(trans.latestForumPosts()),
            span(cls := "more")(trans.more(), " »")
          ),
          div(cls := "lobby__box__content")(
            views.html.forum.post recent forumRecent
          )
        ),
        bits.lastPosts(lastPost),
        div(cls := "lobby__support")(
          a(href := routes.Plan.index())(
            iconTag(patronIconChar),
            span(cls := "lobby__support__text")(
              strong(trans.patron.donate()),
              span(trans.patron.becomePatron())
            )
          ),
          a(href := "https://shop.spreadshirt.com/lichess-org")(
            iconTag(""),
            span(cls := "lobby__support__text")(
              strong("Swag Store"),
              span(trans.playChessInStyle())
            )
          )
        ),
        div(cls := "lobby__about")(
          ctx.blind option h2("About"),
          a(href := "/about")(trans.aboutX("Lichess")),
          a(href := "/faq")(trans.faq.faqAbbreviation()),
          a(href := "/contact")(trans.contact.contact()),
          a(href := "/mobile")(trans.mobileApp()),
          a(href := routes.Page.tos())(trans.termsOfService()),
          a(href := routes.Page.privacy())(trans.privacy()),
          a(href := routes.Page.source())(trans.sourceCode()),
          a(href := routes.Page.ads())("Ads"),
          views.html.base.bits.connectLinks
        )
      )
    }
  }

  private val i18nKeys = List(
    trans.realTime,
    trans.correspondence,
    trans.nbGamesInPlay,
    trans.player,
    trans.time,
    trans.joinTheGame,
    trans.cancel,
    trans.casual,
    trans.rated,
    trans.variant,
    trans.mode,
    trans.list,
    trans.graph,
    trans.filterGames,
    trans.youNeedAnAccountToDoThat,
    trans.oneDay,
    trans.nbDays,
    trans.aiNameLevelAiLevel,
    trans.yourTurn,
    trans.rating,
    trans.createAGame,
    trans.quickPairing,
    trans.lobby,
    trans.custom,
    trans.anonymous
  ).map(_.key)

  private val nbPlaceholder = strong("--,---")
}
