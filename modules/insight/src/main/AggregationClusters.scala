package lila.insight

import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
import reactivemongo.bson._
import lila.db.dsl._

object AggregationClusters {

  def apply[X](question: Question[X], aggDocs: List[Bdoc]): List[Cluster[X]] =
    postSort(question) {
      if (Metric isStacked question.metric) stacked(question, aggDocs)
      else single(question, aggDocs)
    }

  private def single[X](question: Question[X], aggDocs: List[Bdoc]): List[Cluster[X]] =
    aggDocs flatMap { doc =>
      for {
        x <- doc.getAs[X]("_id")(question.dimension.bson)
        value <- doc.getAs[BSONNumberLike]("v")
        nb <- doc.getAs[Int]("nb")
        ids <- doc.getAs[List[String]]("ids")
      } yield Cluster(x, Insight.Single(Point(value.toDouble)), nb, ids)
    }

  private case class StackEntry(metric: BSONValue, v: BSONNumberLike)
  private implicit val StackEntryBSONReader = Macros.reader[StackEntry]

  private def stacked[X](question: Question[X], aggDocs: List[Bdoc]): List[Cluster[X]] =
    aggDocs flatMap { doc =>
      val metricValues = Metric valuesOf question.metric
      // println(lila.db.BSON debug doc)
      for {
        x <- doc.getAs[X]("_id")(question.dimension.bson)
        stack <- doc.getAs[List[StackEntry]]("stack")
        points = metricValues.map {
          case Metric.MetricValue(id, name) =>
            name -> Point(stack.find(_.metric == id).??(_.v.toDouble))
        }
        total = stack.map(_.v.toInt).sum
        percents = if (total == 0) points
        else points.map {
          case (n, p) => n -> Point(100 * p.y / total)
        }
        ids <- doc.getAs[List[String]]("ids")
      } yield Cluster(x, Insight.Stacked(percents), total, ids)
    }

  private def postSort[X](q: Question[X])(clusters: List[Cluster[X]]): List[Cluster[X]] = q.dimension match {
    case Dimension.Opening => clusters
    case _ => clusters.sortLike(Dimension.valuesOf(q.dimension), _.x)
  }
}
