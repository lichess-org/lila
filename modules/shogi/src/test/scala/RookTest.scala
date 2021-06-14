package shogi

import Pos._

class RookTest extends ShogiTest {

  "a rook" should {

    val rook = Sente - Rook

    "move to any position along the same rank or file" in {
      pieceMoves(rook, E5) must bePoss(E6, E7, E8, E9, E4, E3, E2, E1, F5, G5, H5, I5, D5, C5, B5, A5)
    }

    "move to any position along the same rank or file, even when at the edges" in {
      pieceMoves(rook, I9) must bePoss(I8, I7, I6, I5, I4, I3, I2, I1, H9, G9, F9, E9, D9, C9, B9, A9)
    }

    "not move to positions that are occupied by the same colour" in {
      """
k B



N R    P

PPPPPPPPP

    K
""" destsFrom C5 must bePoss(C4, C6, C7, C7, C8, C8, B5, D5, E5, F5, G5)
    }

    "capture opponent pieces" in {
      """
k
  b


n R   p

PPPPPPPPP

    K
""" destsFrom C5 must bePoss(C4, C6, C7, C7, C8, C8, B5, A5, D5, E5, F5, G5)
    }
    "threaten" in {
      val board = """
k B
  r  r
p

n R    P

PPPPPPPPP

    K
"""
      "a reachable enemy to the left" in {
        board actorAt C5 map (_ threatens A5) must beSome(true)
      }
      "a reachable enemy to the top" in {
        board actorAt C5 map (_ threatens C8) must beSome(true)
      }
      "an unreachable enemy" in {
        board actorAt C5 map (_ threatens A7) must beSome(false)
      }
      "a reachable friend" in {
        board actorAt C5 map (_ threatens H5) must beSome(true)
      }
      "nothing left" in {
        board actorAt C5 map (_ threatens B5) must beSome(true)
      }
      "nothing up" in {
        board actorAt C5 map (_ threatens C6) must beSome(true)
      }
    }
  }
}
