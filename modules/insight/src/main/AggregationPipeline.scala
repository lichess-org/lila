package lila.insight

import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
import reactivemongo.bson._
import scalaz.NonEmptyList

import lila.db.Implicits._

private final class AggregationPipeline {

  import lila.insight.{ Dimension => D, Metric => M }
  import Storage._

  private val sampleGames = Sample(10 * 1000)
  private val sampleMoves = Sample(200 * 1000).some
  private val unwindMoves = Unwind("moves").some
  private val sortNb = Sort(Descending("nb")).some
  private def limit(nb: Int) = Limit(nb).some
  private def group(d: Dimension[_], f: GroupFunction) = GroupField(d.dbKey)(
    "v" -> f,
    "nb" -> SumValue(1)
  ).some
  private val regroupStacked = GroupField("_id.dimension")(
    "nb" -> SumField("v"),
    "stack" -> PushMulti(
      "metric" -> "_id.metric",
      "v" -> "v")).some

  def apply(question: Question[_], userId: String): NonEmptyList[PipelineOperator] = {
    import question.{ dimension, metric }
    val gameMatcher = combineDocs(question.filters.collect {
      case f if f.dimension.isInGame => f.matcher
    })
    val matchMoves = combineDocs(question.filters.collect {
      case f if f.dimension.isInMove => f.matcher
    }).some.filterNot(_.isEmpty) map Match

    // #TODO make it depend on move matchers and metric
    def projectForMove = Project(BSONDocument(
      "_id" -> false,
      "moves" -> true
    ) ++ dimension.dbKey.startsWith("moves.").fold(
        BSONDocument(),
        BSONDocument(dimension.dbKey -> true)
      )).some

    NonEmptyList.nel[PipelineOperator](
      Match(
        selectUserId(userId) ++
          gameMatcher ++
          Metric.requiresAnalysis(metric).??(BSONDocument("analysed" -> true))
      ),
      sampleGames :: ((metric match {
        case M.MeanCpl => List(
          projectForMove,
          unwindMoves,
          matchMoves,
          sampleMoves,
          group(dimension, Avg("moves.c"))
        )
        case M.NbMoves => List(
          projectForMove,
          unwindMoves,
          matchMoves,
          sampleMoves,
          group(dimension, SumValue(1))
        )
        case M.Movetime => List(
          projectForMove,
          unwindMoves,
          matchMoves,
          sampleMoves,
          group(dimension, GroupFunction("$avg",
            BSONDocument("$divide" -> BSONArray("$moves.t", 10))
          ))
        )
        case M.RatingDiff => List(
          group(dimension, Avg("ratingDiff"))
        )
        case M.OpponentRating => List(
          group(dimension, Avg("opponent.rating"))
        )
        case M.Result => List(
          GroupMulti("dimension" -> dimension.dbKey, "metric" -> "result")(
            "v" -> SumValue(1)
          ).some,
          regroupStacked
        )
        case M.PieceRole => List(
          projectForMove,
          unwindMoves,
          matchMoves,
          sampleMoves,
          GroupMulti("dimension" -> dimension.dbKey, "metric" -> "moves.r")(
            "v" -> SumValue(1)
          ).some,
          regroupStacked
        )
      }) ::: (dimension match {
        case D.Opening => List(sortNb, limit(12))
        case _         => Nil
      })).flatten
    )
  }
}
