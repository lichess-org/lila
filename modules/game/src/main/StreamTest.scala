package lila.game

import BSONHandlers._
import lila.db.dsl._
import play.api.libs.iteratee._
import reactivemongo.play.iteratees.cursorProducer

object StreamTest {

  val max = 100000

  def start = {
    GameRepo.coll
      .find($empty)
      .cursor[Game]()
      .enumerator(max) |>>>
      Iteratee.fold[Game, Int](0) {
        case (nb, g) =>
          if (nb % 10000 == 0) println(nb)
          nb + 1
      } map (_.toString)
  }.chronometer.pp("##################")
}
