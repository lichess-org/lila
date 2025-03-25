package lila.insight

import reactivemongo.api.bson.*

import lila.db.dsl.*

object AggregationClusters:

  def apply[X](question: Question[X], aggDocs: List[Bdoc]): List[Cluster[X]] =
    postSort(question):
      if InsightMetric.isStacked(question.metric) then stacked(question, aggDocs)
      else single(question, aggDocs)

  private def single[X](question: Question[X], aggDocs: List[Bdoc]): List[Cluster[X]] =
    for
      doc   <- aggDocs
      x     <- getId[X](doc)(question.dimension.bson)
      value <- doc.double("v")
      nb    <- doc.int("nb")
      ids = ~doc.getAsOpt[List[String]]("ids")
    yield Cluster(x, Insight.Single(Point(value)), nb, ids)

  private def getId[X](doc: Bdoc)(reader: BSONReader[X]): Option[X] =
    doc.get("_id").flatMap(reader.readOpt)

  private case class StackEntry(metric: BSONValue, v: BSONNumberLike)
  private given BSONDocumentReader[StackEntry] = Macros.reader

  private def stacked[X](question: Question[X], aggDocs: List[Bdoc]): List[Cluster[X]] =
    for
      doc <- aggDocs
      metricValues = InsightMetric.valuesOf(question.metric)
      x     <- getId[X](doc)(question.dimension.bson)
      stack <- doc.getAsOpt[List[StackEntry]]("stack")
      points = metricValues.map { case InsightMetric.MetricValue(id, name) =>
        name -> Point(stack.find(_.metric == id).so(_.v.toDouble.get))
      }
      total = stack.map(_.v.toInt.get).sum
      percents =
        if total == 0 then points
        else
          points.map { case (n, p) =>
            n -> Point(100 * p.value / total)
          }
      ids = ~doc.getAsOpt[List[String]]("ids")
    yield Cluster(x, Insight.Stacked(percents.toList), total, ids)

  private def postSort[X](q: Question[X])(clusters: List[Cluster[X]]): List[Cluster[X]] =
    q.dimension match
      case InsightDimension.OpeningFamily    => clusters
      case InsightDimension.OpeningVariation => clusters
      case _                                 => clusters.sortLike(InsightDimension.valuesOf(q.dimension), _.x)
