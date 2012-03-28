package lila.chess

import scala.math.round

final class EloCalculator {

  // Player 1 wins
  val P1WIN = -1;

  // No player wins
  val DRAW = 0;

  // Player 2 wins
  val P2WIN = 1;

  type User = {
    def elo: Int
    def nbRatedGames: Int
  }

  def calculate(user1: User, user2: User, win: Option[Color]): (Int, Int) = {
    val winCode = win match {
      case None        ⇒ DRAW
      case Some(White) ⇒ P1WIN
      case Some(Black) ⇒ P2WIN
    }
    (calculateUserElo(user1, user2.elo, -winCode),
      calculateUserElo(user2, user1.elo, winCode))
  }

  private def calculateUserElo(user: User, opponentElo: Int, win: Int) = {
    val score = (1 + win) / 2f
    val expected = 1 / (1 + math.pow(10, (opponentElo - user.elo) / 400f))
    val kFactor = math.round(
      if (user.nbRatedGames > 20) 16
      else 50 - user.nbRatedGames * (34 / 20f)
    )
    val diff = 2 * kFactor * (score - expected)

    round(user.elo + diff).toInt
  }
}
