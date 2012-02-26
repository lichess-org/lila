package lila.chess
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

    "move behind pawn barrier" in {
      """
PPPPPPPP
R  QK NR""" movesFrom(E1) must bePoss(F1)
    }

    "not move to positions that are occupied by the same colour" in {
      val board = """
   P
NPKP   P

PPPPPPPP
 NBQKBNR
"""
      board movesFrom C4 must bePoss(board, """



 xxP
NPKP   P
 xxx
PPPPPPPP
 NBQKBNR
""")
    }

    "capture hanging opponent pieces" in {
      val board = """
 bpp   k
  Kp
 p

"""
      board movesFrom C3 must bePoss(board, """




 xxx   k
  Kp
 x

""")
    }
    "threaten" in {
      val board = """
k B

 b B
bpp
  Kb
  P Q
PP   PPP
 NBQ BNR
"""
      "a reachable enemy" in {
        board actorAt C4 map (_ threatens B5) must succeedWith(true)
      }
      "an unreachable enemy" in {
        board actorAt C4 map (_ threatens A5) must succeedWith(false)
      }
      "a reachable friend" in {
        board actorAt C4 map (_ threatens C3) must succeedWith(false)
      }
    }
    "not move near from the other king" in {
      """
   k
 K
""" movesFrom B1 must bePoss(A1, A2, B2)
    }
  }
}
