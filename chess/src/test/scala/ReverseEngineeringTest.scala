package lila.chess

import Pos._
import format.Visual.addNewLines
import scalaz.Success

class ReverseEngineeringTest extends ChessTest {

  def findMove(g1: Game, g2: Game) = (new ReverseEngineering(g1, g2.board)).move
  def play(game: Game, moves: (Pos, Pos)*): Game =
    game.playMoveList(moves).fold(e ⇒ sys.error(e.toString), identity)
  val playedGame = play(Game(),
    E2 -> E4, E7 -> E5, F1 -> C4, G8 -> F6, D2 -> D3, C7 -> C6, C1 -> G5, H7 -> H6, G1 -> H3, A7 -> A5)
  /*
rnbqkb r
 p p pp
  p  n p
p   p B
  B P
   P   N
PPP  PPP
RN QK  R
*/

  "reverse engineer a move" should {
    "none on same games" in {
      "initial game" in {
        findMove(Game(), Game()) must beFailure
      }
      "played game" in {
        findMove(playedGame, playedGame) must beFailure
      }
    }
    "none on different games" in {
      "initial to played" in {
        findMove(Game(), playedGame) must beFailure
      }
      "played to initial" in {
        findMove(playedGame, Game()) must beFailure
      }
    }
    "find one move" in {
      "initial game pawn moves one square" in {
        findMove(Game(), play(Game(), D2 -> D3)) must_== Success(D2 -> D3)
      }
      "initial game pawn moves two squares" in {
        findMove(Game(), play(Game(), D2 -> D4)) must_== Success(D2 -> D4)
      }
      "initial game bishop moves" in {
        findMove(Game(), play(Game(), B1 -> C3)) must_== Success(B1 -> C3)
      }
      "played game king moves right" in {
        findMove(playedGame, play(playedGame, E1 -> F1)) must_== Success(E1 -> F1)
      }
      "played game bishop eats knight" in {
        findMove(playedGame, play(playedGame, G5 -> F6)) must_== Success(G5 -> F6)
      }
      "played game king castles kingside" in {
        findMove(playedGame, play(playedGame, E1 -> G1)) must_== Success(E1 -> G1)
      }
      "promotion" in {
        val game = Game("""
  p  k
K      """, Black)
        "to queen" in {
          val newGame = game.playMove(C2, C1, Queen).fold(e ⇒ sys.error(e.toString), identity)
          findMove(game, newGame) must_== Success(C2 -> C1)
        }
        "to knight" in {
          val newGame = game.playMove(C2, C1, Knight).fold(e ⇒ sys.error(e.toString), identity)
          findMove(game, newGame) must_== Success(C2 -> C1)
        }
        "not" in {
          val newGame = game.playMove(F2, E2).fold(e ⇒ sys.error(e.toString), identity)
          findMove(game, newGame) must_== Success(F2 -> E2)
        }
      }
    }
  }
}
