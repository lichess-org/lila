package chess

import variant.RacingKings

class RacingKingsVariantTest extends ChessTest {

  "Racing Kings" should {

    "disallow discovered check" in {
      val fenPosition = "1r6/8/5qRK/8/7Q/8/2knNn2/2b2B2 b - - 11 11"
      val game        = fenToGame(fenPosition, RacingKings).toOption err "ooopses"
      game.situation.destinations get Pos.D2 must beNone
    }
  }
}
