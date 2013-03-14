package lila.app
package ai

import chess.{ Game, Move, ReverseEngineering, Variant }
import chess.format.Forsyth

trait FenBased {

  def applyFen(game: Game, fen: String): Valid[(Game, Move)] = for {
    newSituation ← Forsyth << fen toValid "Cannot parse engine FEN: " + fen
    reverseEngineer = new ReverseEngineering(game, newSituation.board)
    poss = reverseEngineer.move.mapFail(msgs ⇒
      ("ReverseEngineering failure: " + msgs.shows + "\n--------\n" + game.board + "\n" + newSituation.board + "\n" + fen).wrapNel
    ).err
    (orig, dest) = poss
    newGameAndMove ← game(orig, dest)
  } yield newGameAndMove

  def toFen(game: Game, variant: Variant): String = Forsyth >> (variant match {
    case Variant.Chess960 ⇒ game updateBoard { board ⇒
      board updateHistory (_.withoutAnyCastles)
    }
    case _ ⇒ game
  })
}
