package shogi

import Pos._

class GoldTest extends ShogiTest {

  "a gold general" should {

    val gold = Sente - Gold

    "move in 6 directions" in {
      pieceMoves(gold, SQ5E) must bePoss(SQ5D, SQ4D, SQ4E, SQ5F, SQ6E, SQ6D)
    }

    "move in 2 directions, when at the edges" in {
      pieceMoves(gold, SQ1A) must bePoss(SQ1B, SQ2A)
    }

    "not move to positions that are occupied by the same colour" in {
      val board = """
k B



 NG     P

PPPPPPPPP

    K
"""
      board destsFrom SQ7E must bePoss(
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
      board destsFrom SQ7E must bePoss(
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
        board actorAt SQ7E map (_ threatens SQ7D) must beSome(true)
      }
      "an unreachable enemy" in {
        board actorAt SQ7E map (_ threatens SQ9C) must beSome(false)
      }
      "a reachable friend" in {
        board actorAt SQ7E map (_ threatens SQ7F) must beSome(true)
      }
      "nothing left" in {
        board actorAt SQ7E map (_ threatens SQ8E) must beSome(true)
      }
      "nothing right" in {
        board actorAt SQ7E map (_ threatens SQ6E) must beSome(true)
      }
      "nothing left up" in {
        board actorAt SQ7E map (_ threatens SQ8D) must beSome(true)
      }
      "nothing right up" in {
        board actorAt SQ7E map (_ threatens SQ6D) must beSome(true)
      }
    }
  }
}
