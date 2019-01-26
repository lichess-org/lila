package lila.round

import lila.common.LightUser.IsBotSync
import lila.game.{ Game, GameRepo, CrosstableApi }

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
  def apply(g: Game): Fu[Boolean] = g.userIds.distinct match {
    case List(u1, u2) if g.finished && g.rated && g.userIds.exists(isBotSync) =>
      crosstableApi(u1, u2) flatMap {
        _ ?? { ct =>
          GameRepo.gamesFromSecondary(ct.results.reverse.take(PREV_GAMES).map(_.gameId)) map {
            _ exists { prev =>
              g.winnerUserId == prev.winnerUserId &&
                g.pgnMoves.take(SAME_PLIES) == prev.pgnMoves.take(SAME_PLIES)
            }
          }
        }
      }
    case _ => fuccess(false)
  }
}
