package views

import play.api.libs.json.Json

import lila.app.UiEnv.{ *, given }
import lila.app.mashup.Preload.Homepage
import lila.core.perf.UserWithPerfs
import lila.web.ui.lobby

def home(homepage: Homepage)(using ctx: Context) =
  import homepage.*
  lobby
    .page(siteName)
    .js(
      PageModule(
        "lobby",
        Json
          .obj(
            "data" -> data,
            "showRatings" -> ctx.pref.showRatings
          )
          .add("hasUnreadLichessMessage", hasUnreadLichessMessage)
          .add("playban", playban.map(lila.playban.TempBan.lobbyJson))
      )
    )
    .graph(
      OpenGraph(
        image = assetFullUrl("logo/lichess-tile-wide.png").some,
        title = "The best free, adless Chess server",
        url = netBaseUrl.into(Url),
        description = trans.site.siteDescription.txt()
      )
    ):
      given Option[UserWithPerfs] = homepage.me
      val donateLink = lobby.donateLink
      val swagLink = lobby.swagLink
      main(
        cls := List(
          "lobby" -> true,
          "lobby-nope" -> (playban.isDefined || currentGame.isDefined || homepage.hasUnreadLichessMessage)
        )
      )(
        div(cls := "lobby__side")(
          ctx.blind.option(h2(trans.nvui.featuredEvents())),
          ctx.kid.no.option(views.streamer.bits.liveStreams(streams)),
          div(cls := "lobby__spotlights"):
            val eventTags = events.map(views.event.spotlight)
            val relayTags = views.relay.ui.spotlight(relays)
            frag(
              eventTags,
              relayTags,
              ctx.noBot.option:
                val nbManual = eventTags.size + relayTags.size
                val simulBBB = simuls.find(isFeaturable(_) && nbManual < 4)
                val nbForced = nbManual + simulBBB.size.toInt
                val tourBBBs = if nbForced > 3 then 0 else if nbForced == 3 then 1 else 3 - nbForced
                frag(
                  lila.tournament.Spotlight.select(tours, tourBBBs).map {
                    views.tournament.list.homepageSpotlight(_)
                  },
                  swiss.ifTrue(nbForced < 3).map(views.swiss.ui.homepageSpotlight),
                  simulBBB.map(views.simul.ui.homepageSpotlight)
                )
            )
          ,
          views.clas.ui.lobby(classes),
          if userTimeline.nonEmpty then views.timeline.lobby(userTimeline)
          else lobby.about
        ),
        currentGame
          .map(c => lobby.currentGameInfo(c.pov, c.opponent))
          .orElse:
            hasUnreadLichessMessage.option(lobby.showUnreadLichessMessage)
          .orElse:
            playban.map(ban => lobby.playbanInfo(timeRemaining(ban.endsAt)))
          .getOrElse:
            if ctx.blind then views.round.ui.blindLobby(blindGames) else lobby.app
        ,
        lobby.table,
        div(cls := "lobby__support")(donateLink, swagLink),
        div(cls := "lobby__tv")(
          donateLink,
          featured.map(g => views.game.mini(Pov.naturalOrientation(g), tv = true))
        ),
        div(cls := "lobby__puzzle")(
          swagLink,
          puzzle.map(p => views.puzzle.bits.dailyLink(p)())
        ),
        views.ublog.ui.homeCarousel(ublogPosts),
        views.feed.lobbyUpdates(lastUpdates),
        lobby.toursAndSimuls(
          views.tournament.ui.enterable(tours),
          simuls.nonEmpty.option(views.simul.ui.allCreated(simuls, withName = false))
        ),
        lobby.aboutLinks
      )
