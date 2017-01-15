package lila.tournament

import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
import reactivemongo.bson.Macros
import scala.concurrent.duration._

import chess.Color
import lila.db.dsl._

final class TournamentStatsApi(mongoCache: lila.memo.MongoCache.Builder) {

  def apply(tournament: Tournament): Fu[Option[TournamentStats]] =
    tournament.isFinished ?? cache(tournament.id).map(some)

  private implicit val statsBSONHandler = Macros.handler[TournamentStats]

  private val cache = mongoCache[String, TournamentStats](
    prefix = "tournament:stats",
    keyToString = identity,
    f = fetch,
    timeToLive = 90 days)

  private def fetch(tournamentId: Tournament.ID): Fu[TournamentStats] =
    PairingRepo.rawStats(tournamentId) map TournamentStats.readAggregation
}

case class TournamentStats(
  games: Int,
  moves: Int,
  whiteWins: Int,
  blackWins: Int,
  draws: Int,
  berserks: Int)

private object TournamentStats {

  private case class ColorStats(games: Int, moves: Int, b1: Int, b2: Int) {
    def berserks = b1 + b2
  }

  def readAggregation(docs: List[Bdoc]): TournamentStats = {
    val colorStats = docs.map { doc =>
      doc.getAs[Boolean]("_id").map(Color.apply) ->
        ColorStats(
          ~doc.getAs[Int]("games"),
          ~doc.getAs[Int]("moves"),
          ~doc.getAs[Int]("b1"),
          ~doc.getAs[Int]("b2"))
    }.toMap
    TournamentStats(
      games = colorStats.foldLeft(0)(_ + _._2.games),
      moves = colorStats.foldLeft(0)(_ + _._2.moves),
      whiteWins = colorStats.get(Color.White.some).??(_.games),
      blackWins = colorStats.get(Color.Black.some).??(_.games),
      draws = colorStats.get(none).??(_.games),
      berserks = colorStats.foldLeft(0)(_ + _._2.berserks))
  }
}
