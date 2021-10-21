package shogi

import format.Visual.addNewLines
import Pos._

class PlayTest extends ShogiTest {

  "playing a game" should {
    "opening one" in {
      val game =
        makeGame.playMoves(SQ7G -> SQ7F, SQ3C -> SQ3D, SQ8H -> SQ2B)
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
        game flatMap { _.playMoves(SQ3A -> SQ2B) } must beValid.like { case g =>
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
