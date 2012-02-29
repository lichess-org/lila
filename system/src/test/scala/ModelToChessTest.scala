package lila.system

import model._
import lila.chess

class ModelToChessTest extends SystemTest {

  "model to chess conversion" should {
    "new game" in {
      newGame.toChess must_== chess.Game()
    }
  }
}
