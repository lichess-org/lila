package lidraughts.round

import lidraughts.common.LightUser.IsBotSync
import lidraughts.game.{ Game, GameRepo, CrosstableApi }

private final class BotFarming(
    crosstableApi: CrosstableApi,
    isBotSync: IsBotSync
) {

  val SAME_PLIES = 20
  val PREV_GAMES = 2

  /* true if
   * - at least one bot
   * - rated
   * - recent game in same matchup has same first SAME_PLIES and same winner
   */
  def apply(g: Game): Fu[Boolean] = g.twoUserIds match {
    case Some((u1, u2)) if g.finished && g.rated && g.userIds.exists(isBotSync) =>
      crosstableApi(u1, u2) flatMap { ct =>
        GameRepo.gamesFromSecondary(ct.results.reverse.take(PREV_GAMES).map(_.gameId)) map {
          _ exists { prev =>
            g.winnerUserId == prev.winnerUserId &&
              g.pdnMoves.take(SAME_PLIES) == prev.pdnMoves.take(SAME_PLIES)
          }
        }
      }
    case _ => fuccess(false)
  }
}
