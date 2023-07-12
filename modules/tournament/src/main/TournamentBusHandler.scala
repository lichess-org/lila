package lila.tournament

import lila.game.actorApi.FinishGame

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
  ):

    case FinishGame(game, _) => api.finishGame(game)

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

    case lila.hub.actorApi.mod.MarkBooster(userId)           => ejectFromEnterable(userId)
    case lila.hub.actorApi.round.Berserk(gameId, userId)     => api.berserk(gameId, userId)
    case lila.hub.actorApi.playban.Playban(userId, _, true)  => api.pausePlaybanned(userId)
    case lila.hub.actorApi.team.KickFromTeam(teamId, userId) => api.kickFromTeam(teamId, userId)
    case lila.playban.SittingDetected(game, player)          => api.sittingDetected(game, player)

  private def ejectFromEnterable(userId: UserId) =
    tournamentRepo.withdrawableIds(userId, reason = "ejectFromEnterable") flatMap {
      _.traverse_ {
        api.ejectLameFromEnterable(_, userId)
      }
    }
