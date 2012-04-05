package lila
package ai

import lila.chess.{ Game, Move }
import model._

import java.io.File
import scala.io.Source
import scala.sys.process.Process
import scalaz.effects._

final class CraftyAi(server: CraftyServer) extends Ai with FenBased {

  def apply(dbGame: DbGame): IO[Valid[(Game, Move)]] = {

    val oldGame = dbGame.toChess
    val oldFen = toFen(oldGame, dbGame.variant)

    server(oldFen, dbGame.aiLevel | 1).fold(
      err ⇒ io(failure(err)),
      iop ⇒ iop map { newFen ⇒ applyFen(oldGame, newFen) }
    )
  }
}
