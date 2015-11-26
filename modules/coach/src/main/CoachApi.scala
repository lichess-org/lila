package lila.coach

import reactivemongo.bson._

import lila.db.Implicits._
import lila.user.User

final class CoachApi(coll: Coll) {

  import Storage._
  import coll.BatchCommands.AggregationFramework._

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
      makePipeline(question.xAxis, question.yAxis, moveMatcher).flatten
    ).map { res =>
        val clusters = res.documents.flatMap { doc =>
          for {
            id <- doc.getAs[X]("_id")(question.xAxis.bson)
            value <- doc.getAs[Double]("v")
            nb <- doc.getAs[Int]("nb")
          } yield Cluster(id,
            Point.Data(question.yAxis.name, value),
            Point.Size(nb))
        }
        Answer(question, clusters)
      }
  }

  private val unwindMoves = Unwind("moves").some
  private val sortNb = Sort(Descending("nb")).some
  private def limit(nb: Int) = Limit(nb).some

  private def makePipeline(
    x: Dimension[_],
    y: Metric,
    moveMatcher: Option[Match]): List[Option[PipelineOperator]] = y match {
    case Metric.MeanCpl => List(
      unwindMoves,
      moveMatcher,
      GroupField(x.dbKey)(
        "v" -> Avg("moves.c"),
        "nb" -> SumValue(1)
      ).some,
      sortNb,
      limit(20)
    )
    case Metric.NbMoves => List(
      unwindMoves,
      moveMatcher,
      GroupField(x.dbKey)("nb" -> SumValue(1)).some,
      sortNb,
      limit(20)
    )
    case Metric.Movetime => List(
      unwindMoves,
      moveMatcher,
      GroupField(x.dbKey)(
        "v" -> Avg("moves.t"),
        "nb" -> SumValue(1)
      ).some,
      sortNb,
      limit(20)
    )
    case Metric.RatingDiff => List(
      GroupField(x.dbKey)(
        "v" -> SumField("ratingDiff"),
        "nb" -> SumValue(1)
      ).some,
      sortNb,
      limit(20)
    )
    // case Metric.Result => List(
    //   GroupField(x.dbKey)(
    //     "nb" -> SumValue(1),
    //     "v" -> Avg("moves.c")
    //   ).some
    // )
    case _ => Nil
  }
}
