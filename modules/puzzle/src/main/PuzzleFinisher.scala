package lila.puzzle

import cats.syntax.all.*
import lila.rating.glicko2
import scala.util.chaining.*

import lila.common.Bus
import lila.db.dsl.{ *, given }
import lila.rating.{ Glicko, Perf, PerfType }
import lila.user.{ User, UserRepo }
import chess.Mode
import lila.common.config.Max
import lila.puzzle.PuzzleForm.batch.Solution

final private[puzzle] class PuzzleFinisher(
    api: PuzzleApi,
    userRepo: UserRepo,
    historyApi: lila.history.HistoryApi,
    colls: PuzzleColls
)(using ec: Executor, scheduler: Scheduler):

  private val sequencer = lila.hub.AsyncActorSequencers[PuzzleId](
    maxSize = Max(64),
    expiration = 5 minutes,
    timeout = 5 seconds,
    name = "puzzle.finish"
  )

  def batch(me: User, angle: PuzzleAngle, solutions: List[Solution]): Fu[List[(PuzzleRound, IntRatingDiff)]] =
    lila.common.LilaFuture
      .fold(solutions)((me.perfs.puzzle, List.empty[(PuzzleRound, IntRatingDiff)])) {
        case ((perf, rounds), sol) =>
          apply(sol.id, angle, me, sol.win, sol.mode) map {
            case Some((round, newPerf)) =>
              val rDiff = IntRatingDiff(newPerf.intRating.value - perf.intRating.value)
              (newPerf, (round, rDiff) :: rounds)
            case None => (perf, rounds)
          }
      }
      .map { (_, rounds) => rounds.reverse }

  def apply(
      id: PuzzleId,
      angle: PuzzleAngle,
      user: User,
      win: PuzzleWin,
      mode: Mode
  ): Fu[Option[(PuzzleRound, Perf)]] =
    if (api.casual(user, id)) fuccess {
      PuzzleRound(
        id = PuzzleRound.Id(user.id, id),
        win = win,
        fixedAt = none,
        date = nowInstant
      ) -> user.perfs.puzzle
    } dmap some
    else
      sequencer(id) {
        api.round.find(user, id) zip api.puzzle.find(id) flatMap {
          case (_, None) => fuccess(none)
          case (prevRound, Some(puzzle)) =>
            val now = nowInstant
            val (round, newPuzzleGlicko, userPerf) = prevRound match
              case Some(prev) =>
                (
                  prev.updateWithWin(win),
                  none,
                  user.perfs.puzzle
                )
              case None if mode.casual =>
                val round = PuzzleRound(
                  id = PuzzleRound.Id(user.id, puzzle.id),
                  win = win,
                  fixedAt = none,
                  date = now
                )
                (round, none, user.perfs.puzzle)
              case None =>
                val userRating = user.perfs.puzzle.toRating
                val puzzleRating = new glicko2.Rating(
                  puzzle.glicko.rating atLeast Glicko.minRating.value,
                  puzzle.glicko.deviation,
                  puzzle.glicko.volatility,
                  puzzle.plays,
                  none
                )
                updateRatings(userRating, puzzleRating, win)
                val newPuzzleGlicko = !user.perfs.dubiousPuzzle ?? ponder
                  .puzzle(
                    angle,
                    win,
                    puzzle.glicko -> Glicko(
                      rating = puzzleRating.rating
                        .atMost(puzzle.glicko.rating + Glicko.maxRatingDelta)
                        .atLeast(puzzle.glicko.rating - Glicko.maxRatingDelta),
                      deviation = puzzleRating.ratingDeviation,
                      volatility = puzzleRating.volatility
                    ).cap,
                    player = user.perfs.puzzle.glicko
                  )
                  .some
                  .filter(puzzle.glicko !=)
                  .filter(_.sanityCheck)
                val round =
                  PuzzleRound(
                    id = PuzzleRound.Id(user.id, puzzle.id),
                    win = win,
                    fixedAt = none,
                    date = now
                  )
                val userPerf =
                  user.perfs.puzzle
                    .addOrReset(_.puzzle.crazyGlicko, s"puzzle ${puzzle.id}")(userRating, now) pipe { p =>
                    p.copy(glicko =
                      ponder.player(angle, win, user.perfs.puzzle.glicko -> p.glicko, puzzle.glicko)
                    )
                  }
                (round, newPuzzleGlicko, userPerf)
            api.round.upsert(round, angle) zip
              colls.puzzle {
                _.update
                  .one(
                    $id(puzzle.id),
                    $inc(Puzzle.BSONFields.plays -> $int(1)) ++ newPuzzleGlicko.?? { glicko =>
                      $set(Puzzle.BSONFields.glicko -> glicko)
                    }
                  )
                  .void
              } zip
              (userPerf != user.perfs.puzzle).?? {
                userRepo.setPerf(user.id, PerfType.Puzzle, userPerf.clearRecent) zip
                  historyApi.addPuzzle(user = user, completedAt = now, perf = userPerf) void
              } >>- {
                if (prevRound.isEmpty)
                  Bus.publish(
                    Puzzle
                      .UserResult(puzzle.id, user.id, win, user.perfs.puzzle.intRating -> userPerf.intRating),
                    "finishPuzzle"
                  )
              } inject (round -> userPerf).some
        }
      }

  private object ponder:

    // themes that don't hint at the solution
    private val nonHintingThemes: Set[PuzzleTheme.Key] = Set(
      PuzzleTheme.opening,
      PuzzleTheme.middlegame,
      PuzzleTheme.endgame,
      PuzzleTheme.rookEndgame,
      PuzzleTheme.bishopEndgame,
      PuzzleTheme.pawnEndgame,
      PuzzleTheme.knightEndgame,
      PuzzleTheme.queenEndgame,
      PuzzleTheme.queenRookEndgame,
      PuzzleTheme.master,
      PuzzleTheme.masterVsMaster,
      PuzzleTheme.superGM
    ).map(_.key)

    private def isHinting(theme: PuzzleTheme.Key) = !nonHintingThemes(theme)

    // themes that make the solution very obvious
    private val isObvious: Set[PuzzleTheme.Key] = Set(
      PuzzleTheme.enPassant,
      PuzzleTheme.attackingF2F7,
      PuzzleTheme.doubleCheck,
      PuzzleTheme.mateIn1,
      PuzzleTheme.castling
    ).map(_.key)

    private def weightOf(angle: PuzzleAngle, win: PuzzleWin) =
      angle.asTheme.fold(1f) { theme =>
        if (theme == PuzzleTheme.mix.key) 1
        else if (isObvious(theme))
          if (win.yes) 0.2f else 0.6f
        else if (isHinting(theme))
          if (win.yes) 0.3f else 0.7f
        else if (win.yes) 0.7f
        else 0.8f
      }

    def player(angle: PuzzleAngle, win: PuzzleWin, glicko: (Glicko, Glicko), puzzle: Glicko) =
      val provisionalPuzzle = puzzle.provisional.yes ?? {
        if (win.yes) -0.2f else -0.7f
      }
      glicko._1.average(glicko._2, (weightOf(angle, win) + provisionalPuzzle) atLeast 0.1f)

    def puzzle(angle: PuzzleAngle, win: PuzzleWin, glicko: (Glicko, Glicko), player: Glicko) =
      if (player.clueless) glicko._1
      else glicko._1.average(glicko._2, weightOf(angle, win))

  private val VOLATILITY = Glicko.default.volatility
  private val TAU        = 0.75d
  private val calculator = glicko2.RatingCalculator(VOLATILITY, TAU)

  def incPuzzlePlays(puzzleId: PuzzleId): Funit =
    colls.puzzle.map(_.incFieldUnchecked($id(puzzleId), Puzzle.BSONFields.plays))

  private def updateRatings(u1: glicko2.Rating, u2: glicko2.Rating, win: PuzzleWin): Unit =
    val results = glicko2.GameRatingPeriodResults(
      List(
        if win.yes then glicko2.GameResult(u1, u2, false)
        else glicko2.GameResult(u2, u1, false)
      )
    )
    try calculator.updateRatings(results)
    catch case e: Exception => logger.error("finisher", e)
