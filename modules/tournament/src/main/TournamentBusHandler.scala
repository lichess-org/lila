package lila.tournament

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
    "team",
    "berserk"
  ):

    case lila.core.game.FinishGame(game, _) => api.finishGame(game)

    case lila.core.mod.MarkCheater(userId, true) =>
      for
        _      <- ejectFromEnterable(userId)
        recent <- leaderboard.getAndDeleteRecent(userId, nowInstant.minusDays(3))
        _      <- recent.parallelVoid(api.removePlayerAndRewriteHistory(_, userId))
        _      <- shieldApi.clearAfterMarking(userId)
        _      <- winnersApi.clearAfterMarking(userId)
      yield ()

    case lila.core.mod.MarkBooster(userId)              => ejectFromEnterable(userId)
    case lila.core.round.Berserk(gameId, userId)        => api.berserk(gameId, userId)
    case lila.core.playban.Playban(userId, _, true)     => api.pausePlaybanned(userId)
    case lila.core.team.KickFromTeam(teamId, _, userId) => api.kickFromTeam(teamId, userId)
    case lila.core.playban.SittingDetected(tourId, userId) =>
      api.withdraw(tourId, userId, isPause = false, isStalling = true)

  private def ejectFromEnterable(userId: UserId) =
    tournamentRepo
      .withdrawableIds(userId, reason = "ejectFromEnterable")
      .flatMap:
        _.sequentiallyVoid:
          api.ejectLameFromEnterable(_, userId)
