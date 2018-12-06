package lila.tournament

import akka.actor._
import org.joda.time.DateTime

import lila.game.actorApi.FinishGame

private[tournament] final class ApiActor(
    api: TournamentApi,
    leaderboard: LeaderboardApi,
    socketMap: SocketMap
) extends Actor {

  def receive = {

    case FinishGame(game, _, _) => api finishGame game

    case lila.hub.actorApi.mod.MarkCheater(userId, true) =>
      leaderboard.getAndDeleteRecent(userId, DateTime.now minusDays 3) foreach {
        api.ejectLame(userId, _)
      }

    case lila.hub.actorApi.mod.MarkBooster(userId) => api.ejectLame(userId, Nil)

    case lila.hub.actorApi.round.Berserk(gameId, userId) => api.berserk(gameId, userId)

    case lila.hub.actorApi.playban.Playban(userId, _) => api.pausePlaybanned(userId)

    case m: lila.hub.actorApi.Deploy => socketMap tellAll m
  }
}
