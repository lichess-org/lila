package lila.app
package mashup

import com.github.blemale.scaffeine.AsyncLoadingCache
import play.api.libs.json.*

import lila.api.Context
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
import lila.user.User

final class Preload(
    tv: lila.tv.Tv,
    gameRepo: lila.game.GameRepo,
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
    relayApi: lila.relay.RelayApi
)(using Executor):

  import Preload.*

  def apply(
      tours: Fu[List[Tournament]],
      swiss: Option[Swiss],
      events: Fu[List[Event]],
      simuls: Fu[List[Simul]],
      streamerSpots: Int
  )(using ctx: Context): Fu[Homepage] =
    lobbyApi.apply.mon(_.lobby segment "lobbyApi") zip
      tours.mon(_.lobby segment "tours") zip
      events.mon(_.lobby segment "events") zip
      simuls.mon(_.lobby segment "simuls") zip
      tv.getBestGame.mon(_.lobby segment "tvBestGame") zip
      (ctx.userId ?? timelineApi.userEntries).mon(_.lobby segment "timeline") zip
      userCached.topWeek.mon(_.lobby segment "userTopWeek") zip
      tourWinners.all.dmap(_.top).mon(_.lobby segment "tourWinners") zip
      (ctx.noBot ?? dailyPuzzle()).mon(_.lobby segment "puzzle") zip
      (ctx.noKid ?? liveStreamApi.all
        .dmap(_.homepage(streamerSpots, ctx.req, ctx.me.flatMap(_.lang)) withTitles lightUserApi)
        .mon(_.lobby segment "streams")) zip
      (ctx.userId ?? playbanApi.currentBan).mon(_.lobby segment "playban") zip
      (ctx.blind ?? ctx.me ?? roundProxy.urgentGames) zip
      lastPostsCache.get {} zip
      ctx.userId
        .ifTrue(ctx.nbNotifications > 0)
        .filterNot(liveStreamApi.isStreaming)
        .??(msgApi.hasUnreadLichessMessage) flatMap {
        // format: off
        case ((((((((((((((data, povs), tours), events), simuls), feat), entries), lead), tWinners), puzzle), streams), playban), blindGames), ublogPosts), lichessMsg) =>
        // format: on
          (ctx.me ?? currentGameMyTurn(povs, lightUserApi.sync))
            .mon(_.lobby segment "currentGame") zip
            lightUserApi
              .preloadMany(tWinners.map(_.userId) ::: entries.flatMap(_.userIds).toList)
              .mon(_.lobby segment "lightUsers") map { case (currentGame, _) =>
              Homepage(
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
                hasUnreadLichessMessage = lichessMsg
              )
            }
      }

  def currentGameMyTurn(user: User): Fu[Option[CurrentGame]] =
    gameRepo.playingRealtimeNoAi(user).flatMap {
      _.map { roundProxy.pov(_, user) }.parallel.dmap(_.flatten)
    } flatMap {
      currentGameMyTurn(_, lightUserApi.sync)(user)
    }

  private def currentGameMyTurn(povs: List[Pov], lightUser: lila.common.LightUser.GetterSync)(
      user: User
  ): Fu[Option[CurrentGame]] =
    ~povs.collectFirst {
      case p1 if p1.game.nonAi && p1.game.hasClock && p1.isMyTurn =>
        roundProxy.pov(p1.gameId, user) dmap (_ | p1) map { pov =>
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
      relays: List[lila.relay.RelayTour.ActiveWithNextRound],
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
      hasUnreadLichessMessage: Boolean
  )

  case class CurrentGame(pov: Pov, opponent: String)
