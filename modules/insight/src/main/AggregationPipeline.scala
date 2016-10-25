package lila.insight

import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
import reactivemongo.bson._
import scalaz.NonEmptyList

import lila.db.dsl._

private final class AggregationPipeline {

  import lila.insight.{ Dimension => D, Metric => M }
  import Storage._
  import Entry.{ BSONFields => F }

  private lazy val movetimeIdDispatcher =
    MovetimeRange.reversedNoInf.foldLeft[BSONValue](BSONInteger(MovetimeRange.MTRInf.id)) {
      case (acc, mtr) => $doc(
        "$cond" -> $arr(
          $doc("$lte" -> $arr("$" + F.moves("t"), mtr.tenths.last)),
          mtr.id,
          acc))
    }
  private lazy val materialIdDispatcher = $doc(
    "$cond" -> $arr(
      $doc("$eq" -> $arr("$" + F.moves("i"), 0)),
      MaterialRange.Equal.id,
      MaterialRange.reversedButEqualAndLast.foldLeft[BSONValue](BSONInteger(MaterialRange.Up4.id)) {
        case (acc, mat) => $doc(
          "$cond" -> $arr(
            $doc(mat.negative.fold("$lt", "$lte") -> $arr("$" + F.moves("i"), mat.imbalance)),
            mat.id,
            acc))
      }))
  private def dimensionGroupId(dim: Dimension[_]): BSONValue = dim match {
    case Dimension.MovetimeRange => movetimeIdDispatcher
    case Dimension.MaterialRange => materialIdDispatcher
    case d                       => BSONString("$" + d.dbKey)
  }

  private val sampleGames = Sample(10 * 1000)
  private val sortDate = Sort(Descending(F.date))
  private val sampleMoves = Sample(200 * 1000).some
  private val unwindMoves = Unwind(F.moves).some
  private val sortNb = Sort(Descending("nb")).some
  private def limit(nb: Int) = Limit(nb).some
  private def group(d: Dimension[_], f: GroupFunction) = Group(dimensionGroupId(d))(
    "v" -> f,
    "nb" -> SumValue(1),
    "ids" -> AddToSet("_id")
  ).some
  private def groupMulti(d: Dimension[_], metricDbKey: String) = Group($doc(
    "dimension" -> dimensionGroupId(d),
    "metric" -> ("$" + metricDbKey)))(
    "v" -> SumValue(1),
    "ids" -> AddToSet("_id")
  ).some
  private val regroupStacked = GroupField("_id.dimension")(
    "nb" -> SumField("v"),
    "ids" -> First("ids"),
    "stack" -> PushMulti(
      "metric" -> "_id.metric",
      "v" -> "v")).some
  private val sliceIds = Project($doc(
    "_id" -> true,
    "v" -> true,
    "nb" -> true,
    "ids" -> $doc("$slice" -> $arr("$ids", 4))
  )).some
  private val sliceStackedIds = Project($doc(
    "_id" -> true,
    "nb" -> true,
    "stack" -> true,
    "ids" -> $doc("$slice" -> $arr("$ids", 4))
  )).some

  def apply(question: Question[_], userId: String): NonEmptyList[PipelineOperator] = {
    import question.{ dimension, metric, filters }
    val gameMatcher = combineDocs(question.filters.collect {
      case f if f.dimension.isInGame => f.matcher
    })
    def matchMoves(extraMatcher: Bdoc = $empty) =
      combineDocs(extraMatcher :: question.filters.collect {
        case f if f.dimension.isInMove => f.matcher
      }).some.filterNot(_.isEmpty) map Match
    def projectForMove = Project($doc({
      metric.dbKey :: dimension.dbKey :: filters.collect {
        case Filter(d, _) if d.isInMove => d.dbKey
      }
    }.distinct.map(_ -> BSONBoolean(true)))).some

    NonEmptyList.nel[PipelineOperator](
      Match(
        selectUserId(userId) ++
          gameMatcher ++
          (dimension == Dimension.Opening).??($doc(F.eco -> $doc("$exists" -> true))) ++
          Metric.requiresAnalysis(metric).??($doc(F.analysed -> true)) ++
          (Metric.requiresStableRating(metric) || Dimension.requiresStableRating(dimension)).?? {
            $doc(F.provisional -> $doc("$ne" -> true))
          }
      ),
      /* sortDate :: */ sampleGames :: ((metric match {
        case M.MeanCpl => List(
          projectForMove,
          unwindMoves,
          matchMoves(),
          sampleMoves,
          group(dimension, Avg(F.moves("c"))),
          sliceIds
        )
        case M.Material => List(
          projectForMove,
          unwindMoves,
          matchMoves(),
          sampleMoves,
          group(dimension, Avg(F.moves("i"))),
          sliceIds
        )
        case M.Opportunism => List(
          projectForMove,
          unwindMoves,
          matchMoves($doc(F.moves("o") -> $doc("$exists" -> true))),
          sampleMoves,
          group(dimension, GroupFunction("$push", $doc(
            "$cond" -> $arr("$" + F.moves("o"), 1, 0)
          ))),
          sliceIds,
          Project($doc(
            "_id" -> true,
            "v" -> $doc("$multiply" -> $arr(100, $doc("$avg" -> "$v"))),
            "nb" -> true,
            "ids" -> true
          )).some
        )
        case M.Luck => List(
          projectForMove,
          unwindMoves,
          matchMoves($doc(F.moves("l") -> $doc("$exists" -> true))),
          sampleMoves,
          group(dimension, GroupFunction("$push", $doc(
            "$cond" -> $arr("$" + F.moves("l"), 1, 0)
          ))),
          sliceIds,
          Project($doc(
            "_id" -> true,
            "v" -> $doc("$multiply" -> $arr(100, $doc("$avg" -> "$v"))),
            "nb" -> true,
            "ids" -> true
          )).some
        )
        case M.NbMoves => List(
          projectForMove,
          unwindMoves,
          matchMoves(),
          sampleMoves,
          group(dimension, SumValue(1)),
          Project($doc(
            "v" -> true,
            "ids" -> true,
            "nb" -> $doc("$size" -> "$ids")
          )).some,
          Project($doc(
            "v" -> $doc(
              "$divide" -> $arr("$v", "$nb")
            ),
            "nb" -> true,
            "ids" -> $doc("$slice" -> $arr("$ids", 4))
          )).some
        )
        case M.Movetime => List(
          projectForMove,
          unwindMoves,
          matchMoves(),
          sampleMoves,
          group(dimension, GroupFunction("$avg",
            $doc("$divide" -> $arr("$" + F.moves("t"), 10))
          )),
          sliceIds
        )
        case M.RatingDiff => List(
          group(dimension, Avg(F.ratingDiff)),
          sliceIds
        )
        case M.OpponentRating => List(
          group(dimension, Avg(F.opponentRating)),
          sliceIds
        )
        case M.Result => List(
          groupMulti(dimension, F.result),
          regroupStacked,
          sliceStackedIds
        )
        case M.Termination => List(
          groupMulti(dimension, F.termination),
          regroupStacked,
          sliceStackedIds
        )
        case M.PieceRole => List(
          projectForMove,
          unwindMoves,
          matchMoves(),
          sampleMoves,
          groupMulti(dimension, F.moves("r")),
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
