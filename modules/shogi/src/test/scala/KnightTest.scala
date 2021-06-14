package shogi

import Pos._

class KnightTest extends ShogiTest {

  "a knight" should {

    val knight = Sente - Knight

    "move in any of 2 positions" in {
      pieceMoves(knight, E5) must bePoss(D7, F7)
    }

    "move in 1 one position when at the edges" in {
      pieceMoves(knight, I6) must bePoss(H8)
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
      board destsFrom C5 must bePoss(
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
      board destsFrom C5 must bePoss(
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
        board actorAt C5 map (_ threatens B7) must beSome(true)
      }
      "an unreachable enemy" in {
        board actorAt C5 map (_ threatens A6) must beSome(false)
      }
      "an unreachable friend" in {
        board actorAt C5 map (_ threatens E4) must beSome(false)
      }
      "nothing left" in {
        board actorAt C5 map (_ threatens B5) must beSome(false)
      }
      "nothing up" in {
        board actorAt C5 map (_ threatens C6) must beSome(false)
      }
    }
  }
}
