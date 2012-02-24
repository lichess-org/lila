package lila
package model

import Pos._
import format.Visual

class KingTest extends LilaSpec {

  "a king" should {

    val king = White - King

    def basicMoves(pos: Pos): Valid[Set[Pos]] = Board.empty place king at pos map { b ⇒
      king.basicMoves(pos, b)
    }

    "move 1 position in any direction" in {
      basicMoves(D4) must bePoss(D3, C3, C4, C5, D5, E5, E4, E3)
    }

    "move 1 position in any direction, even from the edges" in {
      basicMoves(H8) must bePoss(H7, G7, G8)
    }

    "not move to positions that are occupied by the same colour" in {
      val board = Visual << """
k B


   P
NPKP   P

PPPPPPPP
 NBQKBNR
"""
      board pieceAt C4 map { _.basicMoves(C4, board) } must bePoss(board, """
k B


 xxP
NPKP   P
 xxx
PPPPPPPP
 NBQKBNR
""")
    }

    "can capture opponent pieces" in {
      val board = Visual << """
k B


  pP
NPKp   P
 p
PPPPPPPP
 NBQKBNR
"""
      board pieceAt C4 map { _.basicMoves(C4, board) } must bePoss(board, """
k B


 xxP
NPKx   P
 xxx
PPPPPPPP
 NBQKBNR
""")
    }
  }
}
