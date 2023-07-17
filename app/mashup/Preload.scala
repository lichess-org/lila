package lila.app
package mashup

import com.github.blemale.scaffeine.AsyncLoadingCache
import play.api.libs.json.*

import lila.event.Event
import lila.game.{ Game, Pov }
import lila.playban.TempBan
import lila.simul.{ Simul, SimulIsFeaturable }
import lila.streamer.LiveStreams
import lila.swiss.Swiss
import lila.timeline.Entry
import lila.tournament.{ Tournament, Winner }
import lila.ublog.UblogPost
import lila.user.LightUserApi
import lila.user.{ User, Me }

final class Preload(
    tv: lila.tv.Tv,
    gameRepo: lila.game.GameRepo,
    perfsRepo: lila.user.UserPerfsRepo,
    userCached: lila.user.Cached,
    tourWinners: lila.tournament.WinnersApi,
    timelineApi: lila.timeline.EntryApi,
    liveStreamApi: lila.streamer.LiveStreamApi,
    dailyPuzzle: lila.puzzle.DailyPuzzle.Try,
    lobbyApi: lila.api.LobbyApi,
    playbanApi: lila.playban.PlaybanApi,
    lightUserApi: LightUserApi,
    roundProxy: lila.round.GameProxyRepo,
    simulIsFeaturable: SimulIsFeaturable,
    lastPostCache: lila.blog.LastPostCache,
    lastPostsCache: AsyncLoadingCache[Unit, List[UblogPost.PreviewPost]],
    msgApi: lila.msg.MsgApi,
    relayApi: lila.relay.RelayApi,
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
    withPerfs       <- ctx.user.soFu(perfsRepo.withPerfs)
    given Option[User.WithPerfs] = withPerfs
    (
      (
        (
          (
            (
              (((((((((data, povs), tours), events), simuls), feat), entries), lead), tWinners), puzzle),
              streams
            ),
            playban
          ),
          blindGames
        ),
        ublogPosts
      ),
      lichessMsg
    ) <- lobbyApi.apply.mon(_.lobby segment "lobbyApi") zip
      tours.mon(_.lobby segment "tours") zip
      events.mon(_.lobby segment "events") zip
      simuls.mon(_.lobby segment "simuls") zip
      tv.getBestGame.mon(_.lobby segment "tvBestGame") zip
      (ctx.userId so timelineApi.userEntries).mon(_.lobby segment "timeline") zip
      userCached.topWeek.mon(_.lobby segment "userTopWeek") zip
      tourWinners.all.dmap(_.top).mon(_.lobby segment "tourWinners") zip
      (ctx.noBot so dailyPuzzle()).mon(_.lobby segment "puzzle") zip
      (ctx.noKid so liveStreamApi.all
        .dmap(_.homepage(streamerSpots, ctx.req, ctx.me.flatMap(_.lang)) withTitles lightUserApi)
        .mon(_.lobby segment "streams")) zip
      (ctx.userId so playbanApi.currentBan).mon(_.lobby segment "playban") zip
      (ctx.blind so ctx.me so roundProxy.urgentGames) zip
      lastPostsCache.get {} zip
      ctx.userId
        .ifTrue(nbNotifications > 0)
        .filterNot(liveStreamApi.isStreaming)
        .so(msgApi.hasUnreadLichessMessage)
    (currentGame, _) <- (ctx.me soUse currentGameMyTurn(povs, lightUserApi.sync))
      .mon(_.lobby segment "currentGame") zip
      lightUserApi
        .preloadMany(tWinners.map(_.userId) ::: entries.flatMap(_.userIds).toList)
        .mon(_.lobby segment "lightUsers")
  yield Homepage(
    data,
    entries,
    tours,
    swiss,
    events,
    relayApi.spotlight,
    simuls,
    feat,
    lead,
    tWinners,
    puzzle,
    streams.excludeUsers(events.flatMap(_.hostedBy)),
    playban,
    currentGame,
    simulIsFeaturable,
    blindGames,
    lastPostCache.apply,
    ublogPosts,
    withPerfs,
    hasUnreadLichessMessage = lichessMsg
  )

  def currentGameMyTurn(using me: Me): Fu[Option[CurrentGame]] =
    gameRepo.playingRealtimeNoAi(me).flatMap {
      _.map { roundProxy.pov(_, me) }.parallel.dmap(_.flatten)
    } flatMap {
      currentGameMyTurn(_, lightUserApi.sync)
    }

  private def currentGameMyTurn(povs: List[Pov], lightUser: lila.common.LightUser.GetterSync)(using
      me: Me
  ): Fu[Option[CurrentGame]] =
    ~povs.collectFirst {
      case p1 if p1.game.nonAi && p1.game.hasClock && p1.isMyTurn =>
        roundProxy.pov(p1.gameId, me) dmap (_ | p1) map { pov =>
          val opponent = lila.game.Namer.playerTextBlocking(pov.opponent)(using lightUser)
          CurrentGame(pov = pov, opponent = opponent).some
        }
    }

object Preload:

  case class Homepage(
      data: JsObject,
      userTimeline: Vector[Entry],
      tours: List[Tournament],
      swiss: Option[Swiss],
      events: List[Event],
      relays: List[lila.relay.RelayTour.ActiveWithSomeRounds],
      simuls: List[Simul],
      featured: Option[Game],
      leaderboard: List[User.LightPerf],
      tournamentWinners: List[Winner],
      puzzle: Option[lila.puzzle.DailyPuzzle.WithHtml],
      streams: LiveStreams.WithTitles,
      playban: Option[TempBan],
      currentGame: Option[Preload.CurrentGame],
      isFeaturable: Simul => Boolean,
      blindGames: List[Pov],
      lastPost: Option[lila.blog.MiniPost],
      ublogPosts: List[UblogPost.PreviewPost],
      me: Option[User.WithPerfs],
      hasUnreadLichessMessage: Boolean
  )

  case class CurrentGame(pov: Pov, opponent: String)
