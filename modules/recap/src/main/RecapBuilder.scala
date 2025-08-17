package lila.recap

import reactivemongo.akkastream.{ AkkaStreamCursor, cursorProducer }
import reactivemongo.api.bson.BSONNull
import chess.ByColor
import chess.opening.OpeningDb
import chess.format.pgn.SanStr

import lila.common.SimpleOpening
import lila.db.dsl.{ *, given }
import lila.game.Query
import lila.core.game.Source

private final class RecapBuilder(
    repo: RecapRepo,
    gameRepo: lila.game.GameRepo,
    puzzleColls: lila.puzzle.PuzzleColls
)(using Executor, akka.stream.Materializer):

  def compute(userId: UserId): Funit = for
    recap <- (
      runGameScan(userId).map(makeGameRecap),
      runPuzzleScan(userId).map(_ | RecapPuzzles())
    ).mapN: (game, puzzle) =>
      Recap(userId, yearToRecap, game, puzzle, nowInstant)
    _ <- repo.insert(recap)
  yield ()

  private def runPuzzleScan(userId: UserId): Fu[Option[RecapPuzzles]] =
    puzzleColls.round:
      _.aggregateOne() { framework =>
        import framework.*
        Match($doc("u" -> userId, "d" -> $doc("$gt" -> dateStart, "$lt" -> dateEnd))) -> List(
          Group(BSONNull)(
            "nb" -> SumAll,
            "wins" -> Sum($doc("$cond" -> $arr("$w", 1, 0))),
            "fixes" -> Sum($doc("$cond" -> $arr($doc("$and" -> $arr("$w", "$f")), 1, 0))),
            "votes" -> Sum($doc("$cond" -> $arr("$v", 1, 0))),
            "themes" -> Sum($doc("$cond" -> $arr("$t", 1, 0)))
          )
        )
      }.map: r =>
        for
          doc <- r
          nb <- doc.int("nb")
          wins <- doc.int("wins")
          fixes <- doc.int("fixes")
          votes <- doc.int("votes")
          themes <- doc.int("themes")
        yield RecapPuzzles(
          nbs = NbWin(total = nb, win = wins - fixes),
          votes = PuzzleVotes(nb = votes, themes = themes)
        )
      .monSuccess(_.recap.puzzles)

  private def makeGameRecap(scan: GameScan): RecapGames =
    RecapGames(
      nbs = scan.nbs,
      nbWhite = scan.nbWhite,
      moves = scan.nbMoves,
      openings = scan.openings.map:
        _.toList.sortBy(-_._2).headOption.fold(Recap.nopening)(Recap.Counted.apply)
      ,
      firstMoves = scan.firstMoves.toList.sortBy(-_._2).take(5).map(Recap.Counted.apply),
      timePlaying = scan.secondsPlaying.seconds,
      sources = scan.sources,
      opponents = scan.opponents.toList.sortBy(-_._2).take(5).map(Recap.Counted.apply),
      perfs = scan.perfs.toList.sortBy(-_._2).map(Recap.Perf.apply)
    )

  /* This might be made faster by:
   * - fetching Bdoc instead of Game with a projection
   * - uncompressing only the moves needed to compute the opening
   * - using mutable state instead of runFold
   *   as the many little immutable objects hit the GC hard
   */
  private def runGameScan(userId: UserId): Fu[GameScan] =
    val query =
      Query.createdBetween(dateStart.some, dateEnd.some) ++
        Query.user(userId) ++
        Query.finished ++
        Query.turnsGt(2) ++
        Query.notFromPosition
    gameRepo
      .sortedCursor(query, Query.sortChronological)
      .documentSource()
      .runFold(GameScan())(_.addGame(userId)(_))
      .monSuccess(_.recap.games)

  private case class GameScan(
      nbs: NbWin = NbWin(),
      nbWhite: Int = 0,
      nbMoves: Int = 0,
      secondsPlaying: Int = 0,
      openings: ByColor[Map[SimpleOpening, Int]] = ByColor.fill(Map.empty),
      firstMoves: Map[SanStr, Int] = Map.empty,
      sources: Map[Source, Int] = Map.empty,
      opponents: Map[UserId, Int] = Map.empty,
      perfs: Map[PerfKey, Int] = Map.empty
  ):
    def addGame(userId: UserId)(g: Game): GameScan =
      g.player(userId)
        .fold(this): player =>
          val opponent = g.opponent(player).userId
          val winner = g.winnerUserId
          val opening = g.variant.standard.so:
            OpeningDb.search(g.sans).map(_.opening).flatMap(SimpleOpening.apply)
          val durationSeconds = g.hasClock.so(g.durationSeconds) | 30 // ?? :shrug:
          copy(
            nbs = NbWin(
              total = nbs.total + 1,
              win = nbs.win + winner.exists(_.is(userId)).so(1)
            ),
            nbWhite = nbWhite + player.color.fold(1, 0),
            nbMoves = nbMoves + g.playerMoves(player.color),
            secondsPlaying = secondsPlaying + durationSeconds,
            openings = opening.fold(openings): op =>
              openings.update(player.color, _.updatedWith(op)(_.fold(1)(_ + 1).some)),
            firstMoves = player.color.white
              .so(g.sans.headOption)
              .fold(firstMoves): fm =>
                firstMoves.updatedWith(fm)(_.fold(1)(_ + 1).some),
            sources = g.source.fold(sources): source =>
              sources.updatedWith(source)(_.fold(1)(_ + 1).some),
            opponents = opponent.fold(opponents): op =>
              opponents.updatedWith(op)(_.fold(1)(_ + 1).some),
            perfs = perfs.updatedWith(g.perfKey): pk =>
              some(pk.orZero + 1)
          )
