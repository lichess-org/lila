package shogi

import Pos._

class PlayOneMoveTest extends ShogiTest {

  "playing a move" should {
    "only process things once" in {
      makeGame.playMoves(C3 -> C4) must beSuccess
    }
  }
}
