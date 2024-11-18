package lila.recap

import chess.ByColor
import lila.game.Query
import chess.opening.OpeningDb
import lila.common.SimpleOpening
import chess.format.pgn.SanStr

private final class RecapBuilder(repo: RecapRepo, gameRepo: lila.game.GameRepo)(using
    Executor,
    akka.stream.Materializer
):

  private def scanToRecap(userId: UserId, scan: Scan): Recap =
    Recap(
      id = userId,
      nbGames = scan.nbGames,
      openings = scan.openings.map:
        _.toList.sortBy(-_._2).headOption.fold(Recap.nopening)(Recap.Counted.apply)
      ,
      firstMove = scan.firstMoves.toList.sortBy(-_._2).headOption.map(Recap.Counted.apply),
      results = scan.results,
      timePlaying = scan.secondsPlaying.seconds,
      opponent = scan.opponents.toList.sortBy(-_._2).headOption.map(Recap.Counted.apply),
      createdAt = nowInstant
    )

  private def runScan(userId: UserId): Fu[Scan] =
    val query =
      Scan.createdThisYear ++
        Query.user(userId) ++
        Query.finished ++
        Query.turnsGt(2) ++
        Query.notFromPosition
    gameRepo
      .sortedCursor(query, Query.sortChronological)
      .documentSource()
      .runFold(Scan.zero)(_.addGame(userId)(_))

  def compute(userId: UserId): Funit =
    println(s"RecapBuilder.compute $userId")
    println(Scan.zero)
    println("WTFFFFFFFFFFFFFFF")
    for
      scan <- fuccess(Scan.zero) // Scan.run(userId.pp("Scan.run"))
      recap = scanToRecap(userId, scan).pp
      _ <- repo.insert(recap)
    yield ()

private case class Scan(
    nbGames: Int,
    secondsPlaying: Int,
    results: Recap.Results,
    openings: ByColor[Map[SimpleOpening, Int]],
    firstMoves: Map[SanStr, Int],
    opponents: Map[UserId, Int]
):
  def addGame(userId: UserId)(g: Game): Scan =
    println(s"${g.id} ${nbGames}")
    g.player(userId)
      .fold(this): player =>
        val opponent = g.opponent(player).userId
        val winner   = g.winnerUserId
        val opening = g.variant.standard.so:
          OpeningDb.search(g.sans).map(_.opening).flatMap(SimpleOpening.apply)
        copy(
          nbGames = nbGames + 1,
          secondsPlaying = secondsPlaying + {
            g.hasClock.so(g.durationSeconds) | 30 // ?? :shrug:
          },
          results = results.copy(
            win = results.win + winner.exists(_.is(userId)).so(1),
            draw = results.draw + winner.isEmpty.so(1),
            loss = results.loss + winner.exists(_.isnt(userId)).so(1)
          ),
          openings = opening.fold(openings): op =>
            openings.update(player.color, _.updatedWith(op)(_.fold(1)(_ + 1).some)),
          firstMoves = player.color.white
            .so(g.sans.headOption)
            .fold(firstMoves): fm =>
              firstMoves.updatedWith(fm)(_.fold(1)(_ + 1).some),
          opponents = opponent.fold(opponents): op =>
            opponents.updatedWith(op)(_.fold(1)(_ + 1).some)
        )

private object Scan:

  def zero = Scan(0, 0, Recap.Results(0, 0, 0), ByColor(Map.empty), Map.empty, Map.empty)
  val createdThisYear =
    Query.createdBetween(instantOf(2024, 1, 1, 0, 0).some, instantOf(2025, 1, 1, 0, 0).some)
