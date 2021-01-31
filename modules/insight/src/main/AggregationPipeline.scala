package lila.insight

import reactivemongo.api.bson._

import lila.db.dsl._
import lila.user.User

final private class AggregationPipeline(store: Storage)(implicit ec: scala.concurrent.ExecutionContext) {

  def aggregate[X](question: Question[X], user: User): Fu[List[Bdoc]] =
    store.coll {
      _.aggregateList(
        maxDocs = Int.MaxValue,
        allowDiskUse = true
      ) { implicit framework =>
        import framework._
        import question.{ dimension, filters, metric }

        import lila.insight.{ Dimension => D, Metric => M }
        import InsightEntry.{ BSONFields => F }
        import Storage._

        val sampleGames    = Sample(10_000)
        val sampleMoves    = Sample(200_000).some
        val unwindMoves    = UnwindField(F.moves).some
        val sortNb         = Sort(Descending("nb")).some
        def limit(nb: Int) = Limit(nb).some

        val regroupStacked = GroupField("_id.dimension")(
          "nb"    -> SumField("v"),
          "ids"   -> FirstField("ids"),
          "stack" -> Push($doc("metric" -> "$_id.metric", "v" -> "$v"))
        )

        lazy val movetimeIdDispatcher =
          MovetimeRange.reversedNoInf.foldLeft[BSONValue](BSONInteger(MovetimeRange.MTRInf.id)) {
            case (acc, mtr) =>
              $doc(
                "$cond" -> $arr(
                  $doc("$lte" -> $arr("$" + F.moves("t"), mtr.tenths.last)),
                  mtr.id,
                  acc
                )
              )
          }
        lazy val materialIdDispatcher = $doc(
          "$cond" -> $arr(
            $doc("$eq" -> $arr("$" + F.moves("i"), 0)),
            MaterialRange.Equal.id,
            MaterialRange.reversedButEqualAndLast.foldLeft[BSONValue](BSONInteger(MaterialRange.Up4.id)) {
              case (acc, mat) =>
                $doc(
                  "$cond" -> $arr(
                    $doc((if (mat.negative) "$lt" else "$lte") -> $arr("$" + F.moves("i"), mat.imbalance)),
                    mat.id,
                    acc
                  )
                )
            }
          )
        )
        lazy val timeVarianceIdDispatcher =
          TimeVariance.all.reverse
            .drop(1)
            .foldLeft[BSONValue](BSONInteger(TimeVariance.VeryVariable.intFactored)) { case (acc, tvi) =>
              $doc(
                "$cond" -> $arr(
                  $doc("$lte" -> $arr("$" + F.moves("v"), tvi.intFactored)),
                  tvi.intFactored,
                  acc
                )
              )
            }
        def dimensionGroupId(dim: Dimension[_]): BSONValue =
          dim match {
            case Dimension.MovetimeRange => movetimeIdDispatcher
            case Dimension.MaterialRange => materialIdDispatcher
            case Dimension.TimeVariance  => timeVarianceIdDispatcher
            case d                       => BSONString("$" + d.dbKey)
          }
        sealed trait Grouping
        object Grouping {
          object Group                                                            extends Grouping
          case class BucketAuto(buckets: Int, granularity: Option[String] = None) extends Grouping
        }
        def dimensionGrouping(dim: Dimension[_]): Grouping =
          dim match {
            case D.Date => Grouping.BucketAuto(buckets = 12)
            case _      => Grouping.Group
          }

        val gameIdsSlice       = $doc("ids" -> $doc("$slice" -> $arr("$ids", 4)))
        val includeSomeGameIds = AddFields(gameIdsSlice)
        val toPercent          = $doc("v" -> $doc("$multiply" -> $arr(100, $doc("$avg" -> "$v"))))

        def group(d: Dimension[_], f: GroupFunction): List[Option[PipelineOperator]] =
          List(dimensionGrouping(d) match {
            case Grouping.Group =>
              Group(dimensionGroupId(d))(
                "v"   -> f,
                "nb"  -> SumAll,
                "ids" -> AddFieldToSet("_id")
              )
            case Grouping.BucketAuto(buckets, granularity) =>
              BucketAuto(dimensionGroupId(d), buckets, granularity)(
                "v"   -> f,
                "nb"  -> SumAll,
                "ids" -> AddFieldToSet("_id") // AddFieldToSet crashes mongodb 3.4.1 server
              )
          }) map { Option(_) }

        def groupMulti(d: Dimension[_], metricDbKey: String): List[Option[PipelineOperator]] =
          (dimensionGrouping(d) match {
            case Grouping.Group =>
              List[PipelineOperator](
                Group($doc("dimension" -> dimensionGroupId(d), "metric" -> s"$$$metricDbKey"))(
                  "v"   -> SumAll,
                  "ids" -> AddFieldToSet("_id")
                ),
                regroupStacked,
                includeSomeGameIds
              )
            case Grouping.BucketAuto(buckets, granularity) =>
              List[PipelineOperator](
                BucketAuto(dimensionGroupId(d), buckets, granularity)(
                  "doc" -> Push(
                    $doc(
                      "id"     -> "$_id",
                      "metric" -> s"$$$metricDbKey"
                    )
                  )
                ),
                UnwindField("doc"),
                Group($doc("dimension" -> "$_id", "metric" -> "$doc.metric"))(
                  "v"   -> SumAll,
                  "ids" -> AddFieldToSet("doc.id")
                ),
                regroupStacked,
                includeSomeGameIds,
                Sort(Ascending("_id.min"))
              )
          }) map { Option(_) }

        val gameMatcher = combineDocs(question.filters.collect {
          case f if f.dimension.isInGame => f.matcher
        })

        def matchMoves(extraMatcher: Bdoc = $empty): Option[PipelineOperator] =
          combineDocs(extraMatcher :: question.filters.collect {
            case f if f.dimension.isInMove => f.matcher
          } ::: (dimension match {
            case D.TimeVariance => List($doc(F.moves("v") $exists true))
            case _              => List.empty[Bdoc]
          })).some.filterNot(_.isEmpty) map Match.apply

        def projectForMove: Option[PipelineOperator] =
          Project(BSONDocument({
            metric.dbKey :: dimension.dbKey :: filters.collect {
              case lila.insight.Filter(d, _) if d.isInMove => d.dbKey
            }
          }.distinct.map(_ -> BSONBoolean(true)))).some

        val pipeline = Match(
          selectUserId(user.id) ++
            gameMatcher ++
            (dimension == Dimension.Opening).??($doc(F.eco $exists true)) ++
            Metric.requiresAnalysis(metric).??($doc(F.analysed -> true)) ++
            (Metric.requiresStableRating(metric) || Dimension.requiresStableRating(dimension)).?? {
              $doc(F.provisional $ne true)
            }
        ) -> /* sortDate :: */ {
          sampleGames :: ((metric match {
            case M.MeanCpl =>
              List(
                projectForMove,
                unwindMoves,
                matchMoves(),
                sampleMoves
              ) :::
                group(dimension, AvgField(F.moves("c"))) :::
                List(includeSomeGameIds.some)
            case M.Material =>
              List(
                projectForMove,
                unwindMoves,
                matchMoves(),
                sampleMoves
              ) :::
                group(dimension, AvgField(F.moves("i"))) :::
                List(includeSomeGameIds.some)
            case M.Opportunism =>
              List(
                projectForMove,
                unwindMoves,
                matchMoves($doc(F.moves("o") $exists true)),
                sampleMoves
              ) :::
                group(dimension, GroupFunction("$push", $doc("$cond" -> $arr("$" + F.moves("o"), 1, 0)))) :::
                List(AddFields(gameIdsSlice ++ toPercent).some)
            case M.Luck =>
              List(
                projectForMove,
                unwindMoves,
                matchMoves($doc(F.moves("l") $exists true)),
                sampleMoves
              ) :::
                group(dimension, GroupFunction("$push", $doc("$cond" -> $arr("$" + F.moves("l"), 1, 0)))) :::
                List(AddFields(gameIdsSlice ++ toPercent).some)
            case M.Blurs =>
              List(
                projectForMove,
                unwindMoves,
                matchMoves(),
                sampleMoves
              ) :::
                group(dimension, GroupFunction("$push", $doc("$cond" -> $arr("$" + F.moves("b"), 1, 0)))) :::
                List(AddFields(gameIdsSlice ++ toPercent).some)
            case M.NbMoves =>
              List(
                projectForMove,
                unwindMoves,
                matchMoves(),
                sampleMoves
              ) :::
                group(dimension, SumAll) :::
                List(
                  Project(
                    $doc(
                      "v"   -> true,
                      "ids" -> true,
                      "nb"  -> $doc("$size" -> "$ids")
                    )
                  ).some,
                  AddFields(
                    $doc("v" -> $doc("$divide" -> $arr("$v", "$nb"))) ++
                      gameIdsSlice
                  ).some
                )
            case M.Movetime =>
              List(
                projectForMove,
                unwindMoves,
                matchMoves(),
                sampleMoves
              ) :::
                group(
                  dimension,
                  GroupFunction(
                    "$avg",
                    $doc("$divide" -> $arr("$" + F.moves("t"), 10))
                  )
                ) :::
                List(includeSomeGameIds.some)
            case M.RatingDiff =>
              group(dimension, AvgField(F.ratingDiff)) ::: List(includeSomeGameIds.some)
            case M.OpponentRating =>
              group(dimension, AvgField(F.opponentRating)) ::: List(includeSomeGameIds.some)
            case M.Result =>
              groupMulti(dimension, F.result)
            case M.Termination =>
              groupMulti(dimension, F.termination)
            case M.PieceRole =>
              List(
                projectForMove,
                unwindMoves,
                matchMoves(),
                sampleMoves
              ) :::
                groupMulti(dimension, F.moves("r"))
            case M.TimeVariance =>
              List(
                projectForMove,
                unwindMoves,
                matchMoves($doc(F.moves("v") $exists true)),
                sampleMoves
              ) :::
                group(
                  dimension,
                  GroupFunction(
                    "$avg",
                    $doc("$divide" -> $arr("$" + F.moves("v"), TimeVariance.intFactor))
                  )
                ) :::
                List(includeSomeGameIds.some)
          }) ::: (dimension match {
            case D.Opening => List(sortNb, limit(12))
            case _         => Nil
          })).flatten
        }
        pipeline
      }
    }
}
