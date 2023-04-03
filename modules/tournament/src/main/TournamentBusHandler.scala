package lila.tournament

import akka.actor.*

import lila.game.actorApi.FinishGame
import lila.user.User

final private class TournamentBusHandler(
    api: TournamentApi,
    leaderboard: LeaderboardApi,
    shieldApi: TournamentShieldApi,
    winnersApi: WinnersApi,
    tournamentRepo: TournamentRepo
)(using Executor):

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
          .getAndDeleteRecent(userId, nowInstant minusDays 30)
          .flatMap {
            _.map {
              api.removePlayerAndRewriteHistory(_, userId)
            }.parallel
          } >>
        shieldApi.clearAfterMarking(userId) >>
        winnersApi.clearAfterMarking(userId)
      ()

    case lila.hub.actorApi.mod.MarkBooster(userId) => ejectFromEnterable(userId).unit

    case lila.hub.actorApi.round.Berserk(gameId, userId) => api.berserk(gameId, userId).unit

    case lila.hub.actorApi.playban.Playban(userId, _, true) => api.pausePlaybanned(userId).unit

    case lila.hub.actorApi.team.KickFromTeam(teamId, userId) => api.kickFromTeam(teamId, userId).unit
  }

  private def ejectFromEnterable(userId: UserId) =
    tournamentRepo.withdrawableIds(userId, reason = "ejectFromEnterable") flatMap {
      _.map {
        api.ejectLameFromEnterable(_, userId)
      }.parallel
    }
