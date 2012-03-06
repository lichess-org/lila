package lila.system
package ai

import lila.chess.{ Game, Move, ReverseEngineering }
import lila.chess.format.Forsyth
import model._

import java.io.File
import scalaz.effects._

final class CraftyAi(
    execPath: String = "crafty",
    bookPath: Option[String] = None) extends Ai {

  def apply(dbGame: DbGame): IO[Valid[(Game, Move)]] = {

    val oldGame = dbGame.variant match {
      case Standard ⇒ dbGame.toChess
      case Chess960 ⇒ dbGame.toChess updateBoard { board ⇒
        board updateHistory (_.withoutAnyCastles)
      }
    }

    getNewFen(Forsyth >> oldGame) map { newFen =>
      for {
        newSituation ← Forsyth << newFen toValid "Cannot parse engine FEN"
        reverseEngineer = new ReverseEngineering(oldGame, newSituation.board)
        poss ← reverseEngineer.move toValid "Cannot reverse engineer engine move"
        (orig, dest) = poss
        newGameAndMove <- oldGame(orig, dest)
      } yield newGameAndMove
    }
  }

  def getNewFen(oldFen: String): IO[String] = for {
    file <- tempFile("lichess_crafty_")
  } yield ""

  def tempFile(prefix: String, suffix: String = ".tmp"): IO[File] = io {
    File.createTempFile(prefix, suffix) ~ { file =>
      file.deleteOnExit
    }
  }
}
