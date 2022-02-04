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
      val situation = """
k . B . . . . . .
. . . . . . . . .
. . . B . . . . .
. . . . P . . . .
. . N . . . . . .
. . . P . . . . .
P P P . . P P P P
. . . . . . . . .
. . . . K . . . .
Hands:
Turn:Sente
"""
      situation moveDestsFrom SQ7E must bePoss(
        situation,
        """
k . B . . . . . .
. . . . . . . . .
. x . B . . . . .
. . . . P . . . .
. . N . . . . . .
. . . P . . . . .
P P P . . P P P P
. . . . . . . . .
. . . . K . . . .
Hands:
Turn:Sente
"""
      )
    }

    "capture opponent pieces" in {
      val situation = """
k . B . . . . . .
. . . . . . . . .
. b . B . . . . .
n . . . . . . . .
. . N . . . . . .
. . . . b . . . .
P P P . . P P P P
. . . . . . . . .
. . . . K . . . .
Hands:
Turn:Sente
"""
      situation moveDestsFrom SQ7E must bePoss(
        situation,
        """
k . B . . . . . .
. . . . . . . . .
. x . B . . . . .
n . . . . . . . .
. . N . . . . . .
. . . . b . . . .
P P P . . P P P P
. . . . . . . . .
. . . . K . . . .
Hands:
Turn:Sente
"""
      )
    }
    "threaten" in {
      val situation = """
k . B . . . . . .
. . . . . . . . .
. b . B . . . . .
n . . . . . . . .
. . N . . . . . .
. . . . R . . . .
P P P . . P P P P
. . . . . . . . .
. . . . K . . . .
Hands:
Turn:Sente
"""
      "a reachable enemy" in {
        situation moveActorAt SQ7E map (_ threatens SQ8C) must beSome(true)
      }
      "an unreachable enemy" in {
        situation moveActorAt SQ7E map (_ threatens SQ9D) must beSome(false)
      }
      "an unreachable friend" in {
        situation moveActorAt SQ7E map (_ threatens SQ5F) must beSome(false)
      }
      "nothing left" in {
        situation moveActorAt SQ7E map (_ threatens SQ8E) must beSome(false)
      }
      "nothing up" in {
        situation moveActorAt SQ7E map (_ threatens SQ7D) must beSome(false)
      }
    }
  }
}
