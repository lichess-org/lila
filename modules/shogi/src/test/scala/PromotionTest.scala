package shogi

import Pos._

class PromotionTest extends ShogiTest {

  "pawn promotion" should {
    val board = """
  p

K       """
    val game  = Game(board, Gote)
    "promote to a tokin" in {
      game.playMove(SQ7G, SQ7H, true) must beGame("""
  t
K       """)
    }
    "don't force promotion by default" in {
      game.playMove(SQ7G, SQ7H) must beGame("""
  p
K       """)
    }
    "don't promote" in {
      game.playMove(SQ7G, SQ7H, false) must beGame("""
  p
K       """)
    }
    "promotion by killing" in {
      Game(
        """
  p
K R""",
        Gote
      ).playMove(SQ7H, SQ7I, true) must beGame("""

K t""")
    }
  }
}
