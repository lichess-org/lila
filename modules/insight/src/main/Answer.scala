package lila.insight

import lila.game.{ Game, Pov }

case class Answer[X](
    question: Question[X],
    clusters: List[Cluster[X]],
    povs: List[Pov]
):
  def totalSize = clusters.map(_.size).sum

// a row per dimension value
case class Cluster[X](
    x: X,             // dimension value
    insight: Insight, // metric values
    size: Int,        // sample size
    insightIds: List[String]
):

  def gameIds = insightIds.map { Game.strToId(_) }

sealed trait Insight
object Insight:
  case class Single(point: Point)                                          extends Insight
  case class Stacked(points: List[(InsightMetric.MetricValueName, Point)]) extends Insight

opaque type Point = Double
object Point extends OpaqueDouble[Point]
