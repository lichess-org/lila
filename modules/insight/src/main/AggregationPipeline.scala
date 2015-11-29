package lila.insight

import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
import reactivemongo.bson._
import scalaz.NonEmptyList

import lila.db.Implicits._

private final class AggregationPipeline {

  import lila.insight.{ Dimension => D, Metric => M }
  import Storage._

  private val sampleGames = Sample(10 * 1000)
  private val sortDate = Sort(Descending("date"))
  private val sampleMoves = Sample(200 * 1000).some
  private val unwindMoves = Unwind("moves").some
  private val sortNb = Sort(Descending("nb")).some
  private def limit(nb: Int) = Limit(nb).some
  private def group(d: Dimension[_], f: GroupFunction) = GroupField(d.dbKey)(
    "v" -> f,
    "nb" -> SumValue(1),
    "ids" -> AddToSet("_id")
  ).some
  private def groupMulti(dimension: Dimension[_], metricDbKey: String) = GroupMulti(
    "dimension" -> dimension.dbKey,
    "metric" -> metricDbKey)(
      "v" -> SumValue(1),
      "ids" -> AddToSet("_id")
    ).some
  private val regroupStacked = GroupField("_id.dimension")(
    "nb" -> SumField("v"),
    "ids" -> First("ids"),
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

    // #TODO make it depend on move matchers and metric?
    def projectForMove = Project(BSONDocument(
      "moves" -> true
    ) ++ dimension.dbKey.startsWith("moves.").fold(
        BSONDocument(),
        BSONDocument(dimension.dbKey -> true)
      )).some
    def sliceIds = Project(BSONDocument(
      "_id" -> true,
      "v" -> true,
      "nb" -> true,
      "ids" -> BSONDocument("$slice" -> BSONArray("$ids", 4))
    )).some
    def sliceStackedIds = Project(BSONDocument(
      "_id" -> true,
      "stack" -> true,
      "ids" -> BSONDocument("$slice" -> BSONArray("$ids", 4))
    )).some

    NonEmptyList.nel[PipelineOperator](
      Match(
        selectUserId(userId) ++
          gameMatcher ++
          Metric.requiresAnalysis(metric).??(BSONDocument("analysed" -> true))
      ),
      /* sortDate :: */ sampleGames :: ((metric match {
        case M.MeanCpl => List(
          projectForMove,
          unwindMoves,
          matchMoves,
          sampleMoves,
          group(dimension, Avg("moves.c")),
          sliceIds
        )
        case M.NbMoves => List(
          projectForMove,
          unwindMoves,
          matchMoves,
          sampleMoves,
          GroupField(dimension.dbKey)(
            "v" -> SumValue(1),
            "ids" -> AddToSet("_id")
          ).some,
          Project(BSONDocument(
            "v" -> true,
            "ids" -> true,
            "nb" -> BSONDocument("$size" -> "$ids")
          )).some,
          Project(BSONDocument(
            "v" -> BSONDocument(
              "$divide" -> BSONArray("$v", "$nb")
            ),
            "nb" -> true,
            "ids" -> BSONDocument("$slice" -> BSONArray("$ids", 4))
          )).some
        )
        case M.Movetime => List(
          projectForMove,
          unwindMoves,
          matchMoves,
          sampleMoves,
          group(dimension, GroupFunction("$avg",
            BSONDocument("$divide" -> BSONArray("$moves.t", 10))
          )),
          sliceIds
        )
        case M.RatingDiff => List(
          group(dimension, Avg("ratingDiff")),
          sliceIds
        )
        case M.OpponentRating => List(
          group(dimension, Avg("opponent.rating")),
          sliceIds
        )
        case M.Result => List(
          groupMulti(dimension, "result"),
          regroupStacked,
          sliceStackedIds
        )
        case M.Termination => List(
          groupMulti(dimension, "termination"),
          regroupStacked,
          sliceStackedIds
        )
        case M.PieceRole => List(
          projectForMove,
          unwindMoves,
          matchMoves,
          sampleMoves,
          groupMulti(dimension, "moves.r"),
          regroupStacked,
          sliceStackedIds
        )
      }) ::: (dimension match {
        case D.Opening => List(sortNb, limit(12))
        case _         => Nil
      })).flatten
    )
  }
}
