package views.html.lobby

import controllers.routes
import play.api.libs.json.Json

import lila.api.Context
import lila.app.mashup.Preload.Homepage
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.LangPath
import lila.common.String.html.safeJsonValue
import lila.game.Pov

object home:

  def apply(homepage: Homepage)(using ctx: Context) =
    import homepage.*
    views.html.base.layout(
      title = "",
      fullTitle = Some {
        s"$siteName • ${trans.freeOnlineChess.txt()}"
      },
      moreJs = frag(
        jsModule("lobby"),
        embedJsUnsafeLoadThen(
          s"""LichessLobby(${safeJsonValue(
              Json
                .obj(
                  "data" -> data,
                  "i18n" -> i18nJsObject(i18nKeys)
                )
                .add("hideRatings" -> !ctx.pref.showRatings)
                .add("hasUnreadLichessMessage", hasUnreadLichessMessage)
                .add(
                  "playban",
                  playban.map { pb =>
                    Json.obj("minutes" -> pb.mins, "remainingSeconds" -> (pb.remainingSeconds + 3))
                  }
                )
            )})"""
        )
      ),
      moreCss = cssTag("lobby"),
      chessground = false,
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
      main(
        cls := List(
          "lobby"      -> true,
          "lobby-nope" -> (playban.isDefined || currentGame.isDefined || homepage.hasUnreadLichessMessage)
        )
      )(
        div(cls := "lobby__table")(
          (ctx.isAnon && ctx.pref.bg == lila.pref.Pref.Bg.SYSTEM) option div(
            cls   := "bg-switch",
            title := "Dark mode"
          )(
            div(cls := "bg-switch__track"),
            div(cls := "bg-switch__thumb")
          ),
          div(cls := "lobby__start")(
            button(cls := "button button-metal", tpe := "button", trans.createAGame()),
            button(cls := "button button-metal", tpe := "button", trans.playWithAFriend()),
            button(cls := "button button-metal", tpe := "button", trans.playWithTheMachine())
          )
        ),
        currentGame.map(bits.currentGameInfo) orElse
          hasUnreadLichessMessage.option(bits.showUnreadLichessMessage) orElse
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
            relays.map(views.html.relay.bits.spotlight),
            !ctx.isBot option frag(
              lila.tournament.Spotlight.select(tours, ctx.me, 3 - events.size) map {
                views.html.tournament.homepageSpotlight(_)
              },
              swiss map views.html.swiss.bits.homepageSpotlight,
              simuls.filter(isFeaturable) map views.html.simul.bits.homepageSpotlight
            )
          ),
          if (ctx.isAuth)
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
              trans.xIsAFreeYLibreOpenSourceChessServer(
                "Lichess",
                a(cls := "blue", href := routes.Plan.features)(trans.really.txt())
              ),
              " ",
              a(href := "/about")(trans.aboutX("Lichess"), "...")
            )
        ),
        featured map { g =>
          div(cls := "lobby__tv")(
            views.html.game.mini(Pov naturalOrientation g, tv = true)
          )
        },
        puzzle map { p =>
          views.html.puzzle.embed.dailyLink(p)(cls := "lobby__puzzle")
        },
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
          a(href := routes.Page.tos)(trans.termsOfService()),
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
    trans.black
  )
