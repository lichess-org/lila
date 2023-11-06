package views.html.lobby

import controllers.routes
import play.api.libs.json.Json

import lila.app.mashup.Preload.Homepage
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.LangPath
import lila.common.String.html.safeJsonValue
import lila.game.Pov

object home:

  def apply(homepage: Homepage)(using ctx: PageContext): Frag =
    import homepage.*
    views.html.base.layout(
      title = "",
      fullTitle = s"$siteName • ${trans.freeOnlineChess.txt()}".some,
      moreJs = jsModuleInit(
        "lobby",
        Json
          .obj("data" -> data, "i18n" -> i18nJsObject(i18nKeys))
          .add("hideRatings" -> !ctx.pref.showRatings)
          .add("hasUnreadLichessMessage", hasUnreadLichessMessage)
          .add(
            "playban",
            playban.map: pb =>
              Json.obj("minutes" -> pb.mins, "remainingSeconds" -> (pb.remainingSeconds + 3))
          )
      ),
      moreCss = cssTag("lobby"),
      openGraph = lila.app.ui
        .OpenGraph(
          image = assetUrl("logo/lichess-tile-wide.png").some,
          twitterImage = assetUrl("logo/lichess-tile.png").some,
          title = "The best free, adless Chess server",
          url = netBaseUrl,
          description = trans.siteDescription.txt()
        )
        .some,
      withHrefLangs = LangPath("/").some
    ) {
      given Option[lila.user.User.WithPerfs] = homepage.me
      main(
        cls := List(
          "lobby"      -> true,
          "lobby-nope" -> (playban.isDefined || currentGame.isDefined || homepage.hasUnreadLichessMessage)
        )
      )(
        div(cls := "lobby__table")(
          div(cls := "lobby__start")(
            button(cls := "button button-metal", tpe := "button", trans.createAGame()),
            button(cls := "button button-metal", tpe := "button", trans.playWithAFriend()),
            button(cls := "button button-metal", tpe := "button", trans.playWithTheMachine())
          )
        ),
        currentGame
          .map(bits.currentGameInfo)
          .orElse:
            hasUnreadLichessMessage.option(bits.showUnreadLichessMessage)
          .orElse:
            playban.map(bits.playbanInfo)
          .getOrElse:
            if ctx.blind then blindLobby(blindGames) else bits.lobbyApp
        ,
        div(cls := "lobby__side")(
          ctx.blind option h2("Highlights"),
          ctx.kid.no option st.section(cls := "lobby__streams")(
            views.html.streamer.bits liveStreams streams,
            streams.live.streams.nonEmpty option a(href := routes.Streamer.index(), cls := "more")(
              trans.streamersMenu(),
              " »"
            )
          ),
          div(cls := "lobby__spotlights")(
            events.map(bits.spotlight),
            relays.map(views.html.relay.bits.spotlight),
            !ctx.isBot option {
              val nbManual = events.size + relays.size
              val simulBBB = simuls.find(isFeaturable(_) && nbManual < 4)
              val nbForced = nbManual + simulBBB.size.toInt
              val tourBBBs = if nbForced > 3 then 0 else if nbForced == 3 then 1 else 3 - nbForced
              frag(
                lila.tournament.Spotlight.select(tours, tourBBBs).map {
                  views.html.tournament.homepageSpotlight(_)
                },
                swiss.ifTrue(nbForced < 3) map views.html.swiss.bits.homepageSpotlight,
                simulBBB map views.html.simul.bits.homepageSpotlight
              )
            }
          ),
          if ctx.isAuth then
            div(cls := "lobby__timeline")(
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
              trans.xIsAFreeYLibreOpenSourceChessServer(
                "Lichess",
                a(cls := "blue", href := routes.Plan.features)(trans.really.txt())
              ),
              " ",
              a(href := "/about")(trans.aboutX("Lichess"), "...")
            )
        ),
        featured.map: g =>
          div(cls := "lobby__tv"):
            views.html.game.mini(Pov naturalOrientation g, tv = true)
        ,
        puzzle.map: p =>
          views.html.puzzle.embed.dailyLink(p)(cls := "lobby__puzzle"),
        bits.lastPosts(lastPost, ublogPosts),
        ctx.noBot option bits.underboards(tours, simuls, leaderboard, tournamentWinners),
        div(cls := "lobby__support")(
          a(href := routes.Plan.index)(
            iconTag(patronIconChar),
            span(cls := "lobby__support__text")(
              strong(trans.patron.donate()),
              span(trans.patron.becomePatron())
            )
          ),
          a(href := "https://shop.spreadshirt.com/lichess-org")(
            iconTag(licon.Tshirt),
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
          a(href := routes.ContentPage.tos)(trans.termsOfService()),
          a(href := "/privacy")(trans.privacy()),
          a(href := "/source")(trans.sourceCode()),
          a(href := "/ads")("Ads"),
          views.html.base.bits.connectLinks
        )
      )
    }

  private val i18nKeys = List(
    trans.realTime,
    trans.correspondence,
    trans.unlimited,
    trans.timeControl,
    trans.incrementInSeconds,
    trans.minutesPerSide,
    trans.daysPerTurn,
    trans.ratingRange,
    trans.nbPlayers,
    trans.nbGamesInPlay,
    trans.player,
    trans.time,
    trans.joinTheGame,
    trans.cancel,
    trans.casual,
    trans.rated,
    trans.perfRatingX,
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
    trans.playWithAFriend,
    trans.playWithTheMachine,
    trans.strength,
    trans.pasteTheFenStringHere,
    trans.quickPairing,
    trans.lobby,
    trans.custom,
    trans.anonymous,
    trans.side,
    trans.white,
    trans.randomColor,
    trans.black,
    trans.boardEditor
  )
