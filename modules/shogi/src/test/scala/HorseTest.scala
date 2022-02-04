package shogi

import Pos._

class HorseTest extends ShogiTest {

  "a horse" should {

    val horse = Sente - Horse

    "move diagonally in 4 directions and all around" in {
      pieceMoves(horse, SQ5E) must bePoss(
        SQ4F,
        SQ4D,
        SQ3G,
        SQ3C,
        SQ2H,
        SQ2B,
        SQ1I,
        SQ1A,
        SQ6F,
        SQ6D,
        SQ7G,
        SQ7C,
        SQ8H,
        SQ8B,
        SQ9I,
        SQ9A,
        SQ6E,
        SQ4E,
        SQ5D,
        SQ5F
      )
    }

    "move in 2 directions, when at the edges" in {
      pieceMoves(horse, SQ1C) must bePoss(SQ2B, SQ3A, SQ2D, SQ3E, SQ4F, SQ5G, SQ6H, SQ7I, SQ2C, SQ1D, SQ1B)
    }

    "not move to positions that are occupied by the same colour" in {
      val situation = """
k . B . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
N .+B . . . . P .
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
k . B . . . x . .
. . . . . x . . .
x . . . x . . . .
. x x x . . . . .
N x+B x . . . P .
. x x x . . . . .
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
. . . . . r . . .
p . . . . . . . .
. . . . . . . . .
N .+B . . . . P .
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
. . . . . x . . .
x . . . x . . . .
. x x x . . . . .
N x+B x . . . P .
. x x x . . . . .
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
. . . . . . . . .
N .+B . . . . P .
. . . . . . . . .
P P P P P P P . P
. . . . . . . . .
. . . . K . . . .
Hands:
Turn:Sente
"""
      "a reachable enemy" in {
        situation moveActorAt SQ7E map (_ threatens SQ9C) must beSome(true)
      }
      "an unreachable enemy" in {
        situation moveActorAt SQ7E map (_ threatens SQ7B) must beSome(false)
      }
      "a reachable friend" in {
        situation moveActorAt SQ7E map (_ threatens SQ9G) must beSome(true)
      }
      "nothing up left" in {
        situation moveActorAt SQ7E map (_ threatens SQ8D) must beSome(true)
      }
      "nothing down right" in {
        situation moveActorAt SQ7E map (_ threatens SQ6F) must beSome(true)
      }
    }
  }
}
