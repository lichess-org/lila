package lila
package analyse

import game.{ DbPlayer, Pov }

import com.codahale.jerkson.Json

final class TimePie(val pov: Pov) {

  def columns = Json generate List(
    "string" :: "Move time" :: Nil,
    "number" :: "Moves" :: Nil)

  def rows = Json generate {

    import pov._

    val steps = (0 to 5) ++ (6 to 12 by 2) ++ (15 to 30 by 3) ++ (35 to 60 by 5)

    val ranges = steps zip (steps drop 1) map { case (a, b) ⇒ Range(a, b) }

    val moveTimes = player.moveTimeList

    def nbMoves(range: Range) = moveTimes count range.contains

    def nameRange(range: Range) = {
      import range._
      if (min == max) (min < 2).fold("%d second", "%d seconds") format min
      else "%d to %d seconds".format(min, max)
    }

    ranges zip (ranges map nbMoves) collect {
      case (range, nb) if nb > 0 ⇒ List(nameRange(range), nb)
    }
  }
}
