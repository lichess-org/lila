package lila.user

import scala.math.round

import play.api.libs.json.Json

case class EloChart(rows: String) {

  def columns: String = EloChart.columns
}

object EloChart {

  private val columns = Json stringify {
    Json.arr(
      Json.arr("string", "Game"),
      Json.arr("number", "Elo"),
      Json.arr("number", "Opponent Elo"),
      Json.arr("number", "Average")
    )
  }

  private[user] def apply(user: User): Fu[Option[String]] = {
    HistoryRepo userElos user.id map { elos ⇒
      val size = elos.size
      (size > 1) option {
        val rawElos = (size > 100).fold(
          (size > 200).fold(
            elos.toList drop 20,
            elos.toList drop 10
          ),
          (user.createdAt.getSeconds.toInt, User.STARTING_ELO, None) :: elos.toList
        )

        val points = 100
        val eloMedian = 30
        val opMedian = 20

        def reduce(elos: List[(Int, Int, Option[Int])]) = {
          val indexed = elos.toIndexedSeq
          val size = indexed.size
          (size <= points).fold(elos, {
            val factor = size.toFloat / points
            ((0 until points).toList map { i ⇒
              indexed(round(i * factor))
            }) :+ indexed.last
          })
        }

        def withMedian(elos: List[(Int, Int, Option[Int])]) = {
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

        Json stringify {
          val values = withMedian(reduce(rawElos))
          Json.obj(
            "ts" -> (values map (_._1)),
            "elo" -> (values map (_._2)),
            "op" -> (values map (_._3)),
            "avg" -> (values map (_._4)))
        }
      }
    }
  }
}
