package lila.system

import lila.chess.{ Game, Board }
import model._
import ai._
import scalaz.Success

trait AiTest extends SystemTest {

  def ai: Ai
  def name: String

  "the %s AI" format name should {
    "play the first move" in {
      val dbGame = newDbGame
      ai(dbGame).unsafePerformIO must beSuccess.like {
        case (game, move) ⇒ game.board must_!= Board()
      }
    }
    "play 10 moves" in {
      val dbGame = (1 to 10).foldLeft(newDbGame) { (dbg, _) ⇒
        ai(dbg).unsafePerformIO match {
          case Success((game, move)) ⇒ dbg.update(game, move)
          case _                     ⇒ dbg
        }
      }
      dbGame.turns must_== 10
    }
  }
}
