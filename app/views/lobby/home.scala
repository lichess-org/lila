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
    nbRounds: Int,
    blindGames: List[Pov] // only in blind mode
  )(implicit ctx: Context) = views.html.base.layout(
    title = "",
    fullTitle = Some {
      s"lichess.${if (isProd && !isStage) "org" else "dev"} • ${trans.freeOnlineChess.txt()}"
    },
    moreJs = frag(
      jsAt(s"compiled/lichess.lobby${isProd ?? (".min")}.js", defer = true),
      embedJsUnsafe(
        s"""lichess=window.lichess||{};customWS=true;lichess_lobby=${
          safeJsonValue(Json.obj(
            "data" -> data,
            "playban" -> playban.map { pb =>
              Json.obj(
                "minutes" -> pb.mins,
                "remainingSeconds" -> (pb.remainingSeconds + 3)
              )
            },
            "i18n" -> i18nJsObject(translations)
          ))
        }"""
      )
    ),
    moreCss = cssTag("lobby"),
    chessground = false,
    openGraph = lila.app.ui.OpenGraph(
      image = staticUrl("images/large_tile.png").some,
      title = "The best free, adless Chess server",
      url = netBaseUrl,
      description = trans.siteDescription.txt()
    ).some,
    deferJs = true
  ) {
      main(cls := List(
        "lobby" -> true,
        "lobby-nope" -> (playban.isDefined || currentGame.isDefined)
      ))(
        div(cls := "lobby__table")(
          div(cls := "lobby__start")(
            ctx.blind option h2("Play"),
            a(href := routes.Setup.hookForm, cls := List(
              "button button-metal config_hook" -> true,
              "disabled" -> (playban.isDefined || currentGame.isDefined || ctx.isBot)
            ), trans.createAGame()),
            a(href := routes.Setup.friendForm(none), cls := List(
              "button button-metal config_friend" -> true,
              "disabled" -> currentGame.isDefined
            ), trans.playWithAFriend()),
            a(href := routes.Setup.aiForm, cls := List(
              "button button-metal config_ai" -> true,
              "disabled" -> currentGame.isDefined
            ), trans.playWithTheMachine())
          ),
          div(cls := "lobby__counters")(
            ctx.blind option h2("Counters"),
            a(id := "nb_connected_players", href := ctx.noBlind.option(routes.User.list.toString))(trans.nbPlayers(nbPlayersPlaceholder)),
            a(id := "nb_games_in_play", href := ctx.noBlind.option(routes.Tv.games.toString))(
              trans.nbGamesInPlay.plural(nbRounds, strong(nbRounds.localize))
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
          ctx.noKid option st.section(cls := "lobby__streams")(views.html.streamer.bits liveStreams streams),
          div(cls := "lobby__spotlights")(
            events.map(bits.spotlight),
            !ctx.isBot option frag(
              lila.tournament.Spotlight.select(tours, ctx.me, 3 - events.size) map { views.html.tournament.homepageSpotlight(_) },
              simuls.find(_.spotlightable).filter(lila.simul.Env.current.featurable).headOption map views.html.simul.bits.homepageSpotlight
            )
          ),
          ctx.me map { u =>
            div(cls := "timeline", dataHref := routes.Timeline.home)(
              ctx.blind option h2("Timeline"),
              views.html.timeline entries userTimeline,
              // userTimeline.size >= 8 option
              a(cls := "more", href := routes.Timeline.home)(trans.more(), " »")
            )
          } getOrElse div(cls := "about-side")(
            ctx.blind option h2("About"),
            trans.xIsAFreeYLibreOpenSourceChessServer("Lichess", a(cls := "blue", href := routes.Plan.features)(trans.really.txt())),
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
          div(cls := "lobby__box__top")(
            h2(cls := "title text", dataIcon := "d")(trans.latestForumPosts()),
            a(cls := "more", href := routes.ForumCateg.index)(trans.more(), " »")
          ),
          div(cls := "lobby__box__content")(
            views.html.forum.post recent forumRecent
          )
        ),
        bits.lastPosts(lastPost),
        div(cls := "lobby__support")(
          a(href := routes.Plan.index)(
            iconTag(patronIconChar),
            span(cls := "lobby__support__text")(
              strong("Lichess Patron"),
              span(trans.directlySupportLichess())
            )
          ),
          a(href := routes.Page.swag)(
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
          a(href := "/faq")("FAQ"),
          a(href := "/contact")(trans.contact()),
          a(href := "/mobile")(trans.mobileApp()),
          a(href := routes.Page.tos)(trans.termsOfService()),
          a(href := routes.Page.privacy)(trans.privacy()),
          a(href := routes.Page.source)(trans.sourceCode())
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
