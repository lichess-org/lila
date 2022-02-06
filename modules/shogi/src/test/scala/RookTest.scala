package shogi

import Pos._

class RookTest extends ShogiTest {

  "a rook" should {

    val rook = Sente - Rook

    "move to any position along the same rank or file" in {
      pieceMoves(rook, SQ5E) must bePoss(
        SQ5D,
        SQ5C,
        SQ5B,
        SQ5A,
        SQ5F,
        SQ5G,
        SQ5H,
        SQ5I,
        SQ4E,
        SQ3E,
        SQ2E,
        SQ1E,
        SQ6E,
        SQ7E,
        SQ8E,
        SQ9E
      )
    }

    "move to any position along the same rank or file, even when at the edges" in {
      pieceMoves(rook, SQ1A) must bePoss(
        SQ1B,
        SQ1C,
        SQ1D,
        SQ1E,
        SQ1F,
        SQ1G,
        SQ1H,
        SQ1I,
        SQ2A,
        SQ3A,
        SQ4A,
        SQ5A,
        SQ6A,
        SQ7A,
        SQ8A,
        SQ9A
      )
    }

    "not move to positions that are occupied by the same colour" in {
      """
k . B . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
N . R . . . . P .
. . . . . . . . .
P P P P P P P . P
. . . . . . . . .
. . . . K . . . .
Hands:
Turn:Sente
""" moveDestsFrom SQ7E must bePoss(SQ7F, SQ7D, SQ7C, SQ7B, SQ8E, SQ6E, SQ5E, SQ4E, SQ3E)
    }

    "capture opponent pieces" in {
      """
k . . . . . . . .
. . b . . . . . .
. . . . . . . . .
. . . . . . . . .
n . R . . . p . .
. . . . . . . . .
P P P P P P P P P
. . . . . . . . .
. . . . K . . . .
Hands:
Turn:Sente
""" moveDestsFrom SQ7E must bePoss(SQ7F, SQ7D, SQ7C, SQ7B, SQ8E, SQ9E, SQ6E, SQ5E, SQ4E, SQ3E)
    }
    "threaten" in {
      val situation = """
k . B . . . . . .
. . r . . r . . .
p . . . . . . . .
. . . . . . . . .
n . R . . . . P .
. . . . . . . . .
P P P P P P P . P
. . . . . . . . .
. . . . K . . . .
Hands:
Turn:Sente
"""
      "a reachable enemy to the left" in {
        situation moveActorAt SQ7E map (_ threatens SQ9E) must beSome(true)
      }
      "a reachable enemy to the top" in {
        situation moveActorAt SQ7E map (_ threatens SQ7B) must beSome(true)
      }
      "an unreachable enemy" in {
        situation moveActorAt SQ7E map (_ threatens SQ9C) must beSome(false)
      }
      "a reachable friend" in {
        situation moveActorAt SQ7E map (_ threatens SQ2E) must beSome(true)
      }
      "nothing left" in {
        situation moveActorAt SQ7E map (_ threatens SQ8E) must beSome(true)
      }
      "nothing up" in {
        situation moveActorAt SQ7E map (_ threatens SQ7D) must beSome(true)
      }
    }
  }
}
