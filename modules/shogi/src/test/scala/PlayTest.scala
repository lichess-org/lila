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
Gote:
l n s g k g s n l
. r . . . . . B .
p p p p p p . p p
. . . . . . p . .
. . . . . . . . .
. . P . . . . . .
P P . P P P P P P
. . . . . . . R .
L N S G K G S N L
Sente:B
"""
        }
      }
      "after recapture" in {
        game flatMap { _.playMoves(SQ3A -> SQ2B) } must beValid.like { case g =>
          addNewLines(g.board.visual) must_== """
Gote:b
l n s g k g . n l
. r . . . . . s .
p p p p p p . p p
. . . . . . p . .
. . . . . . . . .
. . P . . . . . .
P P . P P P P P P
. . . . . . . R .
L N S G K G S N L
Sente:B
"""
        }
      }
    }
  }
}
