package lila.system

import model._
import lila.chess._

import format.Visual
import Pos._

class ModelToChessTest extends SystemTest {

  "model to chess conversion" should {
    //"new game" in {
    //newDbGame.toChess must_== Game()
    //}
    "played game" in {
      val game = dbGame1.toChess
      "player" in {
        game.player must_== White
      }
      "pgn moves" in {
        game.pgnMoves must_== "e4 Nc6 Nf3 Nf6 e5 Ne4 d3 Nc5 Be3 d6 d4 Ne4 Bd3 Bf5 Nc3 Nxc3 bxc3 Qd7 Bxf5 Qxf5 Nh4 Qe4 g3 Qxh1+"
      }
      "pieces" in {
        Visual addNewLines game.board.toString must_== """
r   kb r
ppp pppp
  np
    P
   P   N
  P B P
P P  P P
R  QK  q
"""
      }
      "last move" in {
        game.board.history.lastMove must beNone
      }
      "clock" in {
        game.clock must beNone
      }
    }
    "and another played game" in {
      val game = dbGame2.toChess
      "player" in {
        game.player must_== Black
      }
      "pgn moves" in {
        game.pgnMoves must_== "e4 e5 Qh5 Qf6 Nf3 g6 Qxe5+ Qxe5 Nxe5 Nf6 Nc3 Nc6 Nxc6 bxc6 e5 Nd5 Nxd5 cxd5 d4 Rb8 c3 d6 Be2 dxe5 dxe5 Rg8 Bf3 d4 cxd4 Bb4+ Ke2 g5 a3 g4 Bc6+ Bd7 Bxd7+ Kxd7 axb4 Rxb4 Kd3 Rb3+ Kc4 Rb6 Rxa7 Rc6+ Kb5 Rb8+ Ka5 Rc4 Rd1 Kc6 d5+ Kc5 Rxc7#"
      }
      "pieces" in {
        Visual addNewLines game.board.toString must_== """
 r
  R  p p

K kPP
  r   p

 P   PPP
  BR
"""
      }
      "last move" in {
        game.board.history.lastMove must_== (A7, C7).some
      }
      "clock" in {
        game.clock must_== Some(Clock(
          color = Black,
          increment = 5,
          limit = 1200,
          times = Map(
            White -> 196.25f,
            Black -> 304.1f
          )
        ))
      }
    }
    "a chess960 played game" in {
      val game = dbGame3.toChess
      "player" in {
        game.player must_== Black
      }
      "pieces" in {
        Visual addNewLines game.board.toString must_== """
R  k
       R


 B   pp
 P P P
  K

"""
      }
      "last move" in {
        game.board.history.lastMove must_== (A3, A8).some
      }
    }
  }
}
