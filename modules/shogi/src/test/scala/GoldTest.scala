package shogi

import Pos._

class GoldTest extends ShogiTest {

  "a gold general" should {

    val gold = Sente - Gold

    "move in 6 directions" in {
      pieceMoves(gold, E5) must bePoss(E6, F6, F5, E4, D5, D6)
    }

    "move in 2 directions, when at the edges" in {
      pieceMoves(gold, I9) must bePoss(I8, H9)
    }

    "not move to positions that are occupied by the same colour" in {
      val board = """
k B



 NG     P

PPPPPPPPP

    K
"""
      board destsFrom C5 must bePoss(
        board,
        """
k B


 xxx
 NGx    P
  x
PPPPPPPPP

    K
"""
      )
    }

    "capture opponent pieces" in {
      val board = """
k B


  p
N G     P

PPPPPPPPP

    K
"""
      board destsFrom C5 must bePoss(
        board,
        """
k B


 xxx
NxGx    P
  x
PPPPPPPPP

    K
"""
      )
    }
    "threaten" in {
      val board = """
k B
  r  r
p
  p
N G     P
  P
PP PPPPPP

    K
"""
      "a reachable enemy" in {
        board actorAt C5 map (_ threatens C6) must beSome(true)
      }
      "an unreachable enemy" in {
        board actorAt C5 map (_ threatens A7) must beSome(false)
      }
      "a reachable friend" in {
        board actorAt C5 map (_ threatens C4) must beSome(true)
      }
      "nothing left" in {
        board actorAt C5 map (_ threatens B5) must beSome(true)
      }
      "nothing right" in {
        board actorAt C5 map (_ threatens D5) must beSome(true)
      }
      "nothing left up" in {
        board actorAt C5 map (_ threatens B6) must beSome(true)
      }
      "nothing right up" in {
        board actorAt C5 map (_ threatens D6) must beSome(true)
      }
    }
  }
}
