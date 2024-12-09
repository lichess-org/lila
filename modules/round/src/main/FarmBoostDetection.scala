package lila.round

import chess.ByColor

import lila.core.LightUser.IsBotSync
import lila.core.perf.UserWithPerfs
import lila.game.{ CrosstableApi, GameRepo }

final private class FarmBoostDetection(
    gameRepo: GameRepo,
    crosstableApi: CrosstableApi,
    isBotSync: IsBotSync
)(using Executor):

  val SAME_PLIES = 20
  val PREV_GAMES = 2

  /* true if
   * - at least one bot
   * - rated
   * - recent game in same matchup has same first SAME_PLIES and same winner
   */
  def botFarming(g: Game): Fu[Boolean] =
    g.twoUserIds match
      case Some((u1, u2)) if g.finished && g.rated && g.userIds.exists(isBotSync) =>
        crosstableApi(u1, u2).flatMap: ct =>
          gameRepo
            .gamesFromSecondary(ct.results.reverse.take(PREV_GAMES).map(_.gameId))
            .map:
              _.exists: prev =>
                g.winnerUserId === prev.winnerUserId &&
                  g.sans.take(SAME_PLIES) === prev.sans.take(SAME_PLIES)
            .addEffect:
              if _ then lila.mon.round.farming.bot.increment()
      case _ => fuccess(false)

  def newAccountBoosting(g: Game, users: ByColor[UserWithPerfs]): Boolean =
    val found = g.sourceIs(_.Friend) &&
      g.playedTurns < 20 &&
      g.durationSeconds.exists(_ < 60) &&
      users.exists(_.perfs(g.perfKey).provisional.yes)
    if found then lila.mon.round.farming.provisional.increment()
    found
