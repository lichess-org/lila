package lila.chess

import Pos._
import format.Visual.addNewLines

class GameTest extends ChessTest {

  "capture a piece" should {
    "add it to the dead pieces" in {
      val game = Game().playMoves(
        E2 -> E4,
        D7 -> D5,
        E4 -> D5)
      game must beSuccess.like {
        case g ⇒ g.deads must haveTheSameElementsAs(List(D5 -> Black.pawn))
      }
    }
  }
  "recapture a piece" should {
    "add both to the dead pieces" in {
      val game = Game("""
bq
R""").playMoves(
  A1 -> A2,
  B2 -> A2)
      game must beSuccess.like {
        case g ⇒ g.deads must haveTheSameElementsAs(List(
          A2 -> Black.bishop,
          A2 -> White.rook))
      }
    }
  }
}
