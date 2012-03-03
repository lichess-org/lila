package lila.system

import model._
import lila.chess._

import format.Visual
import Pos._

class ChessToModelTest extends SystemTest {

  "chess to model conversion" should {
    "new game" in {
      val game = newDbGame.toChess
      "identity" in {
        newDbGame update game must_== newDbGame
      }
    }
    "played game" in {
      val dbGame = dbGame4
      val game = Game("""
r   kb r
ppp pppp
  np
    P
   P   N
  P B P
P P  P P
R  QK  q
""", White, dbGame.pgn)
      "identity" in {
        dbGame update game must_== dbGame
      }
      "pieces" in {
        val dbg2 = newDbGame update game
        "white" in {
          dbg2 playerByColor "white" map (_.ps) map sortPs must_== {
            dbGame playerByColor "white" map (_.ps) map sortPs
          }
        }
        "black" in {
          dbg2 playerByColor "black" map (_.ps) map sortPs must_== {
            dbGame playerByColor "black" map (_.ps) map sortPs
          }
        }
      }
    }
  }

  def sortPs(ps: String): String = ps.split(' ').toList.sorted mkString " "
}
