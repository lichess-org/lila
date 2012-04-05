package lila.system
package ai

import lila.chess.{ Game, Move, ReverseEngineering }
import lila.chess.format.Forsyth
import model._

trait FenBased {

  def applyFen(game: Game, fen: String): Valid[(Game, Move)] = for {
    newSituation ← Forsyth << fen toValid "Cannot parse engine FEN: " + fen
    reverseEngineer = new ReverseEngineering(game, newSituation.board)
    poss = reverseEngineer.move.mapFail(msgs ⇒
      ("ReverseEngineering failure: " + (msgs.list mkString "\n") + "\n--------\n" + game.board + "\n" + newSituation.board + "\n" + fen).wrapNel
    ).err
    (orig, dest) = poss
    newGameAndMove ← game(orig, dest)
  } yield newGameAndMove

  def toFen(game: Game, variant: Variant): String = Forsyth >> (variant match {
    case Chess960 ⇒ game updateBoard { board ⇒
      board updateHistory (_.withoutAnyCastles)
    }
    case _ ⇒ game
  })
}
