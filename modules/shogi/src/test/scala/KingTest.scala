package shogi

import Pos._

class KingTest extends ShogiTest {

  "a king" should {

    val king = Sente - King

    "move 1 position in any direction" in {
      pieceMoves(king, SQ6F) must bePoss(SQ6G, SQ7G, SQ7F, SQ7E, SQ6E, SQ5E, SQ5F, SQ5G)
    }

    "move 1 position in any direction, even from the edges" in {
      pieceMoves(king, SQ1A) must bePoss(SQ1B, SQ2B, SQ2A)
    }

    "move behind pawn barrier" in {
      """
P P P P P P P P P
L N . G K . . N L""" destsFrom SQ5I must bePoss(SQ4I)
    }

    "not move to positions that are occupied by the same colour" in {
      val board = """
. . . P . . . . .
N P K L . . . P .
. . . . . . . . .
P . P . P P P . P
. . . . . . . . .
. . S G K G S N L
"""
      board destsFrom SQ7E must bePoss(
        board,
        """
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. x x P . . . . .
N P K L . . . P .
. x x x . . . . .
P . P . P P P . P
. . . . . . . . .
. . S G K G S N L
"""
      )
    }

    "capture hanging opponent pieces" in {
      val board = """
k . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. b p l . . . . .
. . K p . . . . .
. p . . . . . . .
l . . . . . . . .
"""
      board destsFrom SQ7G must bePoss(
        board,
        """
k . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. x x x . . . . .
. x K p . . . . .
. x x . . . . . .
l . . . . . . . .
"""
      )
    }
    "threaten" in {
      val board = """
k . B . . . . . .
. . . . . . . . .
. b . B . . . . .
b p p . . . . . .
. . K b . . . . .
. . P . R . . . .
P P . . . P P P P
. . . . . . . . .
L N S G . G S N L
"""
      "a reachable enemy" in {
        board actorAt SQ7E map (_ threatens SQ8D) must beSome(true)
      }
      "an unreachable enemy" in {
        board actorAt SQ7E map (_ threatens SQ9D) must beSome(false)
      }
      "a reachable friend" in {
        board actorAt SQ7E map (_ threatens SQ7F) must beSome(true)
      }
    }
    "not move near from the other king" in {
      """
. . . k
. K . .
""" destsFrom SQ8I must bePoss(SQ9I, SQ9H, SQ8H)
    }
  }
}
