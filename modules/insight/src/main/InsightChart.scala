package lila.insight

import play.api.libs.json.*

import lila.common.Json.given
import lila.core.LightUser
import lila.core.i18n.Translate

case class InsightChart(
    question: JsonQuestion,
    xAxis: InsightChart.Xaxis,
    valueYaxis: InsightChart.Yaxis,
    sizeYaxis: InsightChart.Yaxis,
    series: List[InsightChart.Serie],
    sizeSerie: InsightChart.Serie,
    games: List[JsObject]
)

object InsightChart:

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
  )(answer: Answer[X])(using Translate, Executor): Fu[InsightChart] =

    import answer.*, question.*

    def xAxis =
      Xaxis(
        name = dimension.name,
        categories = clusters.map(_.x).map(InsightDimension.valueJson(dimension)),
        dataType = InsightDimension.dataTypeOf(dimension)
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
                acc.get(key) match
                  case None =>
                    Serie(
                      name = metric.name,
                      dataType = metric.dataType.name,
                      stack = none,
                      data = List(point.value)
                    )
                  case Some(s) => s.copy(data = point.value :: s.data)
              )
            case Insight.Stacked(points) =>
              points.foldLeft(acc) { case (acc, (metricValueName, point)) =>
                val key = s"${metric.name}/${metricValueName}"
                acc.updated(
                  key,
                  acc.get(key) match
                    case None =>
                      Serie(
                        name = metricValueName.value,
                        dataType = metric.dataType.name,
                        stack = metric.name.some,
                        data = List(point.value)
                      )
                    case Some(s) => s.copy(data = point.value :: s.data)
                )
              }
        }
        .map { case (_, serie) =>
          serie.copy(data = serie.data.reverse)
        }
        .toList

    def sortedSeries =
      answer.clusters.headOption.fold(series):
        _.insight match
          case Insight.Single(_) => series
          case Insight.Stacked(points) => series.sortLike(points.map(_._1.value), _.name)

    def gameUserJson(player: lila.core.game.Player): Fu[JsObject] =
      player.userId.so(getLightUser).map { lu =>
        Json
          .obj("rating" -> player.rating)
          .add("name", lu.map(_.name))
          .add("title", lu.map(_.title))
      }

    povs
      .sequentially { pov =>
        for
          user1 <- gameUserJson(pov.player)
          user2 <- gameUserJson(pov.opponent)
        yield Json.obj(
          "id" -> pov.gameId,
          "fen" -> (chess.format.Fen.writeBoard(pov.game.position)),
          "color" -> pov.player.color.name,
          "lastMove" -> (pov.game.lastMoveKeys | ""),
          "user1" -> user1,
          "user2" -> user2
        )
      }
      .map { games =>
        InsightChart(
          question = JsonQuestion.fromQuestion(question),
          xAxis = xAxis,
          valueYaxis = Yaxis(metric.name, metric.dataType.name),
          sizeYaxis = Yaxis(metric.per.tellNumber, InsightMetric.DataType.Count.name),
          series = sortedSeries,
          sizeSerie = sizeSerie,
          games = games
        )
      }
