package lidraughts.tournament

import akka.actor._
import org.joda.time.DateTime

import lidraughts.game.actorApi.FinishGame

private[tournament] final class ApiActor(
    api: TournamentApi,
    leaderboard: LeaderboardApi,
    socketMap: SocketMap
) extends Actor {

  def receive = {

    case FinishGame(game, _, _) => api finishGame game

    case lidraughts.playban.SittingDetected(game, player) => api.sittingDetected(game, player)

    case lidraughts.hub.actorApi.mod.MarkCheater(userId, true) =>
      leaderboard.getAndDeleteRecent(userId, DateTime.now minusDays 3) foreach {
        api.ejectLame(userId, _)
      }

    case lidraughts.hub.actorApi.mod.MarkBooster(userId) => api.ejectLame(userId, Nil)

    case lidraughts.hub.actorApi.round.Berserk(gameId, userId) => api.berserk(gameId, userId)

    case lidraughts.hub.actorApi.playban.Playban(userId, _) => api.pausePlaybanned(userId)

    case m: lidraughts.hub.actorApi.Deploy => socketMap tellAll m
  }
}
