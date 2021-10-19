package shogi

import format.Visual.addNewLines
import Pos._

class PlayTest extends ShogiTest {

  "playing a game" should {
    "opening one" in {
      val game =
        makeGame.playMoves(C3 -> C4, G7 -> G6, B2 -> H8)
      "current game" in {
        game must beValid.like { case g =>
          addNewLines(g.board.visual) must_== """
lnsgkgsnl
 r     B
pppppp pp
      p

  P
PP PPPPPP
       R
LNSGKGSNL
"""
        }
      }
      "after recapture" in {
        game flatMap { _.playMoves(G9 -> H8) } must beValid.like { case g =>
          addNewLines(g.board.visual) must_== """
lnsgkg nl
 r     s
pppppp pp
      p

  P
PP PPPPPP
       R
LNSGKGSNL
"""
        }
      }
    }
  }
}
