package lila.system

import model._
import lila.chess._

import format.Visual

class ModelToChessTest extends SystemTest {

  "model to chess conversion" should {
    //"new game" in {
      //newDbGame.toChess must_== Game()
    //}
    "played game" in {
      val game = DbGame(
        id = "huhuhaha",
        players = List(
          newPlayer("white", "ip ar sp16 sN14 kp ub8 Bp6 dq Kp0 ek np LB12 wp22 Fn2 pp hR"),
          newPlayer("black", "Wp 4r Xp Qn1 Yp LB13 Rp9 hq17 0p 8k 1p 9b 2p sN3 3p ?r")),
        pgn = "e4 Nc6 Nf3 Nf6 e5 Ne4 d3 Nc5 Be3 d6 d4 Ne4 Bd3 Bf5 Nc3 Nxc3 bxc3 Qd7 Bxf5 Qxf5 Nh4 Qe4 g3 Qxh1+",
        status = 31,
        turns = 24,
        variant = 1
      ).toChess
      "player" in {
        game.player must_== White
      }
      "pgn moves" in {
        game.pgnMoves must_==  "e4 Nc6 Nf3 Nf6 e5 Ne4 d3 Nc5 Be3 d6 d4 Ne4 Bd3 Bf5 Nc3 Nxc3 bxc3 Qd7 Bxf5 Qxf5 Nh4 Qe4 g3 Qxh1+"
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
    }
  }
}
