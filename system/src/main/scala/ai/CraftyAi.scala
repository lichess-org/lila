package lila.system
package ai

import lila.chess.{ Game, Move }
import model.DbGame

final class CraftyAi(
  execPath: String = "crafty",
  bookPath: Option[String] = None
) extends Ai {

  def apply(dbGame: DbGame): Valid[(Game, Move)] = {

    failure("Not implemented" wrapNel)
  }
}
