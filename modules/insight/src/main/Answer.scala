package lila.insight

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

  def gameIds = insightIds.map(GameId.take)

enum Insight:
  case Single(point: Point)
  case Stacked(points: List[(InsightMetric.MetricValueName, Point)])

opaque type Point = Double
object Point extends OpaqueDouble[Point]
