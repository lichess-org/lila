package shogi

import Pos._

class PromotionTest extends ShogiTest {

  "pawn promotion" should {
    val situation = """
. . p . . . . . .
. . . . . . . . .
K . . . . . . . .
Hands:
Turn:Gote
"""
    val game      = Game(situation)
    "promote to a tokin" in {
      game.playMove(SQ7G, SQ7H, true) must beGame("""
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. .+p . . . . . .
K . . . . . . . .
Hands:
Turn:Sente
""")
    }
    "don't force promotion by default" in {
      game.playMove(SQ7G, SQ7H) must beGame("""
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . p . . . . . .
K . . . . . . . .
Hands:
Turn:Sente
""")
    }
    "don't promote" in {
      game.playMove(SQ7G, SQ7H, false) must beGame("""
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . p . . . . . .
K . . . . . . . .
Hands:
Turn:Sente
""")
    }
    "promotion by killing" in {
      Game(
        """
. . p . . . . . .
K . R . . . . . .
Turn:Gote"""
      ).playMove(SQ7H, SQ7I, true) must beGame("""
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
. . . . . . . . .
K .+p . . . . . .
Hands:r
Turn:Sente""")
    }
  }
}
