package lila.game

import BSONHandlers._
import lila.db.dsl._
import play.api.libs.iteratee._
import reactivemongo.play.iteratees.cursorProducer

// old lazyload = 25 micros
// old + toChess = 55 micros
object StreamTest {

  val max = 100000

  def start = {
    GameRepo.coll
      .find($empty)
      .cursor[Game]()
      .enumerator(max) |>>>
      Iteratee.fold[Game, Int](0) {
        case (nb, g) =>
          g.toChess
          if (nb % 10000 == 0) println(nb)
          nb + 1
      }
  }.chronometer.lap.map { lap =>
    println(s"""${lap.result} games in ${lap.millis}ms, ${lap.micros / lap.result} micros per game""")
    lap.result
  } map (_.toString)
}
