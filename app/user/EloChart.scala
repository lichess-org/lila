package lila
package user

import scala.math.round
import scalaz.effects._
import com.codahale.jerkson.Json
import org.joda.time.DateTime
import org.joda.time.format.{ DateTimeFormat, DateTimeFormatter }

final class EloChart(elos: List[(Int, Int)]) {

  private val points = 100
  private val median = 30

  private val formatter: DateTimeFormatter =
    DateTimeFormat forPattern "dd/MM/yy"

  def columns = EloChart.columns

  def rows = Json generate {
    withMedian(reduce(elos)) map {
      case (ts, elo, med) ⇒ List(date(ts), elo, med)
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

  private def withMedian(elos: List[(Int, Int)]) = {
    val values = elos map (_._2)
    elos.zipWithIndex map {
      case ((ts, elo), i) ⇒ (ts, elo,
        values.slice(i - median, i + median) |> { vs ⇒ vs.sum / vs.size }
      )
    }
  }

  // ts is in seconds
  def date(ts: Long): String = formatter print new DateTime(ts * 1000)
}

object EloChart {

  val columns = Json generate List(
    "string" :: "Game" :: Nil,
    "number" :: "Elo" :: Nil,
    "number" :: "Average" :: Nil)

  def apply(historyRepo: HistoryRepo)(user: User): IO[Option[EloChart]] =
    historyRepo userElos user.username map { elos ⇒
      (elos.size > 1) option { new EloChart(elos) }
    }
}
