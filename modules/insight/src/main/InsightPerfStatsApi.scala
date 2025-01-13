package lila.insight

import chess.{ ByColor, Centis }
import reactivemongo.api.bson.*

import lila.core.perf.PerfId
import lila.db.dsl.{ *, given }
import lila.rating.PerfType

case class InsightPerfStats(
    rating: MeanRating,
    nbGames: ByColor[Int],
    time: FiniteDuration,
    dates: Option[TimeInterval]
):
  def totalNbGames = nbGames.white + nbGames.black
  def peers        = Question.Peers(rating)

object InsightPerfStats:
  case class WithGameIds(stats: InsightPerfStats, gameIds: List[GameId])

final class InsightPerfStatsApi(
    storage: InsightStorage,
    pipeline: AggregationPipeline
)(using Executor):

  def apply(
      user: User,
      perfTypes: List[PerfType],
      gameIdsPerPerf: Max
  ): Fu[Map[PerfType, InsightPerfStats.WithGameIds]] =
    storage.coll:
      _.aggregateList(perfTypes.size): framework =>
        import framework.*
        import InsightEntry.BSONFields as F
        val filters = List(lila.insight.Filter(InsightDimension.Perf, perfTypes))
        Match(InsightStorage.selectUserId(user.id) ++ pipeline.gameMatcher(filters)) -> List(
          Sort(Descending(F.date)),
          Limit(pipeline.maxGames.value),
          Project(
            $doc(
              F.perf   -> true,
              F.rating -> true,
              F.color  -> true,
              F.date   -> true,
              "t"      -> $doc("$sum" -> s"$$${F.moves("t")}")
            )
          ),
          GroupField(F.perf)(
            "r"    -> AvgField(F.rating),
            "nw"   -> Sum($doc("$cond" -> $arr("$c", 1, 0))),
            "nb"   -> Sum($doc("$cond" -> $arr("$c", 0, 1))),
            "t"    -> SumField("t"),
            "ids"  -> PushField("_id"),
            "from" -> LastField(F.date),
            "to"   -> FirstField(F.date)
          ),
          AddFields(
            $doc(
              "total" -> $doc("$add" -> $arr("$nw", "$nb")),
              "ids"   -> $doc("$slice" -> $arr("$ids", gameIdsPerPerf.value))
            )
          ),
          Match($doc("total".$gte(5)))
        )
      .map: docs =>
        for
          doc <- docs
          id  <- doc.getAsOpt[PerfId]("_id")
          pt  <- PerfType(id)
          ra  <- doc.double("r")
          nw = ~doc.int("nw")
          nb = ~doc.int("nb")
          t   <- doc.getAsOpt[Centis]("t")
          ids <- doc.getAsOpt[List[String]]("ids")
          gameIds = ids.map(GameId.take)
          interval = for
            start <- doc.getAsOpt[Instant]("from")
            end   <- doc.getAsOpt[Instant]("to")
          yield TimeInterval(start, end)
        yield pt -> InsightPerfStats
          .WithGameIds(
            InsightPerfStats(MeanRating(ra.toInt), ByColor(nw, nb), t.toDuration, interval),
            gameIds
          )
      .map(_.toMap)
