package lila.insight

case class Answer[X](
  question: Question[X],
  clusters: List[Cluster[X]])

// a row per dimension value
case class Cluster[X](
  x: X, // dimension value
  insight: Insight, // metric values
  size: Int // sample size
  )

sealed trait Insight
object Insight {
  case class Single(point: Point) extends Insight
  case class Stacked(points: Map[MetricValueName, Point]) extends Insight
}

case class MetricValueName(name: String)

case class Point(y: Double)
