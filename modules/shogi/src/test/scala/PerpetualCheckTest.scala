package shogi

import Pos._

class PerpetualCheckTest extends ShogiTest {

  "Perpetual check" should {
    val g = makeGame.playMoves(
      SQ5G -> SQ5F,
      SQ5C -> SQ5D,
      SQ2H -> SQ5H,
      SQ5A -> SQ5B,
      SQ5F -> SQ5E,
      SQ5B -> SQ5C,
      SQ5H -> SQ5F,
      SQ5C -> SQ6D
    )
    //             1st                                     2nd                                      3rd
    val m = List(
      SQ5F -> SQ6F,
      SQ6D -> SQ7D,
      SQ6F -> SQ7F,
      SQ7D -> SQ6D,
      SQ7F -> SQ6F,
      SQ6D -> SQ7D,
      SQ6F -> SQ7F,
      SQ7D -> SQ6D,
      SQ7F -> SQ6F,
      SQ6D -> SQ7D,
      SQ6F -> SQ7F,
      SQ7D -> SQ6D,
      SQ7F -> SQ6F
    )
    "not trigger" in {
      "after 2 repetitions" in {
        g must beValid.like { case game =>
          game.playMoveList(m take 5) must beValid.like { case game2 =>
            game2.situation.perpetualCheck must beFalse
            game2.situation.winner must beNone
          }
        }
      }
      "after 3 repetitions" in {
        g must beValid.like { case game =>
          game.playMoveList(m take 9) must beValid.like { case game2 =>
            game2.situation.perpetualCheck must beFalse
            game2.situation.winner must beNone
          }
        }
      }
    }
    "trigger" in {
      "after 4 repetitions" in {
        g must beValid.like { case game =>
          game.playMoveList(m) must beValid.like { case game2 =>
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
