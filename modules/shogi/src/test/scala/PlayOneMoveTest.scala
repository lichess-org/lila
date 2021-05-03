package chess

import Pos._

class PlayOneMoveTest extends ChessTest {

  "playing a move" should {
    "only process things once" in {
      makeGame.playMoves(E2 -> E4) must beSuccess
    }
  }
}
