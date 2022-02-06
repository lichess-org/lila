package shogi

import Pos._

class SilverTest extends ShogiTest {

  "a silver general" should {

    val silver = Sente - Silver

    "move in 6 directions" in {
      pieceMoves(silver, SQ5E) must bePoss(SQ5D, SQ4D, SQ4F, SQ6F, SQ6D)
    }

    "move in 2 directions, when at the edges" in {
      pieceMoves(silver, SQ1A) must bePoss(SQ2B)
    }

    "not move to positions that are occupied by the same colour" in {
      val situation = """
k . B . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. N S . . . . P .
. G . . . . . . .
P P P P P P P . P
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
. . . . . . . . .
. x x x . . . . .
. N S . . . . P .
. G . x . . . . .
P P P P P P P . P
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
. . . . . . . . .
. . p . . . . . .
N . S . . . . P .
. . . . . . . . .
P P P P P P P . P
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
. . . . . . . . .
. x x x . . . . .
N . S . . . . P .
. x . x . . . . .
P P P P P P P . P
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
. . r . . r . . .
p . . . . . . . .
. . p . . . . . .
. N S . . . . P .
. . . P . . . . .
P P P . P P P . P
. . . . . . . . .
. . . . K . . . .
Hands:
Turn:Sente
"""
      "a reachable enemy" in {
        situation moveActorAt SQ7E map (_ threatens SQ7D) must beSome(true)
      }
      "an unreachable enemy" in {
        situation moveActorAt SQ7E map (_ threatens SQ9C) must beSome(false)
      }
      "a reachable friend" in {
        situation moveActorAt SQ7E map (_ threatens SQ6F) must beSome(true)
      }
      "nothing down left" in {
        situation moveActorAt SQ7E map (_ threatens SQ8F) must beSome(true)
      }
      "nothing left up" in {
        situation moveActorAt SQ7E map (_ threatens SQ8D) must beSome(true)
      }
      "nothing right up" in {
        situation moveActorAt SQ7E map (_ threatens SQ6D) must beSome(true)
      }
    }
  }
}
