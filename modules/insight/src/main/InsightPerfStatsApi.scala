package lila.insight

import chess.{ Centis, Color }
import reactivemongo.api.bson._
import scala.concurrent.duration._
import scala.concurrent.duration.FiniteDuration

import lila.common.config
import lila.db.dsl._
import lila.game.Game
import lila.rating.PerfType
import lila.user.User

case class InsightPerfStats(
    rating: MeanRating,
    nbGames: Color.Map[Int],
    time: FiniteDuration
) {
  def totalNbGames = nbGames.white + nbGames.black
}

object InsightPerfStats {
  case class WithGameIds(stats: InsightPerfStats, gameIds: List[Game.ID])
}

final class InsightPerfStatsApi(
    storage: InsightStorage,
    pipeline: AggregationPipeline
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(
      user: User,
      perfTypes: List[PerfType],
      gameIdsPerPerf: config.Max
  ): Fu[Map[PerfType, InsightPerfStats.WithGameIds]] =
    storage.coll {
      _.aggregateList(perfTypes.size) { framework =>
        import framework._
        import InsightEntry.{ BSONFields => F }
        val filters = List(lila.insight.Filter(InsightDimension.Perf, perfTypes))
        Match(InsightStorage.selectUserId(user.id) ++ pipeline.gameMatcher(filters)) -> List(
          Sort(Descending(F.date)),
          Limit(pipeline.maxGames.value),
          Project(
            $doc(
              F.perf   -> true,
              F.rating -> true,
              F.color  -> true,
              "t"      -> $doc("$sum" -> s"$$${F.moves("t")}")
            )
          ),
          GroupField(F.perf)(
            "r"   -> AvgField(F.rating),
            "nw"  -> Sum($doc("$cond" -> $arr("$c", 1, 0))),
            "nb"  -> Sum($doc("$cond" -> $arr("$c", 0, 1))),
            "t"   -> SumField("t"),
            "ids" -> PushField("_id")
          ),
          AddFields(
            $doc(
              "total" -> $doc("$add" -> $arr("$nw", "$nb")),
              "ids"   -> $doc("$slice" -> $arr("$ids", gameIdsPerPerf.value))
            )
          ),
          Match($doc("total" $gte 5))
        )
      }.map { docs =>
        for {
          doc <- docs
          id  <- doc int "_id"
          pt  <- PerfType(id)
          ra  <- doc double "r"
          nw = ~doc.int("nw")
          nb = ~doc.int("nb")
          t   <- doc.getAsOpt[Centis]("t")
          ids <- doc.getAsOpt[List[String]]("ids")
          gameIds = ids map Game.takeGameId
        } yield pt -> InsightPerfStats.WithGameIds(
          InsightPerfStats(MeanRating(ra.toInt), Color.Map(nw, nb), t.toDuration),
          gameIds
        )
      }.map(_.toMap)
    }
}
