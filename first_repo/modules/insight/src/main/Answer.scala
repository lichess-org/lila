package lila.insight

import lila.game.{ Game, Pov }

case class Answer[X](
    question: Question[X],
    clusters: List[Cluster[X]],
    povs: List[Pov]
)

// a row per dimension value
case class Cluster[X](
    x: X,             // dimension value
    insight: Insight, // metric values
    size: Int,        // sample size
    insightIds: List[String]
) {

  def gameIds = insightIds.map(_ take Game.gameIdSize)
}

sealed trait Insight
object Insight {
  case class Single(point: Point)                                   extends Insight
  case class Stacked(points: List[(Metric.MetricValueName, Point)]) extends Insight
}

case class Point(y: Double) extends AnyVal
