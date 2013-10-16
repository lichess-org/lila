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

  private[user] def apply(user: User): Fu[Option[EloChart]] = {
    HistoryRepo userElos user.id map { elos ⇒
      (elos.size > 1) option {
        val rawElos = (user.createdAt.getSeconds.toInt, User.STARTING_ELO, None) :: elos.toList

        val points = 100
        val eloMedian = 30
        val opMedian = 20

        def reduce(elos: List[(Int, Int, Option[Int])]) = {
          val size = elos.size
          (size <= points).fold(elos, {
            val factor = size.toFloat / points
            ((0 until points).toList map { i ⇒
              elos(round(i * factor))
            }) :+ elos.last
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

        EloChart {
          Json stringify {
            Json toJson {
              withMedian(reduce(rawElos)) map {
                case (ts, elo, op, med) ⇒ Json.arr(ts, elo, op, med)
              }
            }
          }
        }
      }
    }
  }
}
