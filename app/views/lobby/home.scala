package views.html.lobby

import play.api.libs.json.{ Json, JsObject }

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.HTTPRequest
import lila.common.String.html.safeJsonValue
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
    moreJs = frag(
      jsAt(s"compiled/lichess.lobby${isProd ?? (".min")}.js", async = true),
      embedJs {
        val playbanJs = playban.fold("null")(pb => safeJsonValue(Json.obj("minutes" -> pb.mins, "remainingSeconds" -> (pb.remainingSeconds + 3))))
        val transJs = safeJsonValue(i18nJsObject(translations))
        s"""lichess=window.lichess||{};customWS=true;lichess_lobby={data:${safeJsonValue(data)},playban:$playbanJs,i18n:$transJs}"""
      }
    ),
    moreCss = responsiveCssTag("lobby"),
    chessground = false,
    openGraph = lila.app.ui.OpenGraph(
      image = staticUrl("images/large_tile.png").some,
      title = "The best free, adless Chess server",
      url = netBaseUrl,
      description = trans.siteDescription.txt()
    ).some,
    asyncJs = true
  ) {
      main(cls := List(
        "lobby" -> true,
        "lobby-nope" -> (playban.isDefined || currentGame.isDefined)
      ))(
        div(cls := "lobby__table")(
          div(cls := "lobby__start")(
            a(href := routes.Setup.hookForm, cls := List(
              "button button-metal config_hook" -> true,
              "disabled" -> (playban.isDefined || currentGame.isDefined || ctx.isBot)
            ), trans.createAGame.frag()),
            a(href := routes.Setup.friendForm(none), cls := List(
              "button button-metal config_friend" -> true,
              "disabled" -> currentGame.isDefined
            ), trans.playWithAFriend.frag()),
            a(href := routes.Setup.aiForm, cls := List(
              "button button-metal config_ai" -> true,
              "disabled" -> currentGame.isDefined
            ), trans.playWithTheMachine.frag())
          ),
          div(cls := "lobby__counters")(
            a(id := "nb_connected_players", href := ctx.noBlind.option(routes.User.list.toString))(trans.nbPlayers.frag(nbPlayersPlaceholder)),
            a(id := "nb_games_in_play", href := ctx.noBlind.option(routes.Tv.games.toString))(
              trans.nbGamesInPlay.pluralFrag(nbRounds, strong(nbRounds))
            )
          )
        ),
        currentGame.map(bits.currentGameInfo) orElse
          playban.map(bits.playbanInfo) getOrElse
          bits.lobbyApp,
        div(cls := "lobby__side")(
          ctx.noKid option st.section(cls := "lobby__streams")(views.html.streamer.bits liveStreams streams),
          div(cls := "lobby__spotlights")(
            events.map(bits.spotlight),
            !ctx.isBot option frag(
              lila.tournament.Spotlight.select(tours, ctx.me, 3 - events.size) map { views.html.tournament.homepageSpotlight(_) },
              simuls.find(_.spotlightable).headOption map views.html.simul.bits.homepageSpotlight
            )
          ),
          ctx.me map { u =>
            div(cls := "timeline", dataHref := routes.Timeline.home)(
              views.html.timeline entries userTimeline,
              // userTimeline.size >= 8 option
              a(cls := "more", href := routes.Timeline.home)(trans.more.frag(), " »")
            )
          } getOrElse div(cls := "about-side")(
            trans.xIsAFreeYLibreOpenSourceChessServer.frag("Lichess", a(cls := "blue", href := routes.Plan.features)(trans.really.txt())),
            " ",
            a(cls := "blue", href := "/about")(trans.aboutX.frag("lichess.org"), "...")
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
              trans.puzzleOfTheDay.frag(),
              br,
              p.color.fold(trans.whitePlays, trans.blackPlays)()
            )
          )
        },
        ctx.noBot option bits.underboards(tours, simuls, leaderboard, tournamentWinners),
        ctx.noKid option div(cls := "lobby__forum lobby__box")(
          div(cls := "lobby__box__top")(
            span(cls := "title text", dataIcon := "d")(trans.latestForumPosts.frag()),
            a(cls := "more", href := routes.ForumCateg.index)(trans.more.frag(), " »")
          ),
          div(cls := "lobby__box__content scroll-shadow-hard")(
            views.html.forum.post recent forumRecent
          )
        ),
        bits.lastPosts(lastPost),
        div(cls := "lobby__support")(
          a(href := routes.Plan.index)(
            iconTag(patronIconChar),
            span(cls := "lobby__support__text")(
              strong("Lichess Patron"),
              span(trans.directlySupportLichess.frag())
            )
          ),
          a(href := routes.Page.swag)(
            iconTag(""),
            span(cls := "lobby__support__text")(
              strong("Swag Store"),
              span(trans.playChessInStyle.frag())
            )
          )
        ),
        div(cls := "lobby__about")(
          a(href := "/about")(trans.aboutX.frag("lichess.org")),
          a(href := "/faq")("FAQ"),
          a(href := "/contact")(trans.contact.frag()),
          ctx.noKid option frag(
            a(href := "/mobile")(trans.mobileApp.frag()),
            a(href := "/developers")(trans.webmasters.frag()),
            a(href := "/help/contribute")(trans.contribute.frag()),
            a(href := "/patron")(trans.donate.frag())
          ),
          a(href := "/thanks")(trans.thankYou.frag()),
          a(href := routes.Page.tos)(trans.termsOfService.frag()),
          a(href := routes.Page.privacy)(trans.privacy.frag()),
          a(href := "https://database.lichess.org/")(trans.database.frag()),
          a(href := "https://github.com/ornicar/lila")(trans.sourceCode.frag())
        )
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

  private val nbPlayersPlaceholder = strong("--,---")
}
