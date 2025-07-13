package lila.round

import scalalib.future.FutureAfter
import chess.ByColor

import lila.core.LightUser.IsBotSync
import lila.core.perf.UserWithPerfs
import lila.game.{ CrosstableApi, GameRepo }
import lila.core.game.reasonableMinimumNumberOfMoves

final private class FarmBoostDetection(
    gameRepo: GameRepo,
    crosstableApi: CrosstableApi,
    isBotSync: IsBotSync
)(using Executor, FutureAfter):

  val SAME_PLIES = 20
  val PREV_GAMES = 2

  /* true if
   * - at least one bot
   * - rated
   * - recent game in same matchup has same first SAME_PLIES and same winner
   */
  def botFarming(g: Game): Fu[Boolean] =
    (g.finished && g.rated.yes && g.userIds.exists(isBotSync))
      .so(g.twoUserIds)
      .so: (u1, u2) =>
        crosstableApi(u1, u2).flatMap: ct =>
          gameRepo
            .gamesFromSecondary(ct.results.reverse.take(PREV_GAMES).map(_.gameId))
            .map:
              _.exists: prev =>
                g.winnerUserId === prev.winnerUserId &&
                  g.sans.take(SAME_PLIES) === prev.sans.take(SAME_PLIES)
            .addEffect:
              if _ then lila.mon.round.farming.bot.increment()

  def newAccountBoosting(g: Game, users: ByColor[UserWithPerfs]): Fu[Boolean] =
    g.winnerColor
      .map(users(_))
      .filterNot(_.user.createdSinceDays(7))
      .so: winner =>
        val perf              = winner.perfs(g.perfKey)
        val minSeconds        = linearInterpolation(perf.nb)(0 -> 90, 5 -> 60)
        def minPliesForPerfNb =
          if g.variant.standard
          then linearInterpolation(perf.nb)(0 -> 40, 5 -> 20)
          else reasonableMinimumNumberOfMoves(g.variant)
        def minPliesForLoserRating =
          g.variant.standard
            .so(g.loser.flatMap(_.rating))
            .map: rating =>
              linearInterpolation(rating.value)(1500 -> 10, 2500 -> 40)
        (
          perf.provisional.yes &&
            (g.playedPlies < minPliesForPerfNb || minPliesForLoserRating.exists(g.playedPlies < _)) &&
            g.durationSeconds.exists(_ < minSeconds)
        ) || g.playedPlies < 10
      .so(arePlayersRelated(g))
      .addEffect:
        if _ then
          lila.mon.round.farming.provisional.increment()
          logger.info(s"new account boosting: https://lichess.org/${g.id} ${users.map(_.user.username)}")

  private def linearInterpolation(x: Int)(p1: PairOf[Int], p2: PairOf[Int]): Int =
    val ((x1, y1), (x2, y2)) = (p1, p2)
    y1 + (x - x1) * (y2 - y1) / (x2 - x1)

  private def arePlayersRelated(game: Game): Fu[Boolean] =
    if game.sourceIs(_.Friend) then fuTrue
    else
      game.userIdPair.sequence
        .map(_.toPair)
        .so: userIds =>
          lila.common.Bus.safeAsk(lila.core.security.AskAreRelated(userIds, _))
