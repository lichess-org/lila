package lila.insight

import chess.{ Centis, ByColor }
import reactivemongo.api.bson.*

import lila.common.config
import lila.db.dsl.{ *, given }
import lila.game.Game
import lila.rating.{ Perf, PerfType }
import lila.user.User

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
  val empty = WithGameIds(InsightPerfStats(MeanRating(0), ByColor(0, 0), 0.seconds, none), Nil)

final class InsightPerfStatsApi(
    storage: InsightStorage,
    pipeline: AggregationPipeline
)(using Executor):

  def only(
      user: User,
      perf: PerfType,
      nbGames: config.Max,
      gameIds: config.Max
  ): Fu[Option[InsightPerfStats.WithGameIds]] =
    storage.coll:
      _.aggregateOne(): framework =>
        import framework.*
        import InsightEntry.{ BSONFields as F }
        val filters = List(lila.insight.Filter(InsightDimension.Perf, List(perf)))
        Match(InsightStorage.selectUserId(user.id) ++ pipeline.gameMatcher(filters)) -> List(
          Sort(Descending(F.date)),
          Limit(nbGames.value),
          Project(
            $doc(
              F.rating -> true,
              F.color  -> true,
              F.date   -> true,
              "t"      -> $doc("$sum" -> s"$$${F.moves("t")}")
            )
          ),
          Group(BSONNull)(
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
              "ids"   -> $doc("$slice" -> $arr("$ids", gameIds.value))
            )
          ),
          Match($doc("total" $gte 5))
        )
      .map: docO =>
        for
          doc <- docO
          ra  <- doc double "r"
          nw = ~doc.int("nw")
          nb = ~doc.int("nb")
          t   <- doc.getAsOpt[Centis]("t")
          ids <- doc.getAsOpt[List[String]]("ids")
          gameIds = ids map GameId.take
          interval = for
            start <- doc.getAsOpt[Instant]("from")
            end   <- doc.getAsOpt[Instant]("to")
          yield TimeInterval(start, end)
        yield InsightPerfStats.WithGameIds(
          InsightPerfStats(MeanRating(ra.toInt), ByColor(nw, nb), t.toDuration, interval),
          gameIds
        )
