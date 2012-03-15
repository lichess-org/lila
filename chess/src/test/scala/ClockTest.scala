package lila.chess

import Pos._

class ClockTest extends ChessTest {

  "play with a clock" in {
    val clock = PausedClock(5 * 60 * 1000, 0)
    val game = Game() withClock clock
    "new game" in {
      game.clock must beSome.like {
        case c ⇒ c.color must_== White
      }
    }
    "one move played" in {
      game.playMoves(E2 -> E4) must beSuccess.like {
        case g ⇒ g.clock must beSome.like {
          case c ⇒ c.color must_== Black
        }
      }
    }
  }
}
