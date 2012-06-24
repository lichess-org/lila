package lila
package ai
package crafty

import chess.{ Game, Move }
import game.DbGame

import java.io.File
import scala.io.Source
import scala.sys.process.Process
import scalaz.effects._

final class Ai(server: Server) extends ai.Ai with FenBased {

  def play(dbGame: DbGame, initialFen: Option[String]): IO[Valid[(Game, Move)]] = {

    val oldGame = dbGame.toChess
    val oldFen = toFen(oldGame, dbGame.variant)

    server.play(oldFen, dbGame.aiLevel | 1).fold(
      err ⇒ io(failure(err)),
      iop ⇒ iop map { newFen ⇒ applyFen(oldGame, newFen) }
    )
  }

  def analyse(dbGame: DbGame, initialFen: Option[String]) = io {
    !!("Crafty analysis is not implemented")
  }
}
