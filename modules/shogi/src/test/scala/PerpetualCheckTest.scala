package shogi

import Pos._

class PerpetualCheckTest extends ShogiTest {

  "Perpetual check" should {
    val g = makeGame.playMoves(
      E3 -> E4,
      E7 -> E6,
      H2 -> E2,
      E9 -> E8,
      E4 -> E5,
      E8 -> E7,
      E2 -> E4,
      E7 -> D6
    )
    //             1st                                     2nd                                      3rd
    val m = List(
      E4 -> D4,
      D6 -> C6,
      D4 -> C4,
      C6 -> D6,
      C4 -> D4,
      D6 -> C6,
      D4 -> C4,
      C6 -> D6,
      C4 -> D4,
      D6 -> C6,
      D4 -> C4,
      C6 -> D6,
      C4 -> D4
    )
    "not trigger" in {
      "after 2 repetitions" in {
        g must beSuccess.like { case game =>
          game.playMoveList(m take 5) must beSuccess.like { case game2 =>
            game2.situation.perpetualCheck must beFalse
            game2.situation.winner must beNone
          }
        }
      }
      "after 3 repetitions" in {
        g must beSuccess.like { case game =>
          game.playMoveList(m take 9) must beSuccess.like { case game2 =>
            game2.situation.perpetualCheck must beFalse
            game2.situation.winner must beNone
          }
        }
      }
    }
    "trigger" in {
      "after 4 repetitions" in {
        g must beSuccess.like { case game =>
          game.playMoveList(m) must beSuccess.like { case game2 =>
            game2.situation.perpetualCheck must beTrue
            game2.situation.winner must beSome.like { case color =>
              color == Gote
            }
          }
        }
      }
    }
  }
}
