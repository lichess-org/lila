package shogi

import Pos._

class KnightTest extends ShogiTest {

  "a knight" should {

    val knight = Sente - Knight

    "move in any of 2 positions" in {
      pieceMoves(knight, SQ5E) must bePoss(SQ6C, SQ4C)
    }

    "move in 1 one position when at the edges" in {
      pieceMoves(knight, SQ1D) must bePoss(SQ2B)
    }

    "not move to positions that are occupied by the same colour" in {
      val board = """
k B

   B
    P
  N
    P
PPP  PPPP

    K
"""
      board destsFrom SQ7E must bePoss(
        board,
        """
k B

 x B
    P
  N
    P
PPP  PPPP

    K
"""
      )
    }

    "capture opponent pieces" in {
      val board = """
k B

 b B
n
  N
    b
PPP  PPPP

    K
"""
      board destsFrom SQ7E must bePoss(
        board,
        """
k B

 x B
n
  N
    b
PPP  PPPP

    K
"""
      )
    }
    "threaten" in {
      val board = """
k B

 b B
n
  N
    R
PPP  PPPP

    K
"""
      "a reachable enemy" in {
        board actorAt SQ7E map (_ threatens SQ8C) must beSome(true)
      }
      "an unreachable enemy" in {
        board actorAt SQ7E map (_ threatens SQ9D) must beSome(false)
      }
      "an unreachable friend" in {
        board actorAt SQ7E map (_ threatens SQ5F) must beSome(false)
      }
      "nothing left" in {
        board actorAt SQ7E map (_ threatens SQ8E) must beSome(false)
      }
      "nothing up" in {
        board actorAt SQ7E map (_ threatens SQ7D) must beSome(false)
      }
    }
  }
}
