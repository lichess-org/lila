package shogi

import scalaz.Validation.FlatMap._
import Pos._
import variant.Standard

class AutodrawTest extends ShogiTest {

  "detect automatic draw" should {
    "by lack of pieces" in {
      "empty" in {
        makeEmptyBoard.autoDraw must_== true
      }
      "new" in {
        makeBoard.autoDraw must_== false
      }
      "opened" in {
        makeGame.playMoves(E3 -> E4, E7 -> E6, C3 -> C4, E6 -> E5, E4 -> E5) map { g =>
          g.board.autoDraw
        } must beSuccess(false)
      }
      "two kings with nothing in hand" in {
        """
      k
K      """.autoDraw must_== true
      }
      "one pawn" in {
        """
  P   k
K      """.autoDraw must_== false
      }
      "one bishop" in {
        """
      k
K     B""".autoDraw must_== false
      }
      "one knight" in {
        """
      k
K     N""".autoDraw must_== false
      }
    }
    "by fourfold" in {
      val moves = List(
        H2 -> G2,
        B8 -> C8,
        G2 -> H2,
        C8 -> B8,
        H2 -> G2,
        B8 -> C8,
        G2 -> H2,
        C8 -> B8,
        H2 -> G2,
        B8 -> C8,
        G2 -> H2,
        C8 -> B8,
        H2 -> G2
      )
      "should be fourfold" in {
        makeGame.playMoves(moves: _*) must beSuccess.like { case g =>
          g.situation.autoDraw must beTrue
        }
      }
      "should not be fourfold" in {
        makeGame.playMoves(moves.dropRight(1): _*) must beSuccess.like { case g =>
          g.situation.autoDraw must beFalse
        }
      }
    }
  }
  "do not detect insufficient material" should {
    "on two kings with something in hand" in {
      val position = "4k4/9/9/9/9/9/9/9/5K3 b p 1"
      fenToGame(position, Standard) must beSuccess.like { case game =>
        game.situation.autoDraw must beFalse
      //game.situation.end must beFalse
      //game.situation.opponentHasInsufficientMaterial must beFalse
      }
    }
    "on a single pawn" in {
      val position = "2p2k3/9/9/9/9/9/9/9/4K4 b - 1"
      val game     = fenToGame(position, Standard)
      val newGame = game flatMap (_.playMove(
        Pos.E1,
        Pos.E2
      ))
      newGame must beSuccess.like { case game =>
        game.situation.autoDraw must beFalse
        game.situation.end must beFalse
        game.situation.opponentHasInsufficientMaterial must beTrue
      }
    }
  }
}
