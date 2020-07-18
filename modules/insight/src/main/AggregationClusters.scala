package lila.insight

import reactivemongo.api.bson._
import lila.db.dsl._

object AggregationClusters {

  def apply[X](question: Question[X], aggDocs: List[Bdoc]): List[Cluster[X]] =
    postSort(question) {
      if (Metric isStacked question.metric) stacked(question, aggDocs)
      else single(question, aggDocs)
    }

  private def single[X](question: Question[X], aggDocs: List[Bdoc]): List[Cluster[X]] =
    for {
      doc   <- aggDocs
      x     <- getId[X](doc)(question.dimension.bson)
      value <- doc.double("v")
      nb    <- doc.int("nb")
      ids   <- doc.getAsOpt[List[String]]("ids")
    } yield Cluster(x, Insight.Single(Point(value)), nb, ids)

  private def getId[X](doc: Bdoc)(reader: BSONReader[X]): Option[X] =
    doc.get("_id") flatMap reader.readOpt

  private case class StackEntry(metric: BSONValue, v: BSONNumberLike)
  implicit private val StackEntryBSONReader = Macros.reader[StackEntry]

  private def stacked[X](question: Question[X], aggDocs: List[Bdoc]): List[Cluster[X]] =
    for {
      doc <- aggDocs
      metricValues = Metric valuesOf question.metric
      x     <- getId[X](doc)(question.dimension.bson)
      stack <- doc.getAsOpt[List[StackEntry]]("stack")
      points = metricValues.map {
        case Metric.MetricValue(id, name) =>
          name -> Point(stack.find(_.metric == id).??(_.v.toDouble.get))
      }
      total = stack.map(_.v.toInt.get).sum
      percents =
        if (total == 0) points
        else
          points.map {
            case (n, p) => n -> Point(100 * p.y / total)
          }
      ids <- doc.getAsOpt[List[String]]("ids")
    } yield Cluster(x, Insight.Stacked(percents), total, ids)

  private def postSort[X](q: Question[X])(clusters: List[Cluster[X]]): List[Cluster[X]] =
    q.dimension match {
      case Dimension.Opening => clusters
      case _                 => clusters.sortLike(Dimension.valuesOf(q.dimension), _.x)
    }
}
