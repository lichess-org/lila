package lila.insight

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

final private class AggregationPipeline(store: InsightStorage)(using
    ec: Executor
):
  import InsightStorage.*
  import BSONHandlers.given

  val maxGames = Max(10_000)

  def gameMatcher(filters: List[Filter[?]]) = combineDocs(filters.collect {
    case f if f.dimension.isInGame => f.matcher
  })

  def aggregate[X](
      question: Question[X],
      target: Either[User, Question.Peers],
      withPovs: Boolean,
      nbGames: Max = maxGames
  ): Fu[List[Bdoc]] =
    store.coll:
      _.aggregateList(maxDocs = Int.MaxValue, allowDiskUse = true): framework =>
        import framework.*
        import question.{ dimension, filters, metric }
        import lila.insight.{ InsightDimension as D, InsightMetric as M }
        import InsightEntry.BSONFields as F

        val limitGames     = Limit(nbGames.value)
        val sortDate       = target.isLeft.so(List(Sort(Descending(F.date))))
        val limitMoves     = Limit((200_000 / maxGames.value.toDouble * nbGames.value).toInt).some
        val unwindMoves    = UnwindField(F.moves).some
        val sortNb         = Sort(Descending("nb")).some
        def limit(nb: Int) = Limit(nb).some

        def groupOptions(identifiers: pack.Value)(ops: (String, Option[GroupFunction])*) =
          Group(identifiers)(ops.collect { case (k, Some(f)) => k -> f }*)

        def groupFieldOptions(idField: String)(ops: (String, Option[GroupFunction])*) =
          GroupField(idField)(ops.collect { case (k, Some(f)) => k -> f }*)

        def bucketAutoOptions(groupBy: pack.Value, buckets: Int, granularity: Option[String])(
            output: (String, Option[GroupFunction])*
        ) = BucketAuto(groupBy, buckets, granularity)(output.collect { case (k, Some(f)) => k -> f }*)

        val regroupStacked = groupFieldOptions("_id.dimension")(
          "nb"    -> SumField("v").some,
          "ids"   -> withPovs.option(FirstField("ids")),
          "stack" -> Push($doc("metric" -> "$_id.metric", "v" -> "$v")).some
        )

        lazy val movetimeIdDispatcher =
          MovetimeRange.reversedNoInf.foldLeft[BSONValue](BSONInteger(MovetimeRange.MTRInf.id)):
            case (acc, mtr) =>
              $doc(
                "$cond" -> $arr(
                  $doc("$lt" -> $arr("$" + F.moves("t"), mtr.tenths)),
                  mtr.id,
                  acc
                )
              )

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

        lazy val accuracyPercentDispatcher =
          $doc( // rounding
            "$multiply" -> $arr(
              10,
              $doc(
                "$toInt" -> $arr($divide(s"$$${F.moves("a")}", percentBsonMultiplier * 10))
              )
            )
          )
        lazy val winPercentDispatcher =
          $doc( // rounding
            "$multiply" -> $arr(
              10,
              $doc(
                "$toInt" -> $arr($divide(s"$$${F.moves("w")}", percentBsonMultiplier * 10))
              )
            )
          )

        def clockPercentDispatcher =
          ClockPercentRange.all.tail
            .foldLeft[BSONValue](BSONInteger(ClockPercentRange.all.head.bottom.toInt)) { (acc, tp) =>
              $doc(
                "$cond" -> $arr(
                  $doc("$gte" -> $arr("$" + F.moves("s"), tp.bottom)),
                  tp.bottom.toInt,
                  acc
                )
              )
            }

        lazy val materialIdDispatcher = $doc(
          "$cond" -> $arr(
            $doc("$eq" -> $arr("$" + F.moves("i"), 0)),
            MaterialRange.Equal.id,
            MaterialRange.reversedButEqualAndLast.foldLeft[BSONValue](BSONInteger(MaterialRange.Up4.id)) {
              (acc, mat) =>
                $doc(
                  "$cond" -> $arr(
                    $doc((if mat.negative then "$lt" else "$lte") -> $arr("$" + F.moves("i"), mat.imbalance)),
                    mat.id,
                    acc
                  )
                )
            }
          )
        )
        lazy val evalIdDispatcher =
          EvalRange.reversedButLast.foldLeft[BSONValue](BSONInteger(EvalRange.Up5.id)) { (acc, ev) =>
            $doc(
              "$cond" -> $arr(
                $doc("$lt" -> $arr("$" + F.moves("e"), ev.eval)),
                ev.id,
                acc
              )
            )
          }
        lazy val timeVarianceIdDispatcher =
          TimeVariance.values.reverse
            .drop(1)
            .foldLeft[BSONValue](BSONInteger(TimeVariance.VeryVariable.intFactored)) { (acc, tvi) =>
              $doc(
                "$cond" -> $arr(
                  $doc("$lte" -> $arr("$" + F.moves("v"), tvi.intFactored)),
                  tvi.intFactored,
                  acc
                )
              )
            }
        def dimensionGroupId(dim: InsightDimension[?]): BSONValue =
          dim match
            case InsightDimension.MovetimeRange        => movetimeIdDispatcher
            case InsightDimension.CplRange             => cplIdDispatcher
            case InsightDimension.AccuracyPercentRange => accuracyPercentDispatcher
            case InsightDimension.MaterialRange        => materialIdDispatcher
            case InsightDimension.EvalRange            => evalIdDispatcher
            case InsightDimension.WinPercentRange      => winPercentDispatcher
            case InsightDimension.TimeVariance         => timeVarianceIdDispatcher
            case InsightDimension.ClockPercentRange    => clockPercentDispatcher
            case d                                     => BSONString("$" + d.dbKey)
        enum Grouping:
          case Group
          case BucketAuto(buckets: Int, granularity: Option[String] = None)
        def dimensionGrouping(dim: InsightDimension[?]): Grouping =
          dim match
            case D.Date => Grouping.BucketAuto(buckets = 12)
            case _      => Grouping.Group

        val gameIdsSlice       = withPovs.option($doc("ids" -> $doc("$slice" -> $arr("$ids", 4))))
        val includeSomeGameIds = gameIdsSlice.map(AddFields.apply)
        val addGameId          = withPovs.option(AddFieldToSet("_id"))
        val ratioToPercent     = $doc("v" -> $multiply(100, "$v"))
        val bsonRatioToPercent = $doc("v" -> $divide("$v", ratioBsonMultiplier / 100))

        def group(d: InsightDimension[?], f: GroupFunction): List[Option[PipelineOperator]] =
          List(dimensionGrouping(d) match
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
              )).map(some)

        def groupMulti(d: InsightDimension[?], metricDbKey: String): List[Option[PipelineOperator]] =
          dimensionGrouping(d)
            .match
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
            .map(some)

        val fieldExistsMatcher: Bdoc = dimension.some
          .filter(InsightDimension.optionalDimensions.contains)
          .filter(dim => !question.filters.exists(_.dimension == dim))
          .so { dim => $doc(dim.dbKey.$exists(true)) }

        def matchMoves(extraMatcher: Bdoc = $empty): Option[PipelineOperator] =
          combineDocs(
            extraMatcher :: question.filters.collect {
              case f if f.dimension.isInMove => f.matcher
            } ::: dimension
              .match
                case D.TimeVariance         => "v".some
                case D.CplRange             => "c".some
                case D.AccuracyPercentRange => "a".some
                case D.EvalRange            => "e".some
                case D.WinPercentRange      => "w".some
                case _                      => none
              .map(moveField => $doc(F.moves(moveField).$exists(true)))
              .toList :::
              metric.match
                case InsightMetric.MeanAccuracy => List($doc(F.moves("a").$exists(true)))
                case _                          => List.empty[Bdoc]
          ).some.filterNot(_.isEmpty).map(Match.apply)

        def projectForMove: Option[PipelineOperator] =
          Project(BSONDocument({
            metric.dbKey :: dimension.dbKey :: filters.collect {
              case lila.insight.Filter(d, _) if d.isInMove => d.dbKey
            }
          }.distinct.map(_ -> BSONBoolean(true)))).some

        val pipeline = Match(
          target.fold(u => selectUserId(u.id), selectPeers) ++
            gameMatcher(question.filters) ++
            fieldExistsMatcher ++
            (InsightMetric.requiresAnalysis(metric) || InsightDimension.requiresAnalysis(dimension))
              .so($doc(F.analysed -> true)) ++
            (InsightMetric.requiresStableRating(metric) || InsightDimension.requiresStableRating(dimension))
              .so {
                $doc(F.provisional.$ne(true))
              }
        ) -> {
          sortDate ::: limitGames :: ((metric.match
            case M.MeanCpl =>
              List(
                projectForMove,
                unwindMoves,
                matchMoves(),
                limitMoves
              ) :::
                group(dimension, AvgField(F.moves("c"))) :::
                List(includeSomeGameIds)
            case M.CplBucket =>
              List(
                projectForMove,
                unwindMoves,
                matchMoves(),
                limitMoves,
                AddFields($doc("cplBucket" -> cplIdDispatcher)).some
              ) :::
                groupMulti(dimension, "cplBucket")
            case M.MeanAccuracy => // harmonic mean
              List(
                projectForMove,
                unwindMoves,
                matchMoves(),
                limitMoves,
                AddFields(
                  $doc:
                    "step" -> $doc:
                      "$divide" -> $arr(1, $doc("$max" -> $arr(1, $divide("$m.a", percentBsonMultiplier))))
                ).some
              ) :::
                group(dimension, SumField("step")) :::
                List(
                  AddFields($doc("v" -> $divide("$nb", "$v"))).some,
                  includeSomeGameIds
                )
            case M.Material =>
              List(
                projectForMove,
                unwindMoves,
                matchMoves(),
                limitMoves
              ) :::
                group(dimension, AvgField(F.moves("i"))) :::
                List(includeSomeGameIds)
            case M.Awareness =>
              List(
                projectForMove,
                unwindMoves,
                matchMoves($doc(F.moves("o").$exists(true))),
                limitMoves
              ) :::
                group(dimension, GroupFunction("$avg", $doc("$cond" -> $arr("$" + F.moves("o"), 1, 0)))) :::
                List(AddFields(~gameIdsSlice ++ ratioToPercent).some)
            case M.Luck =>
              List(
                projectForMove,
                unwindMoves,
                matchMoves($doc(F.moves("l").$exists(true))),
                limitMoves
              ) :::
                group(dimension, GroupFunction("$avg", $doc("$cond" -> $arr("$" + F.moves("l"), 1, 0)))) :::
                List(AddFields(~gameIdsSlice ++ ratioToPercent).some)
            case M.ClockPercent =>
              List(
                projectForMove,
                unwindMoves,
                limitMoves
              ) :::
                group(dimension, AvgField(F.moves("s"))) :::
                List(AddFields(~gameIdsSlice ++ bsonRatioToPercent).some)
            case M.Blurs =>
              List(
                projectForMove,
                unwindMoves,
                matchMoves(),
                limitMoves
              ) :::
                group(dimension, GroupFunction("$avg", $doc("$cond" -> $arr("$" + F.moves("b"), 1, 0)))) :::
                List(AddFields(~gameIdsSlice ++ ratioToPercent).some)
            case M.NbMoves =>
              List(
                projectForMove,
                unwindMoves,
                matchMoves(),
                limitMoves
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
                  AddFields($doc("v" -> $divide("$v", "$nb")) ++ ~gameIdsSlice).some
                )
            case M.Movetime =>
              List(
                projectForMove,
                unwindMoves,
                matchMoves(),
                limitMoves
              ) :::
                group(dimension, GroupFunction("$avg", $divide(s"$$${F.moves("t")}", 10))) :::
                List(includeSomeGameIds)
            case M.RatingDiff =>
              group(dimension, AvgField(F.ratingDiff)) ::: List(includeSomeGameIds)
            case M.Performance =>
              group(
                dimension,
                Avg:
                  $doc:
                    "$avg" -> $doc:
                      "$add" -> $arr(
                        "$or",
                        $doc("$multiply" -> $arr(500, $doc("$subtract" -> $arr(2, "$r"))))
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
                limitMoves
              ) :::
                groupMulti(dimension, F.moves("r"))
            case M.TimeVariance =>
              List(
                projectForMove,
                unwindMoves,
                matchMoves($doc(F.moves("v").$exists(true))),
                limitMoves
              ) :::
                group(
                  dimension,
                  GroupFunction("$avg", $divide("$" + F.moves("v"), TimeVariance.intFactor))
                ) :::
                List(includeSomeGameIds)
          ) ::: dimension.match
            case D.OpeningVariation | D.OpeningFamily => List(sortNb, limit(12))
            case _                                    => Nil
          ).flatten
        }
        pipeline
