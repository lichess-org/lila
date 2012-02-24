package lila
package model

import Pos._
import format.Visual

class BishopTest extends LilaSpec {

  "a bishop" should {

    val bishop = White - Bishop

    def basicMoves(pos: Pos): Valid[Set[Pos]] = Board.empty place bishop at pos map { b ⇒
      bishop.basicMoves(pos, b)
    }

    "move in any of 8 positions, 2 and 1 squares away" in {
      basicMoves(E4) must bePoss(F3, G2, H1, D5, C6, B7, A8, D3, C2, B1, F5, G6, H7)
    }

    "move in any of 8 positions, 2 and 1 squares away, even when at the edges" in {
      basicMoves(H7) must bePoss(G8, G6, F5, E4, D3, C2, B1)
    }

    "not move to positions that are occupied by the same colour" in {
      val board = Visual << """
k B



N B    P

PPPPPPPP
 NBQKBNR
"""
      board pieceAt C4 map { _.basicMoves(C4, board) } must bePoss(board, """
k B   x
     x
x   x
 x x
N B    P
 x x
PPPPPPPP
 NBQKBNR
""")
    }

    //"can capture opponent pieces" in {
      //val board = Visual << """
//k
  //b


//n R   p

//PPPPPPPP
 //NBQKBNR
//"""
      //board pieceAt C4 map { _.basicMoves(C4, board) } must bePoss(
        //C3, C5, C6, C7, B4, A4, D4, E4, F4, G4)
    //}
  }
}
