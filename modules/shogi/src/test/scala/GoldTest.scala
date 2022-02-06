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
      val situation = """
k . B . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. N G . . . . P .
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
. N G x . . . P .
. . x . . . . . .
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
N . G . . . . P .
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
N x G x . . . P .
. . x . . . . . .
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
. . r . r . . . .
p . . . . . . . .
. . p . . . . . .
N . G . . . . P .
. . P . . . . . .
P P P P P P P . P
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
        situation moveActorAt SQ7E map (_ threatens SQ7F) must beSome(true)
      }
      "nothing left" in {
        situation moveActorAt SQ7E map (_ threatens SQ8E) must beSome(true)
      }
      "nothing right" in {
        situation moveActorAt SQ7E map (_ threatens SQ6E) must beSome(true)
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
