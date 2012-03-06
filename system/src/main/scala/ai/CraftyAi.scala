package lila.system
package ai

import lila.chess.{ Game, Move }
import lila.chess.format.Forsyth
import model._

final class CraftyAi(
  execPath: String = "crafty",
  bookPath: Option[String] = None
) extends Ai {

  def apply(dbGame: DbGame): Valid[(Game, Move)] = {

    val oldGame = dbGame.variant match {
      case Standard => dbGame.toChess
      case Chess960 => dbGame.toChess updateBoard { board =>
        board updateHistory { history =>
          history.withoutAnyCastles
        }
      }
    }
    val oldFen = Forsyth >> oldGame

    failure("Not implemented" wrapNel)
  }
}
