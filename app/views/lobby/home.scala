package views.html.lobby

import play.api.libs.json.{ Json, JsObject }
import play.twirl.api.Html

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.HTTPRequest
import lila.common.String.html.{ safeJson, safeJsonValue }
import lila.game.Pov

import controllers.routes

object home {

  def apply(
    data: JsObject,
    userTimeline: Vector[lila.timeline.Entry],
    forumRecent: List[lila.forum.MiniForumPost],
    tours: List[lila.tournament.Tournament],
    events: List[lila.event.Event],
    simuls: List[lila.simul.Simul],
    featured: Option[lila.game.Game],
    leaderboard: List[lila.user.User.LightPerf],
    tournamentWinners: List[lila.tournament.Winner],
    puzzle: Option[lila.puzzle.DailyPuzzle],
    streams: lila.streamer.LiveStreams.WithTitles,
    lastPost: List[lila.blog.MiniPost],
    playban: Option[lila.playban.TempBan],
    currentGame: Option[lila.app.mashup.Preload.CurrentGame],
    nbRounds: Int
  )(implicit ctx: Context) = views.html.base.layout(
    title = "",
    fullTitle = Some("lichess.org • " + trans.freeOnlineChess.txt()),
    baseline = Some(frag(
      a(id := "nb_connected_players", href := routes.User.list)(trans.nbPlayers(nbPlayersPlaceholder)),
      a(id := "nb_games_in_play", href := routes.Tv.games)(
        trans.nbGamesInPlay.plural(nbRounds, Html(s"<span>${nbRounds}</span>"))
      ),
      ctx.isMobileBrowser option {
        if (HTTPRequest isAndroid ctx.req) views.html.mobile.bits.googlePlayButton
        else if (HTTPRequest isIOS ctx.req) views.html.mobile.bits.appleStoreButton
        else emptyFrag
      }
    )),
    side = Some(frag(
      ctx.noKid option div(id := "streams_on_air")(views.html.streamer liveStreams streams),
      events map { bits.spotlight(_) },
      !ctx.isBot option frag(
        lila.tournament.Spotlight.select(tours, ctx.me, 3) map { views.html.tournament.homepageSpotlight(_) },
        simuls.find(_.spotlightable) take 2 map { views.html.simul.homepageSpotlight(_) } toList
      ),
      ctx.me map { u =>
        div(id := "timeline", dataHref := routes.Timeline.home)(
          views.html.timeline entries userTimeline,
          div(cls := "links")(
            userTimeline.size >= 8 option
              a(cls := "more", href := routes.Timeline.home)(trans.more(), " »")
          )
        )
      } getOrElse {
        div(cls := "about-side")(
          trans.xIsAFreeYLibreOpenSourceChessServer("Lichess", Html(s"""<a class="blue" href="${routes.Plan.features}">${trans.really.txt()}</a>""")),
          a(cls := "blue", href := "/about")(trans.aboutX("lichess.org"), "...")
        )
      }
    )),
    moreJs = frag(
      jsAt(s"compiled/lichess.lobby${isProd ?? (".min")}.js", async = true),
      embedJs {
        val playbanJs = htmlOrNull(playban)(pb => safeJson(Json.obj("minutes" -> pb.mins, "remainingSeconds" -> (pb.remainingSeconds + 3))))
        val gameJs = htmlOrNull(currentGame)(cg => safeJson(cg.json))
        val transJs = safeJsonValue(i18nJsObject(translations))
        s"""window.customWS = true; lichess_lobby = { data: ${safeJsonValue(data)}, playban: $playbanJs, currentGame: $gameJs, i18n: $transJs, }"""
      }
    ),
    moreCss = cssTag("home.css"),
    underchat = Some(frag(
      div(id := "featured_game")(
        featured map { g =>
          frag(
            gameFen(Pov first g, tv = true),
            views.html.game.bits.vstext(Pov first g)(ctx.some)
          )
        }
      )
    )),
    chessground = false,
    openGraph = lila.app.ui.OpenGraph(
      image = staticUrl("images/large_tile.png").some,
      title = "The best free, adless Chess server",
      url = netBaseUrl,
      description = trans.siteDescription.txt()
    ).some,
    asyncJs = true
  ) {
      frag(
        div(cls := List(
          "lobby_and_ground" -> true,
          "playban" -> playban.isDefined,
          "current_game" -> currentGame.isDefined
        ))(
          currentGame map { bits.currentGameInfo(_) },
          div(id := "hooks_wrap"),
          playban.map(ban => playbanInfo(ban.remainingSeconds)),
          div(id := "start_buttons", cls := "lichess_ground")(
            a(href := routes.Setup.hookForm, cls := List(
              "fat button config_hook" -> true,
              "disabled" -> (playban.isDefined || currentGame.isDefined || ctx.isBot)
            ), trans.createAGame()),
            a(href := routes.Setup.friendForm(none), cls := List(
              "fat button config_friend" -> true,
              "disabled" -> currentGame.isDefined
            ), trans.playWithAFriend()),
            a(href := routes.Setup.aiForm, cls := List(
              "fat button config_ai" -> true,
              "disabled" -> currentGame.isDefined
            ), trans.playWithTheMachine())
          )
        ),
        puzzle map { p =>
          div(id := "daily_puzzle", title := trans.clickToSolve.txt())(
            raw(p.html),
            div(cls := "vstext")(
              trans.puzzleOfTheDay(),
              br,
              p.color.fold(trans.whitePlays, trans.blackPlays)()
            )
          )
        },
        ctx.noBot option bits.underboards(tours, simuls, leaderboard, tournamentWinners),
        ctx.noKid option frag(
          div(cls := "new_posts undertable")(
            div(cls := "undertable_top")(
              a(cls := "more", href := routes.ForumCateg.index)(trans.more(), " »"),
              span(cls := "title text", dataIcon := "d")(trans.latestForumPosts())
            ),
            div(cls := "undertable_inner scroll-shadow-hard")(
              div(cls := "content")(views.html.forum.post recent forumRecent)
            )
          )
        ),
        bits.lastPosts(lastPost),
        div(cls := "donation undertable")(
          a(href := routes.Plan.index)(
            iconTag(patronIconChar),
            strong("Lichess Patron"),
            span(trans.directlySupportLichess())
          ),
          a(href := routes.Page.swag)(
            iconTag(""),
            strong("Swag Store"),
            span(trans.playChessInStyle())
          )
        ),
        div(cls := "about-footer")(a(href := "/about")(trans.aboutX("lichess.org")))
      )
    }

  private val translations = List(
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
  )

  private val nbPlayersPlaceholder = Html("<strong>-,---</strong>")
}
