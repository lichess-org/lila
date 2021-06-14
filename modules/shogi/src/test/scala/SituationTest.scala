package shogi

class SituationTest extends ShogiTest {

  "a game" should {
    "detect check" should {
      "by rook" in {
        ("""
K  r
""" as Sente).check must beTrue
      }
      "by knight" in {
        ("""
 n

K
""" as Sente).check must beTrue
      }
      "by bishop" in {
        ("""
  b

   
     K
""" as Sente).check must beTrue
      }
      "by pawn" in {
        ("""
    p
    K
""" as Sente).check must beTrue
      }
      "not" in {
        ("""
K

 n
""" as Sente).check must beFalse
      }
    }
    "detect check mate" in {
      "by rook" in {
        ("""
PP
K  r
""" as Sente).checkMate must beTrue
      }
      "by knight" in {
        ("""
 n
PB
KR
""" as Sente).checkMate must beTrue
      }
      "not" in {
        ("""
 n
 
K
""" as Sente).checkMate must beFalse
      }
    }
    "stale mate" in {
      "stuck in a corner" in {
        ("""
brr
K
""" as Sente).staleMate must beTrue
      }
      "not" in {
        ("""
  b
K
""" as Sente).staleMate must beFalse
      }
    }

    "Give the correct winner for a game" in {
      val game =
        """
PP
K  r
""" as Sente

      game.checkMate must beTrue
      game.winner must beSome.like { case color =>
        color == Gote
      }
    }

    "Not give a winner if the game is still in progress" in {
      val game = """
    p
     K
    """ as Sente

      game.winner must beNone

    }

    "not be playable" in {
      "with touching kings" in {
        val game = "kK BN" as Gote
        game.playable(true) must beFalse
        game.playable(false) must beFalse
      }

      "with other side in check" in {
        val game = "k Q K" as Sente
        game.playable(true) must beFalse
        game.playable(false) must beFalse
      }
    }

  }
}
