package lila.tournament

import lila.common.Bus

final private class TournamentBusHandler(
    api: TournamentApi,
    leaderboard: LeaderboardApi,
    shieldApi: TournamentShieldApi,
    winnersApi: WinnersApi,
    tournamentRepo: TournamentRepo
)(using Executor):

  Bus.sub[lila.core.game.FinishGame](finished => api.finishGame(finished.game))
  Bus.sub[lila.core.mod.MarkCheater]:
    case lila.core.mod.MarkCheater(userId, true) =>
      for
        _ <- ejectFromEnterable(userId)
        recent <- leaderboard.getAndDeleteRecent(userId, nowInstant.minusDays(3))
        _ <- recent.parallelVoid(api.removePlayerAndRewriteHistory(_, userId))
        _ <- shieldApi.clearAfterMarking(userId)
        _ <- winnersApi.clearAfterMarking(userId)
      yield ()
  Bus.sub[lila.core.mod.MarkBooster](booster => ejectFromEnterable(booster.userId))

  Bus.sub[lila.core.round.Berserk]:
    case lila.core.round.Berserk(gameId, userId) => api.berserk(gameId, userId)

  Bus.sub[lila.core.playban.Playban]:
    case lila.core.playban.Playban(userId, _, true) => api.pausePlaybanned(userId)

  Bus.sub[lila.core.team.KickFromTeam]:
    case lila.core.team.KickFromTeam(teamId, _, userId) => api.kickFromTeam(teamId, userId)

  Bus.sub[lila.core.playban.SittingDetected]:
    case lila.core.playban.SittingDetected(tourId, userId) =>
      api.withdraw(tourId, userId, isPause = false, isStalling = true)

  private def ejectFromEnterable(userId: UserId) =
    tournamentRepo
      .withdrawableIds(userId, reason = "ejectFromEnterable")
      .flatMap:
        _.sequentiallyVoid:
          api.ejectLameFromEnterable(_, userId)
