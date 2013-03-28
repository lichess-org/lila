package lila.user

import scala.math.round
import play.api.libs.json.Json
import org.joda.time.DateTime
import org.joda.time.format.{ DateTimeFormat, DateTimeFormatter }

import play.api.libs.concurrent.Execution.Implicits._

final class EloChart(rawElos: List[(Int, Int, Option[Int])]) {

  private val points = 100
  private val eloMedian = 30
  private val opMedian = 20

  private val formatter: DateTimeFormatter = DateTimeFormat forPattern "dd/MM/yy"

  def columns = EloChart.columns

  def rows = Json toJson {
    withMedian(reduce(rawElos)) map {
      case (ts, elo, op, med) ⇒ Json toJson List(
        Json toJson date(ts), 
        Json toJson elo, 
        Json toJson op, 
        Json toJson med)
    }
  }

  private def reduce(elos: List[(Int, Int, Option[Int])]) = {
    val size = elos.size
    (size <= points).fold(elos, {
      val factor = size.toFloat / points
      ((0 until points).toList map { i ⇒
        elos(round(i * factor))
      }) :+ elos.last
    })
  }

  private def withMedian(elos: List[(Int, Int, Option[Int])]) = {
    val eloValues = elos map (_._2)
    val opValues = elos map (_._3)
    elos.zipWithIndex map {
      case ((ts, elo, op), i) ⇒ (ts, elo,
        opValues.slice(i - opMedian, i + opMedian).flatten |> { vs ⇒
          vs.nonEmpty option (vs.sum / vs.size)
        },
        eloValues.slice(i - eloMedian, i + eloMedian) |> { vs ⇒ vs.sum / vs.size }
      )
    }
  }

  // ts is in seconds
  def date(ts: Long): String = formatter print new DateTime(ts * 1000)
}

object EloChart {

  val columns = Json.arr(
    Json.arr("string", "Game"),
    Json.arr("number", "Elo"),
    Json.arr("number", "Opponent Elo"),
    Json.arr("number", "Average")
  )

  def apply(user: User): Fu[Option[EloChart]] =
    HistoryRepo userElos user.username map { elos ⇒
      (elos.size > 1) option {
        new EloChart((user.createdAt.getSeconds.toInt, Users.STARTING_ELO, None) :: elos.toList)
      }
    }
}
