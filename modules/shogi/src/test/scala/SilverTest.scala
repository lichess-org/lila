package shogi

import Pos._

class SilverTest extends ShogiTest {

  "a silver general" should {

    val silver = Sente - Silver

    "move in 6 directions" in {
      pieceMoves(silver, E5) must bePoss(E6, F6, F4, D4, D6)
    }

    "move in 2 directions, when at the edges" in {
      pieceMoves(silver, I9) must bePoss(H8)
    }

    "not move to positions that are occupied by the same colour" in {
      val board = """
k B



 NS     P
 G
PPPPPPPPP

    K
"""
      board destsFrom C5 must bePoss(
        board,
        """
k B


 xxx
 NS     P
 G x
PPPPPPPPP

    K
"""
      )
    }

    "capture opponent pieces" in {
      val board = """
k B


  p
N S     P

PPPPPPPPP

    K
"""
      board destsFrom C5 must bePoss(
        board,
        """
k B


 xxx
N S     P
 x x
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
 NS    P
   P
PPP PPPPP

    K
"""
      "a reachable enemy" in {
        board actorAt C5 map (_ threatens C6) must beSome(true)
      }
      "an unreachable enemy" in {
        board actorAt C5 map (_ threatens A7) must beSome(false)
      }
      "a reachable friend" in {
        board actorAt C5 map (_ threatens D4) must beSome(true)
      }
      "nothing down left" in {
        board actorAt C5 map (_ threatens B4) must beSome(true)
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
