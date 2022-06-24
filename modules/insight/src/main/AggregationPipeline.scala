package lila.insight

import reactivemongo.api.bson._

import lila.db.dsl._
import lila.user.User
import lila.common.config

final private class AggregationPipeline(store: InsightStorage)(implicit
    ec: scala.concurrent.ExecutionContext
) {
  import InsightStorage._

  val maxGames = config.Max(10_000)

  def gameMatcher(filters: List[Filter[_]]) = combineDocs(filters.collect {
    case f if f.dimension.isInGame => f.matcher
  })

  def aggregate[X](
      question: Question[X],
      target: Either[User, Question.Peers],
      withPovs: Boolean,
      nbGames: config.Max = maxGames
  ): Fu[List[Bdoc]] =
    store.coll {
      _.aggregateList(
        maxDocs = Int.MaxValue,
        allowDiskUse = true
      ) { implicit framework =>
        import framework._
        import question.{ dimension, filters, metric }
        import lila.insight.{ InsightDimension => D, Metric => M }
        import InsightEntry.{ BSONFields => F }

        val limitGames     = Limit(maxGames.value)
        val sortDate       = Sort(Descending(F.date))
        val sampleMoves    = Sample(200_000).some
        val unwindMoves    = UnwindField(F.moves).some
        val sortNb         = Sort(Descending("nb")).some
        def limit(nb: Int) = Limit(nb).some

        def groupOptions(identifiers: pack.Value)(ops: (String, Option[GroupFunction])*) =
          Group(identifiers)(ops.collect { case (k, Some(f)) => k -> f }: _*)
        def groupFieldOptions(idField: String)(ops: (String, Option[GroupFunction])*) =
          GroupField(idField)(ops.collect { case (k, Some(f)) => k -> f }: _*)
        def bucketAutoOptions(groupBy: pack.Value, buckets: Int, granularity: Option[String])(
            output: (String, Option[GroupFunction])*
        ) = BucketAuto(groupBy, buckets, granularity)(output.collect { case (k, Some(f)) => k -> f }: _*)

        val regroupStacked = groupFieldOptions("_id.dimension")(
          "nb"    -> SumField("v").some,
          "ids"   -> withPovs.option(FirstField("ids")),
          "stack" -> Push($doc("metric" -> "$_id.metric", "v" -> "$v")).some
        )

        lazy val movetimeIdDispatcher =
          MovetimeRange.reversedNoInf.foldLeft[BSONValue](BSONInteger(MovetimeRange.MTRInf.id)) {
            case (acc, mtr) =>
              $doc(
                "$cond" -> $arr(
                  $doc("$lt" -> $arr("$" + F.moves("t"), mtr.tenths)),
                  mtr.id,
                  acc
                )
              )
          }

        lazy val cplIdDispatcher =
          CplRange.all.reverse.foldLeft[BSONValue](BSONInteger(CplRange.worse.cpl)) { case (acc, cpl) =>
            $doc(
              "$cond" -> $arr(
                $doc("$lte" -> $arr("$" + F.moves("c"), cpl.cpl)),
                cpl.cpl,
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
        lazy val evalIdDispatcher =
          EvalRange.reversedButLast.foldLeft[BSONValue](BSONInteger(EvalRange.Up5.id)) { case (acc, ev) =>
            $doc(
              "$cond" -> $arr(
                $doc("$lt" -> $arr("$" + F.moves("e"), ev.eval)),
                ev.id,
                acc
              )
            )
          }
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
        def dimensionGroupId(dim: InsightDimension[_]): BSONValue =
          dim match {
            case InsightDimension.MovetimeRange => movetimeIdDispatcher
            case InsightDimension.CplRange      => cplIdDispatcher
            case InsightDimension.MaterialRange => materialIdDispatcher
            case InsightDimension.EvalRange     => evalIdDispatcher
            case InsightDimension.TimeVariance  => timeVarianceIdDispatcher
            case d                              => BSONString("$" + d.dbKey)
          }
        sealed trait Grouping
        object Grouping {
          object Group                                                            extends Grouping
          case class BucketAuto(buckets: Int, granularity: Option[String] = None) extends Grouping
        }
        def dimensionGrouping(dim: InsightDimension[_]): Grouping =
          dim match {
            case D.Date => Grouping.BucketAuto(buckets = 12)
            case _      => Grouping.Group
          }

        val gameIdsSlice       = withPovs option $doc("ids" -> $doc("$slice" -> $arr("$ids", 4)))
        val includeSomeGameIds = gameIdsSlice map AddFields.apply
        val addGameId          = withPovs option PushField("_id")
        val toPercent          = $doc("v" -> $doc("$multiply" -> $arr(100, $doc("$avg" -> "$v"))))

        def group(d: InsightDimension[_], f: GroupFunction): List[Option[PipelineOperator]] =
          List(dimensionGrouping(d) match {
            case Grouping.Group =>
              groupOptions(dimensionGroupId(d))(
                "v"   -> f.some,
                "nb"  -> SumAll.some,
                "ids" -> addGameId
              )
            case Grouping.BucketAuto(buckets, granularity) =>
              bucketAutoOptions(dimensionGroupId(d), buckets, granularity)(
                "v"   -> f.some,
                "nb"  -> SumAll.some,
                "ids" -> addGameId
              )
          }) map some

        def groupMulti(d: InsightDimension[_], metricDbKey: String): List[Option[PipelineOperator]] =
          dimensionGrouping(d) ap {
            case Grouping.Group =>
              List(
                groupOptions($doc("dimension" -> dimensionGroupId(d), "metric" -> s"$$$metricDbKey"))(
                  "v"   -> SumAll.some,
                  "ids" -> addGameId
                ).some,
                regroupStacked.some,
                includeSomeGameIds
              ).flatten
            case Grouping.BucketAuto(buckets, granularity) =>
              List(
                BucketAuto(dimensionGroupId(d), buckets, granularity)(
                  "doc" -> Push(
                    $doc(
                      "id"     -> "$_id",
                      "metric" -> s"$$$metricDbKey"
                    )
                  )
                ).some,
                UnwindField("doc").some,
                groupOptions($doc("dimension" -> "$_id", "metric" -> "$doc.metric"))(
                  "v"   -> SumAll.some,
                  "ids" -> addGameId
                ).some,
                regroupStacked.some,
                includeSomeGameIds,
                Sort(Ascending("_id.min")).some
              ).flatten
          } map some

        val fieldExistsMatcher: Bdoc = dimension.some
          .filter(InsightDimension.optionalDimensions.contains)
          .filter(dim => !question.filters.exists(_.dimension == dim))
          .?? { dim => $doc(dim.dbKey $exists true) }

        def matchMoves(extraMatcher: Bdoc = $empty): Option[PipelineOperator] =
          combineDocs(extraMatcher :: question.filters.collect {
            case f if f.dimension.isInMove => f.matcher
          } ::: dimension.ap {
            case D.TimeVariance => List($doc(F.moves("v") $exists true))
            case D.CplRange     => List($doc(F.moves("c") $exists true))
            case D.EvalRange    => List($doc(F.moves("e") $exists true))
            case _              => List.empty[Bdoc]
          }).some.filterNot(_.isEmpty) map Match.apply

        def projectForMove: Option[PipelineOperator] =
          Project(BSONDocument({
            metric.dbKey :: dimension.dbKey :: filters.collect {
              case lila.insight.Filter(d, _) if d.isInMove => d.dbKey
            }
          }.distinct.map(_ -> BSONBoolean(true)))).some

        val pipeline = Match(
          target.fold(u => selectUserId(u.id), peers => $doc(F.rating $inRange peers.ratingRange)) ++
            gameMatcher(question.filters) ++
            fieldExistsMatcher ++
            (Metric.requiresAnalysis(metric) || InsightDimension.requiresAnalysis(dimension))
              .??($doc(F.analysed -> true)) ++
            (Metric.requiresStableRating(metric) || InsightDimension.requiresStableRating(dimension)).?? {
              $doc(F.provisional $ne true)
            }
        ) -> {
          sortDate :: limitGames :: (metric.ap {
            case M.MeanCpl =>
              List(
                projectForMove,
                unwindMoves,
                matchMoves(),
                sampleMoves
              ) :::
                group(dimension, AvgField(F.moves("c"))) :::
                List(includeSomeGameIds)
            case M.CplBucket =>
              List(
                projectForMove,
                unwindMoves,
                matchMoves(),
                sampleMoves,
                AddFields($doc("cplBucket" -> cplIdDispatcher)).some
              ) :::
                groupMulti(dimension, "cplBucket")
            // case M.MeanAccuracy => // power mean
            //   List(
            //     projectForMove,
            //     unwindMoves,
            //     matchMoves(),
            //     sampleMoves,
            //     AddFields(
            //       $doc("pow" -> $doc("$pow" -> $arr($doc("$subtract" -> $arr(1000, "$m.a")), 3)))
            //     ).some
            //   ) :::
            //     group(dimension, SumField("pow")) :::
            //     List(
            //       AddFields(
            //         $doc(
            //           "v" -> $doc(
            //             "$divide" -> $arr(
            //               $doc(
            //                 "$subtract" -> $arr(
            //                   1000,
            //                   $doc("$pow" -> $arr($doc("$divide" -> $arr("$v", "$nb")), 1 / 3d))
            //                 )
            //               ),
            //               10
            //             )
            //           )
            //         )
            //       ).some,
            //       includeSomeGameIds
            //     )
            case M.MeanAccuracy => // harmonic mean
              List(
                projectForMove,
                unwindMoves,
                matchMoves(),
                sampleMoves,
                AddFields(
                  $doc("step" -> $doc("$divide" -> $arr(1, $doc("$add" -> $arr(1, "$m.a")))))
                ).some
              ) :::
                group(dimension, SumField("step")) :::
                List(
                  AddFields(
                    $doc(
                      "v" ->
                        $doc(
                          "$divide" ->
                            $arr(
                              $doc(
                                "$subtract" -> $arr(
                                  $doc("$divide" -> $arr("$nb", "$v")),
                                  1
                                )
                              ),
                              10
                            )
                        )
                    )
                  ).some,
                  includeSomeGameIds
                )
            case M.Material =>
              List(
                projectForMove,
                unwindMoves,
                matchMoves(),
                sampleMoves
              ) :::
                group(dimension, AvgField(F.moves("i"))) :::
                List(includeSomeGameIds)
            case M.Awareness =>
              List(
                projectForMove,
                unwindMoves,
                matchMoves($doc(F.moves("o") $exists true)),
                sampleMoves
              ) :::
                group(dimension, GroupFunction("$push", $doc("$cond" -> $arr("$" + F.moves("o"), 1, 0)))) :::
                List(AddFields(~gameIdsSlice ++ toPercent).some)
            case M.Luck =>
              List(
                projectForMove,
                unwindMoves,
                matchMoves($doc(F.moves("l") $exists true)),
                sampleMoves
              ) :::
                group(dimension, GroupFunction("$push", $doc("$cond" -> $arr("$" + F.moves("l"), 1, 0)))) :::
                List(AddFields(~gameIdsSlice ++ toPercent).some)
            case M.Blurs =>
              List(
                projectForMove,
                unwindMoves,
                matchMoves(),
                sampleMoves
              ) :::
                group(dimension, GroupFunction("$push", $doc("$cond" -> $arr("$" + F.moves("b"), 1, 0)))) :::
                List(AddFields(~gameIdsSlice ++ toPercent).some)
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
                      "ids" -> withPovs,
                      "nb"  -> $doc("$size" -> "$ids")
                    )
                  ).some,
                  AddFields($doc("v" -> $doc("$divide" -> $arr("$v", "$nb"))) ++ ~gameIdsSlice).some
                )
            case M.Movetime =>
              List(
                projectForMove,
                unwindMoves,
                matchMoves(),
                sampleMoves
              ) :::
                group(dimension, GroupFunction("$avg", $doc("$divide" -> $arr(s"$$${F.moves("t")}", 10)))) :::
                List(includeSomeGameIds)
            case M.RatingDiff =>
              group(dimension, AvgField(F.ratingDiff)) ::: List(includeSomeGameIds)
            case M.Performance =>
              group(
                dimension,
                Avg(
                  $doc(
                    "$avg" -> $doc(
                      "$add" -> $arr(
                        "$or",
                        $doc("$multiply" -> $arr(500, $doc("$subtract" -> $arr(2, "$r"))))
                      )
                    )
                  )
                )
              ) ::: List(includeSomeGameIds)
            case M.OpponentRating =>
              group(dimension, AvgField(F.opponentRating)) ::: List(includeSomeGameIds)
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
                List(includeSomeGameIds)
          } ::: dimension.ap {
            case D.OpeningVariation | D.OpeningFamily => List(sortNb, limit(12))
            case _                                    => Nil
          }).flatten
        }
        pipeline
      }
    }
}
