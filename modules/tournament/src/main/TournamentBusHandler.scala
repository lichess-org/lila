package lila.tournament

import akka.actor.*
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext

import lila.game.actorApi.FinishGame
import lila.user.User

final private class TournamentBusHandler(
    api: TournamentApi,
    leaderboard: LeaderboardApi,
    shieldApi: TournamentShieldApi,
    winnersApi: WinnersApi,
    tournamentRepo: TournamentRepo
)(using ec: ExecutionContext):

  lila.common.Bus.subscribeFun(
    "finishGame",
    "adjustCheater",
    "adjustBooster",
    "playban",
    "teamLeave",
    "berserk"
  ) {

    case FinishGame(game, _, _) => api.finishGame(game).unit

    case lila.playban.SittingDetected(game, player) => api.sittingDetected(game, player).unit

    case lila.hub.actorApi.mod.MarkCheater(userId, true) =>
      ejectFromEnterable(userId) >>
        leaderboard
          .getAndDeleteRecent(userId, DateTime.now minusDays 30)
          .flatMap {
            _.map {
              api.removePlayerAndRewriteHistory(_, userId)
            }.sequenceFu
          } >>
        shieldApi.clearAfterMarking(userId) >>
        winnersApi.clearAfterMarking(userId)
      ()

    case lila.hub.actorApi.mod.MarkBooster(userId) => ejectFromEnterable(userId).unit

    case lila.hub.actorApi.round.Berserk(gameId, userId) => api.berserk(gameId, userId).unit

    case lila.hub.actorApi.playban.Playban(userId, _, true) => api.pausePlaybanned(userId).unit

    case lila.hub.actorApi.team.KickFromTeam(teamId, userId) => api.kickFromTeam(teamId, userId).unit
  }

  private def ejectFromEnterable(userId: User.ID) =
    tournamentRepo.withdrawableIds(userId, reason = "ejectFromEnterable") flatMap {
      _.map {
        api.ejectLameFromEnterable(_, userId)
      }.sequenceFu
    }
