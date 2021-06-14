package shogi

import Pos._

class KingTest extends ShogiTest {

  "a king" should {

    val king = Sente - King

    "move 1 position in any direction" in {
      pieceMoves(king, D4) must bePoss(D3, C3, C4, C5, D5, E5, E4, E3)
    }

    "move 1 position in any direction, even from the edges" in {
      pieceMoves(king, I9) must bePoss(I8, H8, H9)
    }

    "move behind pawn barrier" in {
      """
PPPPPPPPP
LN GK  NL""" destsFrom E1 must bePoss(F1)
    }

    "not move to positions that are occupied by the same colour" in {
      val board = """


  
   P
NPKL   P

P P PPP P

  SGKGSNL
"""
      board destsFrom C5 must bePoss(
        board,
        """



 xxP
NPKL   P
 xxx
P P PPP P

  SGKGSNL
"""
      )
    }

    "capture hanging opponent pieces" in {
      val board = """
k




 bpl
  Kp
 p
l
"""
      board destsFrom C3 must bePoss(
        board,
        """
k




 xxx
 xKp
 xx
l
"""
      )
    }
    "threaten" in {
      val board = """
k B

 b B
bpp
  Kb
  P R
PP   PPPP

LNSG BNL
"""
      "a reachable enemy" in {
        board actorAt C5 map (_ threatens B6) must beSome(true)
      }
      "an unreachable enemy" in {
        board actorAt C5 map (_ threatens A6) must beSome(false)
      }
      "a reachable friend" in {
        board actorAt C5 map (_ threatens C4) must beSome(true)
      }
    }
    "not move near from the other king" in {
      """
   k
 K
""" destsFrom B1 must bePoss(A1, A2, B2)
    }
  }
}
