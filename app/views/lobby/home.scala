package views.lobby

import play.api.libs.json.Json

import lila.app.mashup.Preload.Homepage
import lila.app.templating.Environment.{ *, given }

import lila.core.perf.UserWithPerfs

object home:

  def apply(homepage: Homepage)(using ctx: PageContext): Frag =
    import homepage.*
    views.base.layout(
      title = "",
      fullTitle = s"$siteName • ${trans.site.freeOnlineChess.txt()}".some,
      pageModule = PageModule(
        "lobby",
        Json
          .obj(
            "data"                    -> data,
            "i18n"                    -> i18nJsObject(i18nKeys),
            "showRatings"             -> ctx.pref.showRatings,
            "hasUnreadLichessMessage" -> hasUnreadLichessMessage
          )
          .add(
            "playban",
            playban.map: pb =>
              Json.obj("minutes" -> pb.mins, "remainingSeconds" -> (pb.remainingSeconds + 3))
          )
      ).some,
      moreCss = cssTag("lobby"),
      openGraph = OpenGraph(
        image = assetUrl("logo/lichess-tile-wide.png").some,
        twitterImage = assetUrl("logo/lichess-tile.png").some,
        title = "The best free, adless Chess server",
        url = netBaseUrl.value,
        description = trans.site.siteDescription.txt()
      ).some,
      withHrefLangs = lila.ui.LangPath("/").some
    ) {
      given Option[UserWithPerfs] = homepage.me
      main(
        cls := List(
          "lobby"      -> true,
          "lobby-nope" -> (playban.isDefined || currentGame.isDefined || homepage.hasUnreadLichessMessage)
        )
      )(
        div(cls := "lobby__table")(
          div(cls := "lobby__start")(
            button(cls := "button button-metal", tpe := "button", trans.site.createAGame()),
            button(cls := "button button-metal", tpe := "button", trans.site.playWithAFriend()),
            button(cls := "button button-metal", tpe := "button", trans.site.playWithTheMachine())
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
          ctx.blind.option(h2("Highlights")),
          ctx.kid.no.option(
            st.section(cls := "lobby__streams")(
              views.streamer.bits.liveStreams(streams),
              streams.live.streams.nonEmpty.option(
                a(href := routes.Streamer.index(), cls := "more")(
                  trans.site.streamersMenu(),
                  " »"
                )
              )
            )
          ),
          div(cls := "lobby__spotlights")(
            events.map(bits.spotlight),
            views.relay.bits.spotlight(relays),
            ctx.noBot.option {
              val nbManual = events.size + relays.size
              val simulBBB = simuls.find(isFeaturable(_) && nbManual < 4)
              val nbForced = nbManual + simulBBB.size.toInt
              val tourBBBs = if nbForced > 3 then 0 else if nbForced == 3 then 1 else 3 - nbForced
              frag(
                lila.tournament.Spotlight.select(tours, tourBBBs).map {
                  views.tournament.homepageSpotlight(_)
                },
                swiss.ifTrue(nbForced < 3).map(views.swiss.bits.homepageSpotlight),
                simulBBB.map(views.simul.bits.homepageSpotlight)
              )
            }
          ),
          if ctx.isAuth then
            div(cls := "lobby__timeline")(
              ctx.blind.option(h2("Timeline")),
              views.timeline.entries(userTimeline),
              userTimeline.nonEmpty.option(
                a(cls := "more", href := routes.Timeline.home)(
                  trans.site.more(),
                  " »"
                )
              )
            )
          else
            div(cls := "about-side")(
              ctx.blind.option(h2("About")),
              trans.site.xIsAFreeYLibreOpenSourceChessServer(
                "Lichess",
                a(cls := "blue", href := routes.Plan.features)(trans.site.really.txt())
              ),
              " ",
              a(href := "/about")(trans.site.aboutX("Lichess"), "...")
            )
        ),
        featured.map: g =>
          div(cls := "lobby__tv"):
            views.game.mini(Pov.naturalOrientation(g), tv = true)
        ,
        puzzle.map: p =>
          views.puzzle.embed.dailyLink(p)(cls := "lobby__puzzle"),
        div(cls := "lobby__blog ublog-post-cards"):
          ublogPosts
            .filter(_.isLichess || ctx.kid.no)
            .take(3)
            .map:
              views.ublog.postUi
                .card(_, showAuthor = views.ublog.postUi.ShowAt.bottom, showIntro = false)
        ,
        ctx.noBot.option(bits.underboards(tours, simuls, leaderboard, tournamentWinners)),
        div(cls := "lobby__feed"):
          views.feed.lobbyUpdates(lastUpdates)
        ,
        div(cls := "lobby__support")(
          a(href := routes.Plan.index)(
            iconTag(patronIconChar),
            span(cls := "lobby__support__text")(
              strong(trans.patron.donate()),
              span(trans.patron.becomePatron())
            )
          ),
          a(href := "https://shop.spreadshirt.com/lichess-org")(
            iconTag(Icon.Tshirt),
            span(cls := "lobby__support__text")(
              strong("Swag Store"),
              span(trans.site.playChessInStyle())
            )
          )
        ),
        div(cls := "lobby__about")(
          ctx.blind.option(h2("About")),
          a(href := "/about")(trans.site.aboutX("Lichess")),
          a(href := "/faq")(trans.faq.faqAbbreviation()),
          a(href := "/contact")(trans.contact.contact()),
          a(href := "/mobile")(trans.site.mobileApp()),
          a(href := routes.Cms.tos)(trans.site.termsOfService()),
          a(href := "/privacy")(trans.site.privacy()),
          a(href := "/source")(trans.site.sourceCode()),
          a(href := "/ads")("Ads"),
          views.base.bits.connectLinks
        )
      )
    }

  private val i18nKeys = List(
    trans.site.realTime,
    trans.site.correspondence,
    trans.site.unlimited,
    trans.site.timeControl,
    trans.site.incrementInSeconds,
    trans.site.minutesPerSide,
    trans.site.daysPerTurn,
    trans.site.ratingRange,
    trans.site.nbPlayers,
    trans.site.nbGamesInPlay,
    trans.site.player,
    trans.site.time,
    trans.site.joinTheGame,
    trans.site.cancel,
    trans.site.casual,
    trans.site.rated,
    trans.site.perfRatingX,
    trans.site.variant,
    trans.site.mode,
    trans.site.list,
    trans.site.graph,
    trans.site.filterGames,
    trans.site.youNeedAnAccountToDoThat,
    trans.site.oneDay,
    trans.site.nbDays,
    trans.site.aiNameLevelAiLevel,
    trans.site.yourTurn,
    trans.site.rating,
    trans.site.createAGame,
    trans.site.playWithAFriend,
    trans.site.playWithTheMachine,
    trans.site.strength,
    trans.site.pasteTheFenStringHere,
    trans.site.quickPairing,
    trans.site.lobby,
    trans.site.custom,
    trans.site.anonymous,
    trans.site.side,
    trans.site.white,
    trans.site.randomColor,
    trans.site.black,
    trans.site.boardEditor
  )
