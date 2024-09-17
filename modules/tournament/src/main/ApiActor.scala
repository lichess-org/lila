package lila.tournament

import akka.actor._
import org.joda.time.DateTime

import lila.game.actorApi.FinishGame
import lila.user.User

final private[tournament] class ApiActor(
    api: TournamentApi,
    leaderboard: LeaderboardApi,
    tournamentRepo: TournamentRepo
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
      ejectLameFromEnterable(userId) >>
        leaderboard.getAndDeleteRecent(userId, DateTime.now minusDays 14) foreach {
          _.map { tourId => api.ejectLameFromHistory(tourId, userId) }.sequenceFu
        }

    case lila.hub.actorApi.mod.MarkBooster(userId) =>
      ejectLameFromEnterable(userId).unit

    case lila.hub.actorApi.round.Berserk(gameId, userId) => api.berserk(gameId, userId).unit

    case lila.hub.actorApi.playban.Playban(userId, _) => api.pausePlaybanned(userId).unit

    case lila.hub.actorApi.team.KickFromTeam(teamId, userId) => api.kickFromTeam(teamId, userId).unit
  }

  private def ejectLameFromEnterable(userId: User.ID) =
    tournamentRepo.withdrawableIds(userId) flatMap {
      _.map {
        api.ejectLameFromEnterable(_, userId)
      }.sequenceFu
    }
}
