package lila.insight

import play.api.libs.json._
import play.api.i18n.Lang

import lila.common.LightUser

case class Chart(
    question: JsonQuestion,
    xAxis: Chart.Xaxis,
    valueYaxis: Chart.Yaxis,
    sizeYaxis: Chart.Yaxis,
    series: List[Chart.Serie],
    sizeSerie: Chart.Serie,
    games: List[JsObject]
)

object Chart {

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

  def fromAnswer[X](getLightUser: LightUser.GetterSync)(answer: Answer[X])(implicit lang: Lang): Chart = {

    import answer._, question._

    def gameUserJson(player: lila.game.Player): JsObject = {
      val light = player.userId flatMap getLightUser
      Json
        .obj(
          "name"   -> light.map(_.name),
          "title"  -> light.map(_.title),
          "rating" -> player.rating
        )
        .noNull
    }

    def games =
      povs.map { pov =>
        Json.obj(
          "id"       -> pov.gameId,
          "fen"      -> (chess.format.Forsyth exportBoard pov.game.board),
          "color"    -> pov.player.color.name,
          "lastMove" -> ~pov.game.lastMoveKeys,
          "user1"    -> gameUserJson(pov.player),
          "user2"    -> gameUserJson(pov.opponent)
        )
      }

    def xAxis(implicit lang: Lang) =
      Xaxis(
        name = dimension.name,
        categories = clusters.map(_.x).map(Dimension.valueJson(dimension)),
        dataType = Dimension dataTypeOf dimension
      )

    def sizeSerie =
      Serie(
        name = metric.per.tellNumber,
        dataType = Metric.DataType.Count.name,
        stack = none,
        data = clusters.map(_.size.toDouble)
      )

    def series =
      clusters
        .foldLeft(Map.empty[String, Serie]) {
          case (acc, cluster) =>
            cluster.insight match {
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
                        data = List(point.y)
                      )
                    case Some(s) => s.copy(data = point.y :: s.data)
                  }
                )
              case Insight.Stacked(points) =>
                points.foldLeft(acc) {
                  case (acc, (metricValueName, point)) =>
                    val key = s"${metric.name}/${metricValueName.name}"
                    acc.updated(
                      key,
                      acc.get(key) match {
                        case None =>
                          Serie(
                            name = metricValueName.name,
                            dataType = metric.dataType.name,
                            stack = metric.name.some,
                            data = List(point.y)
                          )
                        case Some(s) => s.copy(data = point.y :: s.data)
                      }
                    )
                }
            }
        }
        .map {
          case (_, serie) => serie.copy(data = serie.data.reverse)
        }
        .toList

    def sortedSeries =
      answer.clusters.headOption.fold(series) {
        _.insight match {
          case Insight.Single(_)       => series
          case Insight.Stacked(points) => series.sortLike(points.map(_._1.name), _.name)
        }
      }

    Chart(
      question = JsonQuestion fromQuestion question,
      xAxis = xAxis,
      valueYaxis = Yaxis(metric.name, metric.dataType.name),
      sizeYaxis = Yaxis(metric.per.tellNumber, Metric.DataType.Count.name),
      series = sortedSeries,
      sizeSerie = sizeSerie,
      games = games
    )
  }
}
