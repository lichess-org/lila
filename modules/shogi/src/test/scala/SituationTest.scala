package shogi

import scala.language.reflectiveCalls

class SituationTest extends ShogiTest {

  "a game" should {
    "detect check" should {
      "by rook" in {
        """
K . . r . . . . .
""".check must beTrue
      }
      "by knight" in {
        """
. n . . . . . . .
. . . . . . . . .
K . . . . . . . .
""".check must beTrue
      }
      "by bishop" in {
        """
. . b . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . K . . .
""".check must beTrue
      }
      "by pawn" in {
        """
. . . . p . . . .
. . . . K . . . .
""".check must beTrue
      }
      "not" in {
        """
K . . . . . . . .
. . . . . . . . .
.+n . . . . . . .
""".check must beFalse
      }
    }
    "detect check mate" in {
      "by rook" in {
        """
P P . . . . . . .
K . . r . . . . .
""".checkmate must beTrue
      }
      "by knight" in {
        """
. n . . . . . . .
P B . . . . . . .
K R . . . . . . .
""".checkmate must beTrue
      }
      "not" in {
        """
. n . . . . . . .
. . . . . . . . .
K . . . . . . . .
""".checkmate must beFalse
      }
    }
    "stale mate" in {
      "stuck in a corner" in {
        """
b r r . . . . . .
K . . . . . . . .
""".stalemate must beTrue
      }
      "not" in {
        """
. . b . . . . . .
K . . . . . . . .
""".stalemate must beFalse
      }
    }

    "Give the correct winner for a game" in {
      val game =
        """
P P . . . . . . .
K . . r . . . . .
"""

      game.checkmate must beTrue
      game.winner must beSome.like { case color =>
        color.gote
      }
    }

    "Not give a winner if the game is still in progress" in {
      val game = """
. . . . p . . . .
. . . . . K . . .
"""

      game.winner must beNone

    }

    "not be playable" in {
      "with touching kings" in {
        val game = "k K . B N . . . ." as Gote
        game.playable(strict = true, withImpasse = true) must beFalse
        game.playable(strict = false, withImpasse = true) must beFalse
      }

      "with other side in check" in {
        val game = "k . R . K . . . ."
        game.playable(strict = true, withImpasse = true) must beFalse
        game.playable(strict = false, withImpasse = true) must beFalse
      }

      "with doubled pawns" in {
        val game = """
k . . . . . . . .
. . . . . . . . .
P p . . . . . . .
P . . . . . . . .
K . . . . . . . .
"""
        game.playable(strict = true, withImpasse = true) must beFalse
        game.playable(strict = false, withImpasse = true) must beFalse
      }
    }

  }
}
