package lila
package user

import scala.math.round
import scalaz.effects._
import com.codahale.jerkson.Json

final class EloChart(elos: List[(Int, Int)]) {

  private val points = 100

  def columns = EloChart.columns

  val rows = Json generate {
    reduce(elos) map {
      case (ts, elo) ⇒ ts :: elo :: Nil
    }
  }

  private def reduce(elos: List[(Int, Int)]) = {
    val size = elos.size
    (size <= points).fold(elos, {
      val factor = size.toFloat / points
      ((0 until points).toList map { i ⇒
        elos(round(i * factor))
      }) :+ elos.last
    })
  }
}

object EloChart {

  val columns = Json generate List(
    "number" :: "Game" :: Nil,
    "number" :: "Elo" :: Nil)

  def apply(historyRepo: HistoryRepo)(user: User): IO[Option[EloChart]] =
    historyRepo userElos user.username map { elos ⇒
      (elos.size > 1) option { new EloChart(elos) }
    }
}
