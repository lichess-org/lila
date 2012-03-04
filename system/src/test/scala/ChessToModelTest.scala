package lila.system

import model._
import lila.chess._

import format.Visual
import Pos._

class ChessToModelTest extends SystemTest {

  "chess to model conversion" should {
    "new game" in {
      val dbGame = newDbGame
      val game = dbGame.toChess
      "identity" in {
        val dbg2 = dbGame.update(game, anyMove)
        "white pieces" in {
          dbg2 playerByColor "white" map (_.ps) map sortPs must_== {
            dbGame playerByColor "white" map (_.ps) map sortPs
          }
        }
        "black pieces" in {
          dbg2 playerByColor "black" map (_.ps) map sortPs must_== {
            dbGame playerByColor "black" map (_.ps) map sortPs
          }
        }
      }
    }
    "played game" in {
      val dbGame = dbGame4
      val game = Game(
        board = """
r   kb r
ppp pppp
  np
    P
   P   N
  P B P
P P  P P
R  QK  q
""",
        player = White,
        pgnMoves = dbGame.pgn,
        deads = List(
          C3 -> Black.knight,
          F5 -> Black.bishop,
          F5 -> White.bishop,
          C3 -> White.knight,
          H1 -> White.rook
        ))
      "identity" in {
        val dbg2 = dbGame.update(game, anyMove)
        "white pieces" in {
          dbg2 playerByColor "white" map (_.ps) map sortPs must_== {
            dbGame playerByColor "white" map (_.ps) map sortPs
          }
        }
        "black pieces" in {
          dbg2 playerByColor "black" map (_.ps) map sortPs must_== {
            dbGame playerByColor "black" map (_.ps) map sortPs
          }
        }
      }
      "new pieces positions" in {
        val dbg2 = newDbGame.update(game, anyMove)
        "white pieces" in {
          dbg2 playerByColor "white" map (_.ps) map sortPs must_== {
            dbGame playerByColor "white" map (_.ps) map sortPs
          }
        }
        "black pieces" in {
          dbg2 playerByColor "black" map (_.ps) map sortPs must_== {
            dbGame playerByColor "black" map (_.ps) map sortPs
          }
        }
      }
    }
  }

  def sortPs(ps: String): String = ps.split(' ').toList.sorted mkString " "
}
