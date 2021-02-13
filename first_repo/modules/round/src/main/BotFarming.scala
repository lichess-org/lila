package lila.round

import lila.common.LightUser.IsBotSync
import lila.game.{ CrosstableApi, Game, GameRepo }

final private class BotFarming(
    gameRepo: GameRepo,
    crosstableApi: CrosstableApi,
    isBotSync: IsBotSync
)(implicit ec: scala.concurrent.ExecutionContext) {

  val SAME_PLIES = 20
  val PREV_GAMES = 2

  /* true if
   * - at least one bot
   * - rated
   * - recent game in same matchup has same first SAME_PLIES and same winner
   */
  def apply(g: Game): Fu[Boolean] =
    g.twoUserIds match {
      case Some((u1, u2)) if g.finished && g.rated && g.userIds.exists(isBotSync) =>
        crosstableApi(u1, u2) flatMap { ct =>
          gameRepo.gamesFromSecondary(ct.results.reverse.take(PREV_GAMES).map(_.gameId)) map {
            _ exists { prev =>
              g.winnerUserId == prev.winnerUserId &&
              g.pgnMoves.take(SAME_PLIES) == prev.pgnMoves.take(SAME_PLIES)
            }
          }
        }
      case _ => fuccess(false)
    }
}
