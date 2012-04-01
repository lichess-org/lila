package lila.chess
import Pos._

class PromotionTest extends ChessTest {

  "pawn promotion" should {
    val board = """
  p
K      """
    val game = Game(board, Black)
    "promote to a queen" in {
      game.playMove(C2, C1, Queen) must beGame("""

K q    """)
    }
    "promote to a queen by default" in {
      game.playMove(C2, C1) must beGame("""

K q    """)
    }
    "promote to a knight" in {
      game.playMove(C2, C1, Knight) must beGame("""

K n    """)
    }
    "promote to a queen by killing" in {
      Game("""
  p
K  R""", Black).playMove(C2, D1) must beGame("""

K  q""")
    }
    "promote to a knight by killing" in {
      Game("""
  p
K  R""", Black).playMove(C2, D1, Knight) must beGame("""

K  n""")
    }
    "promote to a whiteknight" in {
      Game("""

P





K n    """).playMove(A7, A8, Knight) must beGame("""
N






K n    """)
    }
  }
}
