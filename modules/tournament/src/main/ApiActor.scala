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

    case FinishGame(game, _, _) => api.finishGame(game).unit

    case lila.playban.SittingDetected(game, player) => api.sittingDetected(game, player).unit

    case lila.hub.actorApi.mod.MarkCheater(userId, true) =>
      leaderboard.getAndDeleteRecent(userId, DateTime.now minusDays 7) foreach {
        api.ejectPlayer(userId, _, lila.user.User.lishogiId)
      }

    case lila.hub.actorApi.mod.MarkBooster(userId) =>
      api.ejectPlayer(userId, Nil, lila.user.User.lishogiId).unit

    case lila.hub.actorApi.round.Berserk(gameId, userId) => api.berserk(gameId, userId).unit

    case lila.hub.actorApi.playban.Playban(userId, _) => api.pausePlaybanned(userId).unit

    case lila.hub.actorApi.team.KickFromTeam(teamId, userId) => api.kickFromTeam(teamId, userId).unit
  }
}
