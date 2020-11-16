package chess

class SituationTest extends ChessTest {

  "a game" should {
    "detect check" should {
      "by rook" in {
        ("""
K  r
""" as White).check must beTrue
      }
      "by knight" in {
        ("""
  n
K
""" as White).check must beTrue
      }
      "by bishop" in {
        ("""
  b

   
     K
""" as White).check must beTrue
      }
      "by pawn" in {
        ("""
    p
     K
""" as White).check must beTrue
      }
      "not" in {
        ("""
   n
K
""" as White).check must beFalse
      }
    }
    "detect check mate" in {
      "by rook" in {
        ("""
PP
K  r
""" as White).checkMate must beTrue
      }
      "by knight" in {
        ("""
PPn
KR
""" as White).checkMate must beTrue
      }
      "not" in {
        ("""
  n
K
""" as White).checkMate must beFalse
      }
    }
    "stale mate" in {
      "stuck in a corner" in {
        ("""
prr
K
""" as White).staleMate must beTrue
      }
      "not" in {
        ("""
  b
K
""" as White).staleMate must beFalse
      }
    }

    "Give the correct winner for a game" in {
      val game =
        """
PP
K  r
""" as White

      game.checkMate must beTrue
      game.winner must beSome.like {
        case color => color == Black
      }
    }

    "Not give a winner if the game is still in progress" in {
      val game = """
    p
     K
    """ as White

      game.winner must beNone

    }

    "not be playable" in {
      "with touching kings" in {
        val game = "kK BN" as Black
        game.playable(true) must beFalse
        game.playable(false) must beFalse
      }

      "with other side in check" in {
        val game = "k Q K" as White
        game.playable(true) must beFalse
        game.playable(false) must beFalse
      }
    }

  }
}
