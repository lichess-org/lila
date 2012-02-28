package lila.chess
import Pos._

class PromotionTest extends LilaTest {

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
  }
}
