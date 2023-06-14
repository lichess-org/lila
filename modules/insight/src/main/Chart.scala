package lila.insight

import play.api.i18n.Lang
import play.api.libs.json.*

import lila.common.LightUser
import lila.common.Json.given

case class Chart(
    question: JsonQuestion,
    xAxis: Chart.Xaxis,
    valueYaxis: Chart.Yaxis,
    sizeYaxis: Chart.Yaxis,
    series: List[Chart.Serie],
    sizeSerie: Chart.Serie,
    games: List[JsObject]
)

object Chart:

  case class Xaxis(
      name: String,
      categories: List[JsValue],
      dataType: String
  )

  case class Yaxis(
      name: String,
      dataType: String
  )

  case class Serie(
      name: String,
      dataType: String,
      stack: Option[String],
      data: List[Double]
  )

  def fromAnswer[X](
      getLightUser: LightUser.Getter
  )(answer: Answer[X])(using Lang, Executor): Fu[Chart] =

    import answer.*, question.*

    def xAxis =
      Xaxis(
        name = dimension.name,
        categories = clusters.map(_.x).map(InsightDimension.valueJson(dimension)),
        dataType = InsightDimension dataTypeOf dimension
      )

    def sizeSerie =
      Serie(
        name = metric.per.tellNumber,
        dataType = InsightMetric.DataType.Count.name,
        stack = none,
        data = clusters.map(_.size.toDouble)
      )

    def series =
      clusters
        .foldLeft(Map.empty[String, Serie]) { case (acc, cluster) =>
          cluster.insight match
            case Insight.Single(point) =>
              val key = metric.name
              acc.updated(
                key,
                acc.get(key) match {
                  case None =>
                    Serie(
                      name = metric.name,
                      dataType = metric.dataType.name,
                      stack = none,
                      data = List(point.value)
                    )
                  case Some(s) => s.copy(data = point.value :: s.data)
                }
              )
            case Insight.Stacked(points) =>
              points.foldLeft(acc) { case (acc, (metricValueName, point)) =>
                val key = s"${metric.name}/${metricValueName}"
                acc.updated(
                  key,
                  acc.get(key) match {
                    case None =>
                      Serie(
                        name = metricValueName.value,
                        dataType = metric.dataType.name,
                        stack = metric.name.some,
                        data = List(point.value)
                      )
                    case Some(s) => s.copy(data = point.value :: s.data)
                  }
                )
              }
        }
        .map { case (_, serie) =>
          serie.copy(data = serie.data.reverse)
        }
        .toList

    def sortedSeries =
      answer.clusters.headOption.fold(series) {
        _.insight match
          case Insight.Single(_)       => series
          case Insight.Stacked(points) => series.sortLike(points.map(_._1.value), _.name)
      }

    def gameUserJson(player: lila.game.Player): Fu[JsObject] =
      (player.userId so getLightUser) map { lu =>
        Json
          .obj("rating" -> player.rating)
          .add("name", lu.map(_.name))
          .add("title", lu.map(_.title))
      }

    povs.map { pov =>
      for {
        user1 <- gameUserJson(pov.player)
        user2 <- gameUserJson(pov.opponent)
      } yield Json.obj(
        "id"       -> pov.gameId,
        "fen"      -> (chess.format.Fen writeBoard pov.game.board),
        "color"    -> pov.player.color.name,
        "lastMove" -> (pov.game.lastMoveKeys | ""),
        "user1"    -> user1,
        "user2"    -> user2
      )
    }.parallel map { games =>
      Chart(
        question = JsonQuestion fromQuestion question,
        xAxis = xAxis,
        valueYaxis = Yaxis(metric.name, metric.dataType.name),
        sizeYaxis = Yaxis(metric.per.tellNumber, InsightMetric.DataType.Count.name),
        series = sortedSeries,
        sizeSerie = sizeSerie,
        games = games
      )
    }
