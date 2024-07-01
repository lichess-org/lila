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
        s"lishogi.${if (isProd && !isStage) "org" else "dev"} - ${trans.freeOnlineShogi.txt()}"
      },
      moreJs = frag(
        jsModule("lobby", defer = true),
        embedJsUnsafe(
          s"""lishogi=window.lishogi||{};customWS=true;lishogi_lobby=${safeJsonValue(
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
      shogiground = false,
      openGraph = lila.app.ui
        .OpenGraph(
          image = staticUrl("logo/lishogi-tile-wide.png").some,
          twitterImage = staticUrl("logo/lishogi-tile.png").some,
          title = trans.freeOnlineShogi.txt(),
          url = netBaseUrl,
          description = trans.siteDescription.txt()
        )
        .some,
      deferJs = true,
      canonicalPath = lila.common.CanonicalPath("/").some,
      withHrefLangs = lila.i18n.LangList.All.some
    ) {
      main(
        cls := List(
          "lobby"      -> true,
          "lobby-nope" -> (playban.isDefined || currentGame.isDefined)
        )
      )(
        div(cls := "lobby__table")(
          div(cls := "lobby__start")(
            ctx.blind option h2(trans.play()),
            a(
              href := routes.Setup.hookForm,
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
              href := routes.Setup.aiForm,
              cls := List(
                "button button-metal config_ai" -> true,
                "disabled"                      -> currentGame.isDefined
              ),
              trans.playWithTheMachine()
            )
          ),
          div(cls := "lobby__counters")(
            ctx.blind option h2("Counters"),
            a(
              id   := "nb_connected_players",
              href := ctx.noBlind.option(routes.User.list.url)
            )(
              trans.nbPlayers.plural(
                homepage.counters.members,
                strong(dataCount := homepage.counters.members)(homepage.counters.members.localize)
              )
            ),
            a(
              id   := "nb_games_in_play",
              href := ctx.noBlind.option(langHref(routes.Tv.games))
            )(
              trans.nbGamesInPlay.plural(
                homepage.counters.rounds,
                strong(dataCount := homepage.counters.rounds)(homepage.counters.rounds.localize)
              )
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
          div(cls := "lobby__spotlights")(bits.spotlights(events, simuls, tours)),
          (if (ctx.isAuth)
             div(cls := "timeline")(
               ctx.blind option h2("Timeline"),
               views.html.timeline entries userTimeline,
               userTimeline.nonEmpty option a(cls := "more", href := routes.Timeline.home)(
                 trans.more(),
                 " »"
               )
             )
           else
             div(cls := "about-side")(
               ctx.blind option h2("About"),
               trans.xIsAFreeYLibreOpenSourceShogiServer(
                 "Lishogi",
                 a(cls := "blue", href := routes.Plan.features)(trans.really.txt())
               )
             )),
          div(cls := "lobby__support")(
            a(href := langHref(routes.Plan.index))(
              iconTag(patronIconChar),
              span(cls := "lobby__support__text")(
                if (ctx.me.exists(_.isPatron))
                  frag(
                    strong(trans.patron.lishogiPatron()),
                    span(trans.patron.thankYou())
                  )
                else
                  frag(
                    strong(trans.patron.donate()),
                    ctx.isAuth option span(trans.patron.becomePatron())
                  )
              )
            )
          )
        ),
        featured map { g =>
          a(cls := "lobby__tv", href := routes.Tv.index)(
            gameSfen(Pov first g, withLink = false, tv = true),
            views.html.game.bits.vstext(Pov first g)
          )
        },
        puzzle map { p =>
          views.html.puzzle.embed.dailyLink(p)(ctx.lang)(cls := "lobby__puzzle")
        },
        bits.rankings(leaderboard, tournamentWinners),
        bits.shogiDescription,
        bits.lastPosts(lastPost),
        bits.tournaments(tours),
        bits.studies(studies),
        bits.forumRecent(forumRecent),
        div(cls := "lobby__about")(
          ctx.blind option h2("About"),
          a(href := "/about")(trans.aboutX("Lishogi")),
          a(href := "/faq")(trans.faq.faqAbbreviation()),
          a(href := "/contact")(trans.contact.contact()),
          // a(href := "/mobile")(trans.mobileApp()),
          ctx.noKid option a(href := routes.Page.resources)(trans.shogiResources()),
          a(href := routes.Page.tos)(trans.termsOfService()),
          a(href := routes.Page.privacy)(trans.privacy()),
          a(href := routes.Plan.index)(trans.patron.donate()),
          a(href := routes.Page.source)(trans.sourceCode()),
          views.html.base.bits.connectLinks
        )
      )
    }
  }

  private val i18nKeys = List(
    trans.black,
    trans.white,
    trans.sente,
    trans.gote,
    trans.shitate,
    trans.uwate,
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
    trans.standard,
    trans.minishogi,
    trans.chushogi,
    trans.annanshogi,
    trans.kyotoshogi,
    trans.checkshogi,
    trans.ultrabullet,
    trans.bullet,
    trans.blitz,
    trans.rapid,
    trans.classical,
    trans.mode,
    trans.list,
    trans.graph,
    trans.filterGames,
    trans.youNeedAnAccountToDoThat,
    trans.oneDay,
    trans.nbDays,
    trans.levelX,
    trans.yourTurn,
    trans.rating,
    trans.createAGame,
    trans.startPosition,
    trans.presets,
    trans.lobby,
    trans.custom,
    trans.anonymous,
    trans.readyToPlay,
    trans.xPlays
  ).map(_.key)
}
