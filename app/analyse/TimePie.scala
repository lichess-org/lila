package lila
package analyse

import game.{ DbPlayer, Pov }

import play.api.libs.json.Json

final class TimePie(val pov: Pov) {

  def columns = Json stringify {
    Json toJson List(
      "string" :: "Time in seconds" :: Nil,
      "number" :: "Number of moves" :: Nil)
  }

  def rows = Json stringify {

    import pov._

    val steps = (0 to 1) ++ (2 to 6 by 2) ++ (10 to 20 by 5) ++ (20 to 60 by 10)

    val ranges = steps zip (steps drop 1) map { case (a, b) ⇒ Range(a, b) }

    val moveTimes = player.moveTimeList

    def nbMoves(range: Range) = moveTimes count range.contains

    def nameRange(range: Range) = {
      import range._
      if (min == max) (min < 2).fold("%d second", "%d seconds") format min
      else "%d to %d seconds".format(min, max)
    }

    Json toJson {
      ranges zip (ranges map nbMoves) collect {
        case (range, nb) if nb > 0 ⇒ List(nameRange(range), nb)
      }
    }
  }
}
