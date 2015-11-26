package lila.coach

import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
import reactivemongo.bson._

import lila.db.Implicits._
import lila.user.User

final class CoachApi(
    coll: Coll,
    pipeline: AggregationPipeline) {

  import lila.coach.{ Dimension => D, Metric => M }

  def ask[X](
    question: Question[X],
    user: User): Fu[Answer[X]] = {
    val operators = pipeline(question, user.id)
    coll.aggregate(operators.head, operators.tail).map { res =>
      val clusters = res.documents.flatMap { doc =>
        for {
          id <- doc.getAs[X]("_id")(question.dimension.bson)
          value <- doc.getAs[BSONNumberLike]("v")
          nb <- doc.getAs[Int]("n")
        } yield Cluster(id,
          Point.Data(question.metric.name, value.toDouble),
          Point.Size(question.metric.position.tellNumber, nb))
      }
      Answer(
        question,
        clusters |> postSort(question)
      )
    }
  }

  private def postSort[X](q: Question[X])(clusters: List[Cluster[X]]): List[Cluster[X]] = q.dimension match {
    case D.Opening => clusters
    case _         => sortLike[Cluster[X], X](clusters, D.valuesOf(q.dimension), _.x)
  }

  private def sortLike[A, B](la: List[A], lb: List[B], f: A => B): List[A] = la.sortWith {
    case (x, y) => lb.indexOf(f(x)) < lb.indexOf(f(y))
  }
}
