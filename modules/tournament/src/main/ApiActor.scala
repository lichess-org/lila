package lila.tournament

import akka.actor._
import org.joda.time.DateTime

import lila.game.actorApi.FinishGame

final private[tournament] class ApiActor(
    api: TournamentApi,
    leaderboard: LeaderboardApi
) extends Actor {

  implicit def ec = context.dispatcher

  lila.common.Bus.subscribe(
    self,
    "finishGame",
    "adjustCheater",
    "adjustBooster",
    "playban",
    "teamKick"
  )

  def receive = {

    case FinishGame(game, _, _) => api finishGame game

    case lila.playban.SittingDetected(game, player) => api.sittingDetected(game, player)

    case lila.hub.actorApi.mod.MarkCheater(userId, true) =>
      leaderboard.getAndDeleteRecent(userId, DateTime.now minusDays 3) flatMap {
        api.ejectLame(userId, _)
      }

    case lila.hub.actorApi.mod.MarkBooster(userId) => api.ejectLame(userId, Nil)

    case lila.hub.actorApi.round.Berserk(gameId, userId) => api.berserk(gameId, userId)

    case lila.hub.actorApi.playban.Playban(userId, _) => api.pausePlaybanned(userId)

    case lila.hub.actorApi.team.KickFromTeam(teamId, userId) => api.kickFromTeam(teamId, userId)
  }
}
