package lila.tournament

import reactivemongo.api.bson.*

import lila.db.dsl.*

final class TournamentStatsApi(
    playerRepo: PlayerRepo,
    pairingRepo: PairingRepo,
    mongoCache: lila.memo.MongoCache.Api
)(using Executor):

  def apply(tournament: Tournament): Fu[Option[TournamentStats]] =
    tournament.isFinished.soFu(cache.get(tournament.id))

  private given BSONDocumentHandler[TournamentStats] = Macros.handler

  private val cache = mongoCache[TourId, TournamentStats](64, "tournament:stats", 60.days, _.value): loader =>
    _.expireAfterAccess(10.minutes)
      .maximumSize(256)
      .buildAsyncFuture(loader(fetch))

  private def fetch(tournamentId: TourId): Fu[TournamentStats] =
    for
      rating <- playerRepo.averageRating(tournamentId)
      rawStats <- pairingRepo.rawStats(tournamentId)
    yield TournamentStats.readAggregation(rating)(rawStats)

case class TournamentStats(
    games: Int,
    moves: Int,
    whiteWins: Int,
    blackWins: Int,
    draws: Int,
    berserks: Int,
    averageRating: Int
)

private object TournamentStats:

  private case class ColorStats(games: Int, moves: Int, b1: Int, b2: Int):
    def berserks = b1 + b2

  def readAggregation(rating: Int)(docs: List[Bdoc]): TournamentStats =
    val colorStats: Map[Option[Color], ColorStats] = docs.view.map { doc =>
      doc.getAsOpt[Boolean]("_id").map(Color.fromWhite(_)) ->
        ColorStats(
          ~doc.int("games"),
          ~doc.int("moves"),
          ~doc.int("b1"),
          ~doc.int("b2")
        )
    }.toMap
    TournamentStats(
      games = colorStats.foldLeft(0)(_ + _._2.games),
      moves = colorStats.foldLeft(0)(_ + _._2.moves),
      whiteWins = colorStats.get(Color.White.some).so(_.games),
      blackWins = colorStats.get(Color.Black.some).so(_.games),
      draws = colorStats.get(none).so(_.games),
      berserks = colorStats.foldLeft(0)(_ + _._2.berserks),
      averageRating = rating
    )
