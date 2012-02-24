package lila
package model

import Pos._

class KingTest extends LilaSpec {

  "a king" should {

    val king = White - King

    def moves(pos: Pos): Valid[Set[Pos]] = Board.empty place king at pos flatMap { b ⇒
      b actorAt pos map (_.moves)
    }

    "move 1 position in any direction" in {
      moves(D4) must bePoss(D3, C3, C4, C5, D5, E5, E4, E3)
    }

    "move 1 position in any direction, even from the edges" in {
      moves(H8) must bePoss(H7, G7, G8)
    }

    "not move to positions that are occupied by the same colour" in {
      val board = """
k B


   P
NPKP   P

PPPPPPPP
 NBQKBNR
"""
      board movesFrom C4 must bePoss(board, """
k B


 xxP
NPKP   P
 xxx
PPPPPPPP
 NBQKBNR
""")
    }

    "capture opponent pieces" in {
      val board = """
k B


  pP
NPKp   P
 p
PPPPPPPP
 NBQKBNR
"""
      board movesFrom C4 must bePoss(board, """
k B


 xxP
NPKx   P
 xxx
PPPPPPPP
 NBQKBNR
""")
    }
    "threaten nothing" in {
      val board = """
k B

 b B
bpp
  Kb
    Q
PPP  PPP
 NBQ BNR
"""
      forall(Pos.all) { pos ⇒
        board actorAt C4 map (_ threatens pos) must succeedWith(false)
      }
    }
  }
}
