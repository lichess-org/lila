package lila
package model

import Pos._
import format.Visual

class KnightTest extends LilaSpec {

  "a knight" should {

    val knight = White - Knight

    def basicMoves(pos: Pos): Valid[Set[Pos]] = Board.empty place knight at pos map { b ⇒
      knight.basicMoves(pos, b)
    }

    "be able to move in any of 8 positions, 2 and 1 squares away" in {
      basicMoves(E4) must bePoss(F6, G5, G3, F2, D2, C3, C5, D6)
    }

    "be able to move 2 and 1 squares away, even when at the edges" in {
      basicMoves(H8) must bePoss(G6, F7)
    }

    "not move to positions that are occupied by the same colour" in {
      val board = Visual << """
k B

   B
    P
  N
    P
PPP  PPP
 NBQKBNR
"""
      board pieceAt C4 map { _.basicMoves(C4, board) } must bePoss(board, """
k B

 x B
x   P
  N
x   P
PPPx PPP
 NBQKBNR
""")
    }

    "can capture opponent pieces" in {
      val board = Visual << """
k B

 b B
b
  N
    q
PPP  PPP
 NBQKBNR
"""
      board pieceAt C4 map { _.basicMoves(C4, board) } must bePoss(board, """
k B

 x B
x   x
  N
x   x
PPPx PPP
 NBQKBNR
""")
    }
  }
}
