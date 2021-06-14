package shogi

import scalaz.Validation.FlatMap._
import Pos._
import shogi.variant.Standard

class PromotionTest extends ShogiTest {

  "pawn promotion" should {
    val board = """
  p

K       """
    val game  = Game(board, Gote)
    "promote to a tokin" in {
      game.playMove(C3, C2, true) must beGame("""
  t
K       """)
    }
    "don't force promotion by default" in {
      game.playMove(C3, C2) must beGame("""
  p
K       """)
    }
    "don't promote" in {
      game.playMove(C3, C2, false) must beGame("""
  p
K       """)
    }
    "promotion by killing" in {
      Game(
        """
  p
K R""",
        Gote
      ).playMove(C2, C1, true) must beGame("""

K t""")
    }
  }
}
