package lila.coach

import reactivemongo.bson._

import lila.db.Implicits._
import lila.user.User

final class CoachApi(coll: Coll) {

  import Storage._
  import coll.BatchCommands.AggregationFramework._
  import lila.coach.{ Dimension => D, Metric => M }

  def ask[X](
    question: Question[X],
    user: User): Fu[Answer[X]] = {
    val gameMatcher = combineDocs(question.filters.collect {
      case f if f.dimension.isInGame => f.matcher
    })
    val moveMatcher = combineDocs(question.filters.collect {
      case f if f.dimension.isInMove => f.matcher
    }).some.filterNot(_.isEmpty) map Match
    coll.aggregate(
      Match(selectUserId(user.id) ++ gameMatcher),
      makePipeline(question.dimension, question.metric, moveMatcher).flatten
    ).map { res =>
        val clusters = res.documents.flatMap { doc =>
          for {
            id <- doc.getAs[X]("_id")(question.dimension.bson)
            value <- doc.getAs[BSONNumberLike]("v")
            nb <- doc.getAs[Int]("nb")
          } yield Cluster(id,
            Point.Data(question.metric.name, value.toDouble),
            Point.Size(question.metric.position.tellNumber, nb))
        }
        Answer(
          question,
          clusters |>
            postProcess(question) |>
            postSort(question))
      }
  }

  private val unwindMoves = Unwind("moves").some
  private val sortNb = Sort(Descending("nb")).some
  private def limit(nb: Int) = Limit(nb).some
  private def count = "nb" -> SumValue(1)

  private def makePipeline(
    dimension: Dimension[_],
    metric: Metric,
    moveMatcher: Option[Match]): List[Option[PipelineOperator]] = (metric match {
    case M.MeanCpl => List(
      unwindMoves,
      moveMatcher,
      GroupField(dimension.dbKey)(
        "v" -> Avg("moves.c"),
        count
      ).some
    // sortNb,
    // limit(20)
    )
    case M.NbMoves => List(
      unwindMoves,
      moveMatcher,
      GroupField(dimension.dbKey)(
        "v" -> SumValue(1),
        count
      ).some
    // sortNb,
    // limit(20)
    )
    case M.Movetime => List(
      unwindMoves,
      moveMatcher,
      GroupField(dimension.dbKey)(
        "v" -> GroupFunction("$avg", BSONDocument("$divide" -> BSONArray("$moves.t", 10))),
        count
      ).some
    // sortNb,
    // limit(20)
    )
    case M.RatingDiff => List(
      GroupField(dimension.dbKey)(
        "v" -> Avg("ratingDiff"),
        count
      ).some
    // sortNb,
    // limit(20)
    )
    // case M.Result => List(
    //   GroupField(x.dbKey)(
    //     "nb" -> SumValue(1),
    //     "v" -> Avg("moves.c")
    //   ).some
    // )
    case _ => Nil
  }) ::: (dimension match {
    case D.Opening => List(sortNb, limit(12))
    case _         => Nil
  })

  private def postProcess[X](q: Question[X])(clusters: List[Cluster[X]]): List[Cluster[X]] =
    clusters

  private def postSort[X](q: Question[X])(clusters: List[Cluster[X]]): List[Cluster[X]] = q.dimension match {
    case D.Opening => clusters
    case _         => sortLike[Cluster[X], X](clusters, D.valuesOf(q.dimension), _.x)
  }

  private def sortLike[A, B](la: List[A], lb: List[B], f: A => B): List[A] = la.sortWith {
    case (x, y) => lb.indexOf(f(x)) < lb.indexOf(f(y))
  }
}
