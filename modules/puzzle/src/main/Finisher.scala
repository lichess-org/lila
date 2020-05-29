package lidraughts.puzzle

import org.goochjs.glicko2._
import org.joda.time.DateTime

import draughts.Mode
import draughts.variant.Variant
import lidraughts.db.dsl._
import lidraughts.rating.{ Glicko, PerfType }
import lidraughts.user.{ User, UserRepo }

private[puzzle] final class Finisher(
    api: PuzzleApi,
    historyApi: lidraughts.history.HistoryApi,
    puzzleColl: Map[Variant, Coll],
    bus: lidraughts.common.Bus
) {

  def apply(puzzle: Puzzle, user: User, result: Result): Fu[(Round, Mode)] = {
    val formerUserRating = user.perfs.puzzle(puzzle.variant).intRating
    api.head.find(user, puzzle.variant) flatMap {
      case Some(PuzzleHead(_, Some(c), _)) if c == puzzle.id =>
        api.head.solved(user, puzzle.id, puzzle.variant) >> {
          val userRating = user.perfs.puzzle(puzzle.variant).toRating
          val puzzleRating = puzzle.perf.toRating
          updateRatings(userRating, puzzleRating, result.glicko)
          val date = DateTime.now
          val puzzlePerf = puzzle.perf.addOrReset(_.puzzle.crazyGlicko, s"puzzle ${puzzle.id} user")(puzzleRating)
          val userPerf = user.perfs.puzzle(puzzle.variant).addOrReset(_.puzzle.crazyGlicko, s"puzzle ${puzzle.id}")(userRating, date)
          val a = new Round(
            puzzleId = puzzle.id,
            userId = user.id,
            date = date,
            result = result,
            rating = formerUserRating,
            ratingDiff = userPerf.intRating - formerUserRating
          )
          val perfType = puzzle.variant match {
            case draughts.variant.Frisian => PerfType.PuzzleFrisian
            case draughts.variant.Russian => PerfType.PuzzleRussian
            case _ => PerfType.Puzzle
          }
          historyApi.addPuzzle(user = user, completedAt = date, perf = userPerf, puzzleType = perfType)
          api.round.add(a, puzzle.variant) >> {
            puzzleColl(puzzle.variant).update(
              $id(puzzle.id),
              $inc(Puzzle.BSONFields.attempts -> $int(1)) ++
                $set(Puzzle.BSONFields.perf -> PuzzlePerf.puzzlePerfBSONHandler.write(puzzlePerf))
            ) zip UserRepo.setPerf(user.id, PerfType.puzzlePerf(puzzle.variant), userPerf)
          } inject (a, Mode.Rated, formerUserRating, userPerf.intRating)
        }
      case _ =>
        incPuzzleAttempts(puzzle)
        val a = new Round(
          puzzleId = puzzle.id,
          userId = user.id,
          date = DateTime.now,
          result = result,
          rating = formerUserRating,
          ratingDiff = 0
        )
        fuccess(a, Mode.Casual, formerUserRating, formerUserRating)
    }
  } map {
    case (round, mode, ratingBefore, ratingAfter) =>
      if (mode.rated)
        bus.publish(Puzzle.UserResult(puzzle.id, puzzle.variant, user.id, result, ratingBefore -> ratingAfter), 'finishPuzzle)
      round -> mode
  }

  /* offline solving from the mobile API
   * avoid exploits by not updating the puzzle rating,
   * only the user rating (we don't care about that one).
   * Returns the user with updated puzzle rating */
  def ratedUntrusted(puzzle: Puzzle, user: User, result: Result): Fu[User] = {
    val formerUserRating = user.perfs.puzzle(puzzle.variant).intRating
    val userRating = user.perfs.puzzle(puzzle.variant).toRating
    val puzzleRating = puzzle.perf.toRating
    updateRatings(userRating, puzzleRating, result.glicko)
    val date = DateTime.now
    val userPerf = user.perfs.puzzle(puzzle.variant).addOrReset(_.puzzle.crazyGlicko, s"puzzle ${puzzle.id}")(userRating, date)
    val a = new Round(
      puzzleId = puzzle.id,
      userId = user.id,
      date = date,
      result = result,
      rating = formerUserRating,
      ratingDiff = userPerf.intRating - formerUserRating
    )
    api.round.add(a, puzzle.variant) >>
      UserRepo.setPerf(user.id, PerfType.puzzlePerf(puzzle.variant), userPerf) >>-
      bus.publish(
        Puzzle.UserResult(puzzle.id, puzzle.variant, user.id, result, formerUserRating -> userPerf.intRating),
        'finishPuzzle
      ) inject
        user.copy(perfs = user.perfs.copy(puzzle = user.perfs.puzzle.map { case (v, p) => if (v == puzzle.variant) v -> userPerf else v -> p }))
  } recover lidraughts.db.recoverDuplicateKey { _ =>
    logger.info(s"ratedUntrusted ${user.id} ${puzzle.id} duplicate round")
    user // has already been solved!
  }

  private val VOLATILITY = Glicko.default.volatility
  private val TAU = 0.75d
  private val system = new RatingCalculator(VOLATILITY, TAU)

  def incPuzzleAttempts(puzzle: Puzzle) =
    puzzleColl(puzzle.variant).incFieldUnchecked($id(puzzle.id), Puzzle.BSONFields.attempts)

  private def updateRatings(u1: Rating, u2: Rating, result: Glicko.Result): Unit = {
    val results = new RatingPeriodResults()
    result match {
      case Glicko.Result.Draw => results.addDraw(u1, u2)
      case Glicko.Result.Win => results.addResult(u1, u2)
      case Glicko.Result.Loss => results.addResult(u2, u1)
    }
    try {
      val (r1, r2) = (u1.getRating, u2.getRating)
      system.updateRatings(results)
      // never take away more than 30 rating points - it just causes upsets
      List(r1 -> u1, r2 -> u2).foreach {
        case (prev, next) if next.getRating - prev < -30 => next.setRating(prev - 30)
        case _ =>
      }
    } catch {
      case e: Exception => logger.error("finisher", e)
    }
  }
}
