package lila.system

import lila.chess.{ Game, Board }
import model._
import ai._
import scalaz.Success

class StupidAiTest extends SystemTest {

  val ai = new StupidAi

  "the stupid AI" should {
    "play the first move" in {
      val dbGame = newDbGame
      ai(dbGame) must beSuccess.like {
        case (game, move) ⇒ game.board must_!= Board()
      }
    }
    "play 20 moves" in {
      val dbGame = (1 to 20).foldLeft(newDbGame) { (dbg, _) ⇒
        ai(dbg) match {
          case Success((game, move)) ⇒ dbg.update(game, move)
          case _                     ⇒ dbg
        }
      }
      dbGame.turns must_== 20
    }
  }
}
