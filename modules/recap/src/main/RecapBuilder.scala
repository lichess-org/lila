package lila.recap

import reactivemongo.akkastream.{ AkkaStreamCursor, cursorProducer }
import chess.ByColor
import chess.opening.OpeningDb
import chess.format.pgn.SanStr
import scalalib.model.Days

import lila.common.SimpleOpening
import lila.db.dsl.{ *, given }
import lila.game.Query
import lila.puzzle.PuzzleRound
import lila.common.LichessDay
import lila.core.game.Source
import java.time.LocalDate

private final class RecapBuilder(
    repo: RecapRepo,
    gameRepo: lila.game.GameRepo,
    puzzleColls: lila.puzzle.PuzzleColls
)(using Executor, akka.stream.Materializer):

  def compute(userId: UserId): Funit = for
    recap <- (
      runGameScan(userId).map(makeGameRecap),
      runPuzzleScan(userId).map(makePuzzleRecap)
    ).mapN: (game, puzzle) =>
      Recap(userId, yearToRecap, game, puzzle, nowInstant)
    _ <- repo.insert(recap)
  yield ()

  private def makePuzzleRecap(scan: PuzzleScan): RecapPuzzles =
    RecapPuzzles(
      nb = NbAndStreak(scan.nb, Days(scan.streak.max)),
      results = scan.results,
      votes = scan.votes
    )

  private def runPuzzleScan(userId: UserId): Fu[PuzzleScan] =
    import lila.puzzle.BsonHandlers.roundHandler
    puzzleColls.round:
      _.find($doc("u" -> userId, "d" -> $doc("$gt" -> dateStart, "$lt" -> dateEnd)))
        .sort($sort.asc("d"))
        .cursor[PuzzleRound]()
        .documentSource()
        .runFold(PuzzleScan())(_.addRound(_))

  private case class PuzzleScan(
      nb: Int = 0,
      results: Results = Results(),
      streak: Streak = Streak(),
      votes: PuzzleVotes = PuzzleVotes()
  ):
    def addRound(r: PuzzleRound): PuzzleScan =
      val win = r.firstWin
      copy(
        nb = nb + 1,
        results = results.copy(win = results.win + win.so(1), loss = results.loss + (!win).so(1)),
        streak = streak.add(r.date),
        votes = votes.copy(
          up = votes.up + r.vote.exists(_ > 0).so(1),
          down = votes.down + r.vote.exists(_ < 0).so(1),
          themes = votes.themes + r.themes.size
        )
      )

  private def makeGameRecap(scan: GameScan): RecapGames =
    RecapGames(
      nb = NbAndStreak(scan.nb, Days(scan.streak.max)),
      openings = scan.openings.map:
        _.toList.sortBy(-_._2).headOption.fold(Recap.nopening)(Recap.Counted.apply)
      ,
      firstMove = scan.firstMoves.toList.sortBy(-_._2).headOption.map(Recap.Counted.apply),
      results = scan.results,
      timePlaying = scan.secondsPlaying.seconds,
      sources = scan.sources,
      opponent = scan.opponents.toList.sortBy(-_._2).headOption.map(Recap.Counted.apply),
      perfs = scan.perfs.toList
        .sortBy(-_._2._1)
        .map:
          case (key, (seconds, games)) => Recap.Perf(key, seconds, games)
    )

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

  private case class GameScan(
      nb: Int = 0,
      secondsPlaying: Int = 0,
      results: Results = Results(),
      streak: Streak = Streak(),
      openings: ByColor[Map[SimpleOpening, Int]] = ByColor.fill(Map.empty),
      firstMoves: Map[SanStr, Int] = Map.empty,
      sources: Map[Source, Int] = Map.empty,
      opponents: Map[UserId, Int] = Map.empty,
      perfs: Map[PerfKey, (Int, Int)] = Map.empty
  ):
    def addGame(userId: UserId)(g: Game): GameScan =
      g.player(userId)
        .fold(this): player =>
          val opponent = g.opponent(player).userId
          val winner   = g.winnerUserId
          val opening = g.variant.standard.so:
            OpeningDb.search(g.sans).map(_.opening).flatMap(SimpleOpening.apply)
          val durationSeconds = g.hasClock.so(g.durationSeconds) | 30 // ?? :shrug:
          copy(
            nb = nb + 1,
            secondsPlaying = secondsPlaying + durationSeconds,
            results = results.copy(
              win = results.win + winner.exists(_.is(userId)).so(1),
              draw = results.draw + winner.isEmpty.so(1),
              loss = results.loss + winner.exists(_.isnt(userId)).so(1)
            ),
            streak = streak.add(g.createdAt),
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
              pk.fold((durationSeconds, 1).some): (seconds, games) =>
                (seconds + durationSeconds, games + 1).some
          )

  private case class Streak(
      current: Int = 0,
      max: Int = 0,
      lastPlayed: LichessDay = LichessDay(0)
  ):

    def add(playedAt: Instant): Streak =
      val day = LichessDay.dayOf(playedAt)
      val newStreak =
        if day == lastPlayed then current
        else if day == lastPlayed.map(_ + 1) then current + 1
        else 1
      Streak(
        current = newStreak,
        max = newStreak.atLeast(max),
        lastPlayed = day
      )
