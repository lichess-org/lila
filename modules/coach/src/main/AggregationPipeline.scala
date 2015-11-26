package lila.coach

import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
import reactivemongo.bson._
import scalaz.NonEmptyList

import lila.db.Implicits._

private final class AggregationPipeline {

  import lila.coach.{ Dimension => D, Metric => M }
  import Storage._

  private val unwindMoves = Unwind("moves").some
  private val sortNb = Sort(Descending("nb")).some
  private def limit(nb: Int) = Limit(nb).some
  private def group(d: Dimension[_], f: GroupFunction) = GroupField(d.dbKey)(
    "v" -> f,
    "nb" -> SumValue(1)
  ).some

  def apply(question: Question[_], userId: String): NonEmptyList[PipelineOperator] = {
    import question.dimension
    val gameMatcher = combineDocs(question.filters.collect {
      case f if f.dimension.isInGame => f.matcher
    })
    val moveMatcher = combineDocs(question.filters.collect {
      case f if f.dimension.isInMove => f.matcher
    }).some.filterNot(_.isEmpty) map Match

    NonEmptyList.nel[PipelineOperator](
      Match(
        selectUserId(userId) ++
          gameMatcher ++
          Metric.requiresAnalysis(question.metric).??(BSONDocument("analysed" -> true))
      ),
      ((question.metric match {
        case M.MeanCpl => List(
          unwindMoves,
          moveMatcher,
          group(dimension, Avg("moves.c"))
        )
        case M.NbMoves => List(
          unwindMoves,
          moveMatcher,
          group(dimension, SumValue(1))
        )
        case M.Movetime => List(
          unwindMoves,
          moveMatcher,
          group(dimension, GroupFunction("$avg", BSONDocument("$divide" -> BSONArray("$moves.t", 10))))
        )
        case M.RatingDiff => List(
          group(dimension, Avg("ratingDiff"))
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
      })).flatten
    )
  }
}
