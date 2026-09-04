package lila.insight

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

final private class AggregationPipeline(store: InsightStorage)(using Executor):
  import InsightStorage.*
  import BSONHandlers.given

  def aggregate[X](
      question: Question[X],
      target: Either[User, PeersRatingRange],
      withPovs: Boolean,
      nbGames: Max = maxGames
  ): Fu[List[Bdoc]] =
    store.coll:
      _.aggregateList(maxDocs = Int.MaxValue, allowDiskUse = true): framework =>
        import framework.*
        import question.{ dimension, filters, metric }
        import lila.insight.{ InsightDimension as D, InsightMetric as M }
        import InsightEntry.BSONFields as F

        val limitGames = Limit(nbGames.value)
        val sortDate = target.isLeft.so(List(Sort(Descending(F.date))))
        val limitMoves = Limit((200_000 / maxGames.value.toDouble * nbGames.value).toInt).some
        val unwindMoves = UnwindField(F.moves).some
        val sortNb = Sort(Descending("nb")).some
        def limit(nb: Int) = Limit(nb).some

        def groupOptions(identifiers: pack.Value)(ops: (String, Option[GroupFunction])*) =
          Group(identifiers)(ops.collect { case (k, Some(f)) => k -> f }*)

        def groupFieldOptions(idField: String)(ops: (String, Option[GroupFunction])*) =
          GroupField(idField)(ops.collect { case (k, Some(f)) => k -> f }*)

        def bucketAutoOptions(groupBy: pack.Value, buckets: Int, granularity: Option[String])(
            output: (String, Option[GroupFunction])*
        ) = BucketAuto(groupBy, buckets, granularity)(output.collect { case (k, Some(f)) => k -> f }*)

        val regroupStacked = groupFieldOptions("_id.dimension")(
          "nb" -> SumField("v").some,
          "ids" -> withPovs.option(FirstField("ids")),
          "stack" -> Push(bdoc("metric" -> "$_id.metric", "v" -> "$v")).some
        )

        lazy val movetimeIdDispatcher =
          MovetimeRange.reversedNoInf.foldLeft[BSONValue](BSONInteger(MovetimeRange.MTRInf.id)): (acc, mtr) =>
            bdoc(
              "$cond" -> barr(
                bdoc("$lt" -> barr("$" + F.moves("t"), mtr.tenths)),
                mtr.id,
                acc
              )
            )
        lazy val cplIdDispatcher =
          CplRange.all.reverse.foldLeft[BSONValue](BSONInteger(CplRange.worse.cpl)): (acc, cpl) =>
            bdoc(
              "$cond" -> barr(
                bdoc("$lte" -> barr("$" + F.moves("c"), cpl.cpl)),
                cpl.cpl,
                acc
              )
            )
        def roundingDispatcher(moveField: String, factor: Int) = bdoc(
          "$multiply" -> barr(
            factor,
            bdoc("$toInt" -> barr(divide(s"$$${F.moves(moveField)}", percentBsonMultiplier * factor)))
          )
        )
        def clockPercentDispatcher =
          ClockPercentRange.all.tail
            .foldLeft[BSONValue](BSONInteger(ClockPercentRange.all.head.bottom.toInt)): (acc, tp) =>
              bdoc(
                "$cond" -> barr(
                  bdoc("$gte" -> barr("$" + F.moves("s"), tp.bottom)),
                  tp.bottom.toInt,
                  acc
                )
              )
        lazy val materialIdDispatcher = bdoc(
          "$cond" -> barr(
            bdoc("$eq" -> barr("$" + F.moves("i"), 0)),
            MaterialRange.Equal.id,
            MaterialRange.reversedButEqualAndLast.foldLeft[BSONValue](BSONInteger(MaterialRange.Up4.id)) {
              (acc, mat) =>
                bdoc(
                  "$cond" -> barr(
                    bdoc((if mat.negative then "$lt" else "$lte") -> barr("$" + F.moves("i"), mat.imbalance)),
                    mat.id,
                    acc
                  )
                )
            }
          )
        )
        lazy val evalIdDispatcher =
          EvalRange.reversedButLast.foldLeft[BSONValue](BSONInteger(EvalRange.Up5.id)): (acc, ev) =>
            bdoc(
              "$cond" -> barr(
                bdoc("$lt" -> barr("$" + F.moves("e"), ev.eval)),
                ev.id,
                acc
              )
            )
        lazy val timeVarianceIdDispatcher =
          TimeVariance.values.reverse
            .drop(1)
            .foldLeft[BSONValue](BSONInteger(TimeVariance.VeryVariable.intFactored)): (acc, tvi) =>
              bdoc(
                "$cond" -> barr(
                  bdoc("$lte" -> barr("$" + F.moves("v"), tvi.intFactored)),
                  tvi.intFactored,
                  acc
                )
              )
        def dimensionGroupId(dim: InsightDimension[?]): BSONValue =
          dim match
            case InsightDimension.MovetimeRange => movetimeIdDispatcher
            case InsightDimension.CplRange => cplIdDispatcher
            case InsightDimension.AccuracyPercentRange => roundingDispatcher("a", 10)
            case InsightDimension.MaterialRange => materialIdDispatcher
            case InsightDimension.EvalRange => evalIdDispatcher
            case InsightDimension.WinPercentRange => roundingDispatcher("w", 10)
            case InsightDimension.TimeVariance => timeVarianceIdDispatcher
            case InsightDimension.ClockPercentRange => clockPercentDispatcher
            case d => BSONString("$" + d.dbKey)
        enum Grouping:
          case Group
          case BucketAuto(buckets: Int, granularity: Option[String] = None)
        def dimensionGrouping(dim: InsightDimension[?]): Grouping =
          dim match
            case D.Date => Grouping.BucketAuto(buckets = 12)
            case _ => Grouping.Group

        def divide[A: BSONWriter, B: BSONWriter](a: A, b: B): Bdoc = bdoc("$divide" -> barr(a, b))
        def multiply[A: BSONWriter, B: BSONWriter](a: A, b: B): Bdoc = bdoc("$multiply" -> barr(a, b))

        val gameIdsSlice = withPovs.option(bdoc("ids" -> bdoc("$slice" -> barr("$ids", 4))))
        val includeSomeGameIds = gameIdsSlice.map(AddFields.apply)
        val addGameId = withPovs.option(AddFieldToSet("_id"))
        val ratioToPercent = bdoc("v" -> multiply(100, "$v"))
        val bsonRatioToPercent = bdoc("v" -> divide("$v", ratioBsonMultiplier / 100))

        def group(d: InsightDimension[?], f: GroupFunction): List[Option[PipelineOperator]] =
          List(dimensionGrouping(d) match
            case Grouping.Group =>
              groupOptions(dimensionGroupId(d))(
                "v" -> f.some,
                "nb" -> SumAll.some,
                "ids" -> addGameId
              )
            case Grouping.BucketAuto(buckets, granularity) =>
              bucketAutoOptions(dimensionGroupId(d), buckets, granularity)(
                "v" -> f.some,
                "nb" -> SumAll.some,
                "ids" -> addGameId
              )).map(some)

        def groupMulti(d: InsightDimension[?], metricDbKey: String): List[Option[PipelineOperator]] =
          dimensionGrouping(d)
            .match
              case Grouping.Group =>
                List(
                  groupOptions(bdoc("dimension" -> dimensionGroupId(d), "metric" -> s"$$$metricDbKey"))(
                    "v" -> SumAll.some,
                    "ids" -> addGameId
                  ).some,
                  regroupStacked.some,
                  includeSomeGameIds
                ).flatten
              case Grouping.BucketAuto(buckets, granularity) =>
                List(
                  BucketAuto(dimensionGroupId(d), buckets, granularity)(
                    "doc" -> Push(
                      bdoc(
                        "id" -> "$_id",
                        "metric" -> s"$$$metricDbKey"
                      )
                    )
                  ).some,
                  UnwindField("doc").some,
                  groupOptions(bdoc("dimension" -> "$_id", "metric" -> "bdoc.metric"))(
                    "v" -> SumAll.some,
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
          .so { dim => bdoc(dim.dbKey.exists(true)) }

        def matchMoves(extraMatcher: Bdoc = emptyBdoc): Option[PipelineOperator] =
          combineDocs(
            extraMatcher :: question.filters.collect {
              case f if f.dimension.isInMove => f.matcher
            } ::: dimension
              .match
                case D.TimeVariance => "v".some
                case D.CplRange => "c".some
                case D.AccuracyPercentRange => "a".some
                case D.EvalRange => "e".some
                case D.WinPercentRange => "w".some
                case _ => none
              .map(moveField => bdoc(F.moves(moveField).exists(true)))
              .toList :::
              metric.match
                case InsightMetric.MeanAccuracy => List(bdoc(F.moves("a").exists(true)))
                case _ => List.empty[Bdoc]
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
              .so(bdoc(F.analysed -> true)) ++
            (InsightMetric.requiresStableRating(metric) || InsightDimension.requiresStableRating(dimension))
              .so(bdoc(F.provisional.neq(true)))
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
                AddFields(bdoc("cplBucket" -> cplIdDispatcher)).some
              ) :::
                groupMulti(dimension, "cplBucket")
            case M.MeanAccuracy => // harmonic mean
              List(
                projectForMove,
                unwindMoves,
                matchMoves(),
                limitMoves,
                AddFields(
                  bdoc:
                    "step" -> bdoc:
                      "$divide" -> barr(1, bdoc("$max" -> barr(1, divide("$m.a", percentBsonMultiplier))))
                ).some
              ) :::
                group(dimension, SumField("step")) :::
                List(
                  AddFields(bdoc("v" -> divide("$nb", "$v"))).some,
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
                matchMoves(bdoc(F.moves("o").exists(true))),
                limitMoves
              ) :::
                group(dimension, GroupFunction("$avg", bdoc("$cond" -> barr("$" + F.moves("o"), 1, 0)))) :::
                List(AddFields(~gameIdsSlice ++ ratioToPercent).some)
            case M.Luck =>
              List(
                projectForMove,
                unwindMoves,
                matchMoves(bdoc(F.moves("l").exists(true))),
                limitMoves
              ) :::
                group(dimension, GroupFunction("$avg", bdoc("$cond" -> barr("$" + F.moves("l"), 1, 0)))) :::
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
                group(dimension, GroupFunction("$avg", bdoc("$cond" -> barr("$" + F.moves("b"), 1, 0)))) :::
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
                    bdoc(
                      "v" -> true,
                      "ids" -> withPovs,
                      "nb" -> bdoc("$size" -> "$ids")
                    )
                  ).some,
                  AddFields(bdoc("v" -> divide("$v", "$nb")) ++ ~gameIdsSlice).some
                )
            case M.Movetime =>
              List(
                projectForMove,
                unwindMoves,
                matchMoves(),
                limitMoves
              ) :::
                group(dimension, GroupFunction("$avg", divide(s"$$${F.moves("t")}", 10))) :::
                List(includeSomeGameIds)
            case M.RatingDiff =>
              group(dimension, AvgField(F.ratingDiff)) ::: List(includeSomeGameIds)
            case M.Performance =>
              group(
                dimension,
                Avg:
                  bdoc:
                    "$avg" -> bdoc:
                      "$add" -> barr(
                        "$or",
                        bdoc("$multiply" -> barr(500, bdoc("$subtract" -> barr(2, "$r"))))
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
                matchMoves(bdoc(F.moves("v").exists(true))),
                limitMoves
              ) :::
                group(
                  dimension,
                  GroupFunction("$avg", divide("$" + F.moves("v"), TimeVariance.intFactor))
                ) :::
                List(includeSomeGameIds)
          ) ::: dimension.match
            case D.OpeningVariation | D.OpeningFamily => List(sortNb, limit(12))
            case _ => Nil
          ).flatten
        }
        pipeline
