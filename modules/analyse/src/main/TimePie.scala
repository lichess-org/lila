package lila.analyse

import play.api.libs.json.Json

import lila.game.Pov

final class TimePie(val pov: Pov) {

  def columns = Json stringify {
    Json toJson List(
      "string" :: "Time in seconds" :: Nil,
      "number" :: "Number of moves" :: Nil)
  }

  def rows = Json stringify {

    import pov._

    val index = pov.color.fold(0, 1)
    val moveTimes = game.moveTimes.zipWithIndex.view.filter(_._2 % 2 == index).map(_._1)

    def nbMoves(range: Range) = moveTimes count range.contains

    def nameRange(range: Range) = {
      val min = range.min / 10
      val max = range.max / 10
      if (min == max) (min < 2).fold("%d second", "%d seconds") format min
      else "%d to %d seconds".format(min, max)
    }

    Json toJson {
      TimePie.ranges zip (TimePie.ranges map nbMoves) collect {
        case (range, nb) if nb > 0 ⇒ Json.arr(nameRange(range), nb)
      }
    }
  }
}

object TimePie {

  private[TimePie] val steps = (0 to 10 by 10) ++ (20 to 60 by 20) ++ (100 to 200 by 50) ++ (200 to 600 by 100)
  private[TimePie] val ranges = steps zip (steps drop 1) map { case (a, b) ⇒ Range(a, b) }
}
