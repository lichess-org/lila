package lila
package ai

import chess.{ Game, Move }
import game.DbGame

import java.io.File
import scala.io.Source
import scala.sys.process.Process
import scalaz.effects._

final class CraftyAi(server: CraftyServer) extends Ai with FenBased {

  def apply(dbGame: DbGame, initialFen: Option[String]): IO[Valid[(Game, Move)]] = {

    val oldGame = dbGame.toChess
    val oldFen = toFen(oldGame, dbGame.variant)

    server(oldFen, dbGame.aiLevel | 1).fold(
      err ⇒ io(failure(err)),
      iop ⇒ iop map { newFen ⇒ applyFen(oldGame, newFen) }
    )
  }
}
