package lila.app
package mashup

import com.github.blemale.scaffeine.AsyncLoadingCache
import play.api.libs.json.*

import lila.core.game.Game
import lila.core.perf.UserWithPerfs
import lila.event.Event
import lila.playban.TempBan
import lila.simul.{ Simul, SimulIsFeaturable }
import lila.streamer.LiveStreams
import lila.swiss.Swiss
import lila.timeline.Entry
import lila.tournament.Tournament
import lila.ublog.UblogPost
import lila.user.{ LightUserApi, Me, User }

final class Preload(
    tv: lila.tv.Tv,
    gameRepo: lila.game.GameRepo,
    perfsRepo: lila.user.UserPerfsRepo,
    timelineApi: lila.timeline.EntryApi,
    liveStreamApi: lila.streamer.LiveStreamApi,
    dailyPuzzle: lila.puzzle.DailyPuzzle.Try,
    lobbyApi: lila.api.LobbyApi,
    playbanApi: lila.playban.PlaybanApi,
    lightUserApi: LightUserApi,
    roundProxy: lila.round.GameProxyRepo,
    simulIsFeaturable: SimulIsFeaturable,
    getLastUpdates: lila.feed.Feed.GetLastUpdates,
    lastPostsCache: AsyncLoadingCache[Unit, List[UblogPost.PreviewPost]],
    msgApi: lila.msg.MsgApi,
    relayListing: lila.relay.RelayListing,
    notifyApi: lila.notify.NotifyApi
)(using Executor):

  import Preload.*

  def apply(
      tours: Fu[List[Tournament]],
      swiss: Option[Swiss],
      events: Fu[List[Event]],
      simuls: Fu[List[Simul]],
      streamerSpots: Int
  )(using ctx: Context): Fu[Homepage] = for
    nbNotifications <- ctx.me.so(notifyApi.unreadCount(_))
    withPerfs <- ctx.user.soFu(perfsRepo.withPerfs)
    given Option[UserWithPerfs] = withPerfs
    (
      (
        (
          (
            (
              (((((((data, povs), tours), events), simuls), feat), entries), puzzle),
              streams
            ),
            playban
          ),
          blindGames
        ),
        ublogPosts
      ),
      lichessMsg
    ) <- lobbyApi.apply
      .mon(_.lobby.segment("lobbyApi"))
      .zip(tours.mon(_.lobby.segment("tours")))
      .zip(events.mon(_.lobby.segment("events")))
      .zip(simuls.mon(_.lobby.segment("simuls")))
      .zip(tv.getBestGame.mon(_.lobby.segment("tvBestGame")))
      .zip((ctx.userId.so(timelineApi.userEntries)).mon(_.lobby.segment("timeline")))
      .zip((ctx.noBot.so(dailyPuzzle())).mon(_.lobby.segment("puzzle")))
      .zip(
        ctx.kid.no.so(
          liveStreamApi.all
            .dmap(_.homepage(streamerSpots, ctx.acceptLanguages).withTitles(lightUserApi))
            .mon(_.lobby.segment("streams"))
        )
      )
      .zip((ctx.userId.so(playbanApi.currentBan)).mon(_.lobby.segment("playban")))
      .zip(ctx.blind.so(ctx.me).so(roundProxy.urgentGames))
      .zip(lastPostsCache.get {})
      .zip(
        ctx.userId
          .ifTrue(nbNotifications > 0)
          .filterNot(liveStreamApi.isStreaming)
          .so(msgApi.hasUnreadLichessMessage)
      )
    (currentGame, _) <- (ctx.me
      .soUse(currentGameMyTurn(povs, lightUserApi.sync)))
      .mon(_.lobby.segment("currentGame"))
      .zip:
        lightUserApi
          .preloadMany(entries.flatMap(_.userIds).toList)
          .mon(_.lobby.segment("lightUsers"))
  yield Homepage(
    data,
    entries,
    tours,
    swiss,
    events,
    relayListing.spotlight,
    simuls,
    feat,
    puzzle,
    streams.excludeUsers(events.flatMap(_.hostedBy)),
    playban,
    currentGame,
    simulIsFeaturable,
    blindGames,
    getLastUpdates(),
    ublogPosts,
    withPerfs,
    hasUnreadLichessMessage = lichessMsg
  )

  def currentGameMyTurn(using me: Me): Fu[Option[CurrentGame]] =
    gameRepo
      .playingRealtimeNoAi(me)
      .flatMap:
        _.map { roundProxy.pov(_, me) }.parallel.dmap(_.flatten)
      .flatMap:
        currentGameMyTurn(_, lightUserApi.sync)

  private def currentGameMyTurn(povs: List[Pov], lightUser: lila.core.LightUser.GetterSync)(using
      me: Me
  ): Fu[Option[CurrentGame]] =
    ~povs.collectFirst:
      case p1 if p1.game.nonAi && p1.game.hasClock && p1.isMyTurn =>
        roundProxy.pov(p1.gameId, me).dmap(_ | p1).map { pov =>
          val opponent = lila.game.Namer.playerTextBlocking(pov.opponent)(using lightUser)
          CurrentGame(pov = pov, opponent = opponent).some
        }

object Preload:

  case class Homepage(
      data: JsObject,
      userTimeline: Vector[Entry],
      tours: List[Tournament],
      swiss: Option[Swiss],
      events: List[Event],
      relays: List[lila.relay.RelayCard],
      simuls: List[Simul],
      featured: Option[Game],
      puzzle: Option[lila.puzzle.DailyPuzzle.WithHtml],
      streams: LiveStreams.WithTitles,
      playban: Option[TempBan],
      currentGame: Option[Preload.CurrentGame],
      isFeaturable: Simul => Boolean,
      blindGames: List[Pov],
      lastUpdates: List[lila.feed.Feed.Update],
      ublogPosts: List[UblogPost.PreviewPost],
      me: Option[UserWithPerfs],
      hasUnreadLichessMessage: Boolean
  )

  case class CurrentGame(pov: Pov, opponent: String)
