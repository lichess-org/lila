package lila.insight

import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
import reactivemongo.bson._

object AggregationClusters {

  def apply[X](question: Question[X], res: AggregationResult): List[Cluster[X]] =
    postSort(question) {
      if (Metric isStacked question.metric) stacked(question, res)
      else single(question, res)
    }

  private def single[X](question: Question[X], res: AggregationResult): List[Cluster[X]] =
    res.documents.flatMap { doc =>
      for {
        x <- doc.getAs[X]("_id")(question.dimension.bson)
        value <- doc.getAs[BSONNumberLike]("v")
        nb <- doc.getAs[Int]("nb")
      } yield Cluster(x, Insight.Single(Point(value.toDouble)), nb)
    }

  private case class StackEntry(metricKey: String, value: BSONNumberLike)
  private implicit val StackEntryBSONReader = Macros.reader[StackEntry]

  private def stacked[X](question: Question[X], res: AggregationResult): List[Cluster[X]] =
    res.documents.flatMap { doc =>
      for {
        x <- doc.getAs[X]("_id")(question.dimension.bson)
        stack <- doc.getAs[List[StackEntry]]("stack")
        points = stack.map {
          case StackEntry(key, value) => MetricValueName(key) -> Point(value.toDouble)
        }.toMap
        nb = stack.map(_.value.toInt).sum
      } yield Cluster(x, Insight.Stacked(points), nb)
    }

  private def postSort[X](q: Question[X])(clusters: List[Cluster[X]]): List[Cluster[X]] = q.dimension match {
    case Dimension.Opening => clusters
    case _                 => sortLike[Cluster[X], X](clusters, Dimension.valuesOf(q.dimension), _.x)
  }

  private def sortLike[A, B](la: List[A], lb: List[B], f: A => B): List[A] = la.sortWith {
    case (x, y) => lb.indexOf(f(x)) < lb.indexOf(f(y))
  }
}
